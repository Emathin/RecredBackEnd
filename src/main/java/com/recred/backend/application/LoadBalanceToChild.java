package com.recred.backend.application;

import com.recred.backend.domain.models.Alumno;
import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.models.TipoMovimientoBilletera;
import com.recred.backend.domain.repositories.TutorRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;
import com.recred.backend.domain.repositories.MovimientoBilleteraRepository;
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
    public void execute(LoadBalanceToChildRequest request) {
        request.validate();

        Usuario padre = getPadreValidate(request.cognitoSub());
        Tutor tutor = tutorRepository.findByUsuarioId(padre.getId())
                .orElseThrow(() -> new SecurityException("El usuario no es un tutor válido"));

        Alumno student = getHijo(tutor, request.studentId());
        Billetera billetera = getWallet(request.studentId());

        billetera.credit(request.amount());
        walletRepository.save(billetera);

        registerLoadMovement(billetera, student, request.amount(), padre.getId());
    }

    private Usuario getPadreValidate(String cognitoSub) {
        Usuario padre = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        if (padre.getRol() != RolUsuario.PADRE) {
            throw new SecurityException("Solo un padre puede cargar saldo");
        }
        return padre;
    }

    private Alumno getHijo(Tutor tutor, UUID studentId) {
        return tutor.getHijos().stream()
                .filter(h -> h.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new SecurityException(
                        "El alumno no es hijo del tutor o no existe"));
    }

    private Billetera getWallet(UUID studentId) {
        return walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException(
                        "Billetera no encontrada para el alumno"));
    }

    private void registerLoadMovement(Billetera billetera, Alumno student, 
            BigDecimal amount, UUID padreId) {
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
}

