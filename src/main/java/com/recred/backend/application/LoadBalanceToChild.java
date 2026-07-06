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
    public LoadBalanceToChildResponse execute(LoadBalanceToChildRequest request) {
        request.validate();
        LoadBalanceContext context = contextFor(request);
        loadBalance(request.amount(), context);
        return new LoadBalanceToChildResponse();
    }

    private LoadBalanceContext contextFor(LoadBalanceToChildRequest request) {
        Usuario padre = getPadreValidate(request.cognitoSub());
        Tutor tutor = getTutor(padre);
        Alumno student = getHijo(tutor, request.studentId());
        Billetera billetera = getWallet(request.studentId());
        return new LoadBalanceContext(padre, student, billetera);
    }

    private Usuario getPadreValidate(String cognitoSub) {
        Usuario padre = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));
        padre.validateCanLoadBalanceToChild();
        return padre;
    }

    private Tutor getTutor(Usuario padre) {
        return tutorRepository.findByUsuarioId(padre.getId())
                .orElseThrow(() -> new SecurityException("El usuario no es un tutor válido"));
    }

    private Alumno getHijo(Tutor tutor, UUID studentId) {
        return tutor.childOrThrow(studentId, "El alumno no es hijo del tutor o no existe");
    }

    private Billetera getWallet(UUID studentId) {
        return walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException(
                        "Billetera no encontrada para el alumno"));
    }

    private void loadBalance(BigDecimal amount, LoadBalanceContext context) {
        context.billetera().credit(amount);
        walletRepository.save(context.billetera());
        registerLoadMovement(context.billetera(), context.student(), amount,
                context.padre().getId());
    }

    private void registerLoadMovement(
            Billetera billetera,
            Alumno student,
            BigDecimal amount,
            UUID padreId
    ) {
        String desc = "Carga de saldo realizada por tutor a "
                + student.getNombre() + " " + student.getApellido();
        movimientoBilleteraRepository.registrarMovimiento(
                billetera.getId(),
                TipoMovimientoBilletera.CARGA,
                amount,
                desc,
                padreId
        );
    }

    private record LoadBalanceContext(Usuario padre, Alumno student, Billetera billetera) {
    }
}
