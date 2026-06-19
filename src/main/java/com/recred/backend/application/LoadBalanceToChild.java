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
    public void execute(String cognitoSub, UUID studentId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Usuario padre = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        if (padre.getRol() != RolUsuario.PADRE) {
            throw new SecurityException("Solo un padre puede cargar saldo");
        }

        Tutor tutor = tutorRepository.findByUsuarioId(padre.getId())
                .orElseThrow(() -> new SecurityException("El usuario no es un tutor válido"));

        Alumno student = tutor.getHijos().stream()
                .filter(h -> h.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new SecurityException(
                        "El alumno no es hijo del tutor o no existe"));

        Billetera billetera = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException(
                        "Billetera no encontrada para el alumno"));

        billetera.credit(amount);
        walletRepository.save(billetera);

        String desc = "Carga de saldo realizada por tutor a " 
                + student.getNombre() + " " + student.getApellido();
        movimientoBilleteraRepository.registrarMovimiento(
                billetera.getId(), 
                TipoMovimientoBilletera.CARGA, 
                amount, 
                desc, 
                padre.getId()
        );
    }
}

