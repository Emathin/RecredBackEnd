package com.recred.backend.application;

import com.recred.backend.domain.models.Billetera;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.repositories.WalletRepository;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferBalanceBetweenChildren {

    private final UsuarioRepository usuarioRepository;
    private final WalletRepository walletRepository;

    public TransferBalanceBetweenChildren(
            UsuarioRepository usuarioRepository, 
            WalletRepository walletRepository) {
        this.usuarioRepository = usuarioRepository;
        this.walletRepository = walletRepository;
    }

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

        if (padre.getTutor() == null) {
            throw new SecurityException("El usuario no es un tutor válido");
        }

        boolean hasFrom = padre.getTutor().getHijos().stream()
                .anyMatch(h -> h.getId().equals(fromStudentId));
        boolean hasTo = padre.getTutor().getHijos().stream()
                .anyMatch(h -> h.getId().equals(toStudentId));

        if (!hasFrom || !hasTo) {
            throw new SecurityException("Ambos alumnos deben ser hijos del tutor");
        }

        Billetera fromWallet = walletRepository.findByStudentId(fromStudentId)
                .orElseThrow(() -> new RuntimeException("Billetera origen no encontrada"));
        
        Billetera toWallet = walletRepository.findByStudentId(toStudentId)
                .orElseThrow(() -> new RuntimeException("Billetera destino no encontrada"));

        fromWallet.debit(amount);
        toWallet.credit(amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
    }
}
