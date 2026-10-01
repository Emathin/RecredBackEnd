package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferBalanceBetweenChildren {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final WalletRepository walletRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;
    private final SchoolCreditRepository schoolCreditRepository;

    public TransferBalanceBetweenChildren(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository,
            SchoolCreditRepository schoolCreditRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.walletRepository = walletRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
        this.schoolCreditRepository = schoolCreditRepository;
    }

    @Transactional
    public TransferBalanceBetweenChildrenResponse execute(
            TransferBalanceBetweenChildrenRequest request
    ) {
        TransferContext context = transferContext(request);
        applyTransfer(request, context);
        return new TransferBalanceBetweenChildrenResponse();
    }

    private TransferContext transferContext(TransferBalanceBetweenChildrenRequest request) {
        Usuario padre = validatedParent(request.cognitoSub());
        Tutor tutor = tutorFor(padre);
        return new TransferContext(padre, students(tutor, request), wallets(request));
    }

    private void applyTransfer(
            TransferBalanceBetweenChildrenRequest request,
            TransferContext context
    ) {
        if (context.padre().getTutor() != null 
                && Boolean.TRUE.equals(context.padre().getTutor().getEsColaborador())) {
            BigDecimal maxWithdrawal = movimientoBilleteraRepository.getCollaboratorNetBalance(
                    context.wallets().from().getId(), context.padre().getId()
            );
            if (request.amount().compareTo(maxWithdrawal) > 0) {
                throw new SecurityException(
                        "No puedes transferir más de lo que has aportado a esta billetera (" 
                        + maxWithdrawal + ")");
            }
        }
        
        transfer(request.amount(), context.wallets());
        registerTransferMovements(
                context.students(),
                context.wallets(),
                request.amount(),
                context.padre().getId());
        handleMicrocreditPayment(
                request.toStudentId(),
                context.wallets().to(),
                request.amount(),
                context.padre().getId());
        walletRepository.save(context.wallets().to());
    }

    private Usuario validatedParent(String cognitoSub) {
        Usuario padre = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
        padre.validateCanTransferBalanceBetweenChildren();
        return padre;
    }

    private Tutor tutorFor(Usuario padre) {
        return tutorRepository.findByUsuarioId(padre.getId())
                .orElseThrow(() -> new SecurityException("El usuario no es un tutor valido"));
    }

    private Students students(Tutor tutor, TransferBalanceBetweenChildrenRequest request) {
        String message = "Ambos alumnos deben ser hijos del tutor";
        return new Students(
                tutor.childOrThrow(request.fromStudentId(), message),
                tutor.childOrThrow(request.toStudentId(), message)
        );
    }

    private Wallets wallets(TransferBalanceBetweenChildrenRequest request) {
        return new Wallets(
                getWallet(request.fromStudentId(), "origen"),
                getWallet(request.toStudentId(), "destino")
        );
    }

    private Billetera getWallet(UUID studentId, String type) {
        return walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException(
                        "Billetera " + type + " no encontrada"));
    }

    private void transfer(BigDecimal amount, Wallets wallets) {
        wallets.from().debit(amount);
        wallets.to().credit(amount);
        walletRepository.save(wallets.from());
    }

    private void registerTransferMovements(
            Students students,
            Wallets wallets,
            BigDecimal amount,
            UUID padreId
    ) {
        movimientoBilleteraRepository.registrarMovimiento(
                wallets.from().getId(),
                TipoMovimientoBilletera.AJUSTE,
                amount.negate(),
                "Transferencia enviada a " + fullName(students.to()),
                padreId
        );
        movimientoBilleteraRepository.registrarMovimiento(
                wallets.to().getId(),
                TipoMovimientoBilletera.AJUSTE,
                amount,
                "Transferencia recibida de " + fullName(students.from()),
                padreId
        );
    }

    private String fullName(Alumno student) {
        return student.getNombre() + " " + student.getApellido();
    }

    private void handleMicrocreditPayment(
            UUID toStudentId,
            Billetera toWallet,
            BigDecimal amount,
            UUID padreId
    ) {
        schoolCreditRepository.findByStudentIdAndStatus(toStudentId, CreditStatus.ACTIVE)
                .ifPresent(credit -> {
                    BigDecimal paidAmount = paymentAmount(credit.getAmount(), amount);
                    credit.setAmount(credit.getAmount().subtract(paidAmount));
                    if (credit.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                        credit.setAmount(BigDecimal.ZERO);
                        credit.setStatus(CreditStatus.PAID);
                    }
                    schoolCreditRepository.save(credit);
                    registerMicrocreditPayment(toWallet, paidAmount, padreId);
                });
    }

    private BigDecimal paymentAmount(BigDecimal creditAmount, BigDecimal transferAmount) {
        return transferAmount.compareTo(creditAmount) >= 0 ? creditAmount : transferAmount;
    }

    private void registerMicrocreditPayment(
            Billetera toWallet,
            BigDecimal paidAmount,
            UUID padreId
    ) {
        toWallet.debit(paidAmount);
        movimientoBilleteraRepository.registrarMovimiento(
                toWallet.getId(),
                TipoMovimientoBilletera.AJUSTE,
                paidAmount.negate(),
                "Pago automatico de microcredito con transferencia",
                padreId
        );
    }

    private record Students(Alumno from, Alumno to) {
    }

    private record Wallets(Billetera from, Billetera to) {
    }

    private record TransferContext(Usuario padre, Students students, Wallets wallets) {
    }
}
