package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

public class LoadBalanceToChild {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final WalletRepository walletRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public LoadBalanceToChild(
            UsuarioRepository usuarioRepository,
            TutorRepository tutorRepository,
            WalletRepository walletRepository,
            MovimientoBilleteraRepository movimientoBilleteraRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tutorRepository = tutorRepository;
        this.walletRepository = walletRepository;
        this.movimientoBilleteraRepository = movimientoBilleteraRepository;
    }

    @Transactional
    public LoadBalanceToChildResponse execute(LoadBalanceToChildRequest loadRequest) {
        loadRequest.validate();
        LoadBalanceContext context = contextFor(loadRequest);
        creditChildWallet(loadRequest.amount(), context);
        return new LoadBalanceToChildResponse();
    }

    private LoadBalanceContext contextFor(LoadBalanceToChildRequest loadRequest) {
        Usuario parentUser = findParentAllowedToLoadBalance(loadRequest.cognitoSub());
        Tutor tutor = findTutorFor(parentUser);
        Alumno student = findChildForTutor(tutor, loadRequest.studentId());
        Billetera wallet = findWalletFor(loadRequest.studentId());
        return new LoadBalanceContext(parentUser, student, wallet);
    }

    private Usuario findParentAllowedToLoadBalance(String cognitoSub) {
        Usuario parentUser = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
        parentUser.validateCanLoadBalanceToChild();
        return parentUser;
    }

    private Tutor findTutorFor(Usuario parentUser) {
        return tutorRepository.findByUsuarioId(parentUser.getId())
                .orElseThrow(() -> new SecurityException("El usuario no es un tutor válido"));
    }

    private Alumno findChildForTutor(Tutor tutor, UUID studentId) {
        return tutor.childOrThrow(studentId, "El alumno no es hijo del tutor o no existe");
    }

    private Billetera findWalletFor(UUID studentId) {
        return walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException(
                        "Billetera no encontrada para el alumno"));
    }

    private void creditChildWallet(BigDecimal amount, LoadBalanceContext context) {
        context.billetera().credit(amount);
        walletRepository.save(context.billetera());
        registerBalanceLoadMovement(context.billetera(), context.student(), amount,
                context.parentUser().getId());
    }

    private void registerBalanceLoadMovement(
            Billetera billetera,
            Alumno student,
            BigDecimal amount,
            UUID parentUserId
    ) {
        String desc = "Carga de saldo realizada por tutor a "
                + student.getNombre() + " " + student.getApellido();
        movimientoBilleteraRepository.registrarMovimiento(
                billetera.getId(),
                TipoMovimientoBilletera.CARGA,
                amount,
                desc,
                parentUserId
        );
    }

    private record LoadBalanceContext(
            Usuario parentUser,
            Alumno student,
            Billetera billetera
    ) {
    }
}
