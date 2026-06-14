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

public class TransferBalanceBetweenChildren {

    private final UsuarioRepository usuarioRepository;
    private final TutorRepository tutorRepository;
    private final WalletRepository walletRepository;
    private final MovimientoBilleteraRepository movimientoBilleteraRepository;

    public TransferBalanceBetweenChildren(
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
    public void execute(
            String cognitoSub, 
            UUID fromStudentId, 
            UUID toStudentId, 
            BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (fromStudentId.equals(toStudentId)) {
            throw new IllegalArgumentException("Cannot transfer to the same student");
        }

        Usuario padre = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        if (padre.getRol() != RolUsuario.PADRE) {
            throw new SecurityException("Solo un padre puede transferir saldo");
        }

        Tutor tutor = tutorRepository.findByUsuarioId(padre.getId())
                .orElseThrow(() -> new SecurityException(
                        "El usuario no es un tutor válido"));

        Alumno fromStudent = tutor.getHijos().stream()
                .filter(h -> h.getId().equals(fromStudentId))
                .findFirst()
                .orElseThrow(() -> new SecurityException(
                        "Ambos alumnos deben ser hijos del tutor"));

        Alumno toStudent = tutor.getHijos().stream()
                .filter(h -> h.getId().equals(toStudentId))
                .findFirst()
                .orElseThrow(() -> new SecurityException(
                        "Ambos alumnos deben ser hijos del tutor"));

        Billetera fromWallet = walletRepository.findByStudentId(fromStudentId)
                .orElseThrow(() -> new RuntimeException(
                        "Billetera origen no encontrada"));
        
        Billetera toWallet = walletRepository.findByStudentId(toStudentId)
                .orElseThrow(() -> new RuntimeException(
                        "Billetera destino no encontrada"));

        fromWallet.debit(amount);
        toWallet.credit(amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        String descDebito = "Transferencia enviada a " 
                + toStudent.getNombre() + " " + toStudent.getApellido();
        movimientoBilleteraRepository.registrarMovimiento(
                fromWallet.getId(), 
                TipoMovimientoBilletera.AJUSTE, 
                amount.negate(), 
                descDebito, 
                padre.getId()
        );

        String descCredito = "Transferencia recibida de " 
                + fromStudent.getNombre() + " " + fromStudent.getApellido();
        movimientoBilleteraRepository.registrarMovimiento(
                toWallet.getId(), 
                TipoMovimientoBilletera.AJUSTE, 
                amount, 
                descCredito, 
                padre.getId()
        );
    }
}

