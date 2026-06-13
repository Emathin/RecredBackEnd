package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;

import java.math.BigDecimal;
import java.util.UUID;

public class LoadBalanceToChild {

    private final UsuarioRepository usuarioRepository;
    private final WalletRepository walletRepository;

    public LoadBalanceToChild(
            UsuarioRepository usuarioRepository, 
            WalletRepository walletRepository) {
        this.usuarioRepository = usuarioRepository;
        this.walletRepository = walletRepository;
    }

    public void execute(String cognitoSub, UUID studentId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Usuario padre = usuarioRepository.findByIdCognito(cognitoSub)
                .orElseThrow(() -> new SecurityException("Usuario no encontrado"));

        if (padre.getRol() != RolUsuario.PADRE) {
            throw new SecurityException("Solo un padre puede cargar saldo");
        }

        if (padre.getTutor() == null || padre.getTutor().getHijos().stream()
                .noneMatch(h -> h.getId().equals(studentId))) {
            throw new SecurityException("El alumno no es hijo del tutor o no existe");
        }

        Billetera billetera = walletRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Billetera no encontrada para el alumno"));

        billetera.credit(amount);
        walletRepository.save(billetera);
    }
}
