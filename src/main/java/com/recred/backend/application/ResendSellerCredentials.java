package com.recred.backend.application;

import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoSellerIdentityService;

import java.util.UUID;

public class ResendSellerCredentials {

    private final KiosqueroRepository kiosqueroRepository;
    private final UsuarioRepository usuarioRepository;
    private final CognitoSellerIdentityService cognitoService;

    public ResendSellerCredentials(KiosqueroRepository kiosqueroRepository,
                                   UsuarioRepository usuarioRepository,
                                   CognitoSellerIdentityService cognitoService) {
        this.kiosqueroRepository = kiosqueroRepository;
        this.usuarioRepository = usuarioRepository;
        this.cognitoService = cognitoService;
    }

    public void execute(ResendSellerCredentialsRequest request) {
        UUID usuarioId = findUsuarioIdBySeller(request.sellerId());
        Usuario usuario = fetchUsuario(usuarioId);
        sendResendRequestToCognito(usuario);
    }

    private UUID findUsuarioIdBySeller(UUID sellerId) {
        UUID usuarioId = kiosqueroRepository.findUsuarioIdById(sellerId);
        if (usuarioId == null) {
            throw new IllegalArgumentException("Vendedor no encontrado");
        }
        return usuarioId;
    }

    private Usuario fetchUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private void sendResendRequestToCognito(Usuario usuario) {
        cognitoService.resendTemporaryPassword(usuario.getIdCognito());
    }
}
