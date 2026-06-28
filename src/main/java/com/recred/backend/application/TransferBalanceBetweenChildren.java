package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.CreditStatus;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.TipoPlan;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.SchoolCreditRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
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
    public void execute(TransferBalanceBetweenChildrenRequest request) {
        request.validate();

        Usuario padre = getPadreValidate(request.cognitoSub());
        Tutor tutor = tutorRepository.findByUsuarioId(padre.getId())
                .orElseThrow(() -> new SecurityException("El usuario no es un tutor válido"));

        Alumno fromStudent = getHijo(tutor, request.fromStudentId());
        Alumno toStudent = getHijo(tutor, request.toStudentId());

        Billetera fromWallet = getWallet(request.fromStudentId(), "origen");
        Billetera toWallet = getWallet(request.toStudentId(), "destino");

        fromWallet.debit(request.amount());
        toWallet.credit(request.amount());

        walletRepository.save(fromWallet);

        registerTransferMovements(
                fromStudent, toStudent, fromWallet, toWallet, request.amount(), padre.getId());

        handleMicrocreditPayment(toStudentId(request), toWallet, request.amount(), padre.getId());

        walletRepository.save(toWallet);
    }

    private Usuario getPadreValidate(String cognitoSub) {
        Usuario padre = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        if (padre.getPlan() == TipoPlan.GRATUITO) {
            throw new IllegalArgumentException(
                    "La transferencia entre hijos no está permitida en cuentas gratuitas");
        }

        if (padre.getRol() != RolUsuario.PADRE) {
            throw new SecurityException("Solo un padre puede transferir saldo");
        }
        return padre;
    }

    private Alumno getHijo(Tutor tutor, UUID studentId) {
        return tutor.getHijos().stream()
                .filter(h -> h.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new SecurityException(
                        "Ambos alumnos deben ser hijos del tutor"));
    }

    private Billetera getWallet(UUID studentId, String type) {
        return walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException(
                        "Billetera " + type + " no encontrada"));
    }

    private UUID toStudentId(TransferBalanceBetweenChildrenRequest request) {
        return request.toStudentId();
    }

    private void registerTransferMovements(Alumno fromStudent, Alumno toStudent, 
            Billetera fromWallet, Billetera toWallet, BigDecimal amount, UUID padreId) {
        String descDebito = "Transferencia enviada a " 
                + toStudent.getNombre() + " " + toStudent.getApellido();
        movimientoBilleteraRepository.registrarMovimiento(
                fromWallet.getId(), 
                TipoMovimientoBilletera.AJUSTE, 
                amount.negate(), 
                descDebito, 
                padreId
        );

        String descCredito = "Transferencia recibida de " 
                + fromStudent.getNombre() + " " + fromStudent.getApellido();
        movimientoBilleteraRepository.registrarMovimiento(
                toWallet.getId(), 
                TipoMovimientoBilletera.AJUSTE, 
                amount, 
                descCredito, 
                padreId
        );
    }

    private void handleMicrocreditPayment(UUID toStudentId, Billetera toWallet, 
            BigDecimal amount, UUID padreId) {
        schoolCreditRepository.findByStudentIdAndStatus(toStudentId, CreditStatus.ACTIVE)
                .ifPresent(credit -> {
                    BigDecimal creditAmount = credit.getAmount();
                    BigDecimal paidAmount;
                    if (amount.compareTo(creditAmount) >= 0) {
                        paidAmount = creditAmount;
                        credit.setAmount(BigDecimal.ZERO);
                        credit.setStatus(CreditStatus.PAID);
                    } else {
                        paidAmount = amount;
                        credit.setAmount(creditAmount.subtract(amount));
                    }
                    schoolCreditRepository.save(credit);
                    
                    toWallet.debit(paidAmount);
                    movimientoBilleteraRepository.registrarMovimiento(
                            toWallet.getId(), 
                            TipoMovimientoBilletera.AJUSTE, 
                            paidAmount.negate(), 
                            "Pago automático de microcrédito con transferencia", 
                            padreId
                    );
                });
    }
}
