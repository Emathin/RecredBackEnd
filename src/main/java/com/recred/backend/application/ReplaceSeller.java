package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsernameExistsException;
import com.recred.backend.domain.models.Buffet;
import com.recred.backend.domain.models.Colegio;
import com.recred.backend.domain.models.Kiosquero;
import com.recred.backend.domain.models.RolUsuario;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.BuffetRepository;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import com.recred.backend.domain.repositories.UsuarioRepository;
import com.recred.backend.domain.services.CognitoSellerIdentityService;
import com.recred.backend.domain.services.CreatedCognitoUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReplaceSeller {

    private final BuffetRepository buffetRepository;
    private final UsuarioRepository usuarioRepository;
    private final KiosqueroRepository kiosqueroRepository;
    private final CognitoSellerIdentityService cognitoService;

    public ReplaceSeller(BuffetRepository buffetRepository,
                         UsuarioRepository usuarioRepository,
                         KiosqueroRepository kiosqueroRepository,
                         CognitoSellerIdentityService cognitoService) {
        this.buffetRepository = buffetRepository;
        this.usuarioRepository = usuarioRepository;
        this.kiosqueroRepository = kiosqueroRepository;
        this.cognitoService = cognitoService;
    }

    @Transactional
    public RegisterSellerResponse execute(ReplaceSellerRequest request) {
        Buffet buffet = fetchBuffet(request.buffetId());
        
        disableOldSellerIfPresent(buffet);
        
        CreatedCognitoUser cognitoUser = createCognitoUser(request);
        Usuario savedUser = buildAndSaveUsuario(request, buffet.getColegio(), cognitoUser.sub());
        Kiosquero savedKiosquero = buildAndSaveKiosquero(request, savedUser, buffet);

        buffet.setVendedor(savedKiosquero);
        buffet.setActualizadoEn(LocalDateTime.now());
        buffetRepository.save(buffet);

        return new RegisterSellerResponse(savedKiosquero.getId(), savedUser.getId());
    }

    private Buffet fetchBuffet(UUID buffetId) {
        return buffetRepository.findById(buffetId)
                .orElseThrow(() -> new IllegalArgumentException("Buffet not found"));
    }

    private void disableOldSellerIfPresent(Buffet buffet) {
        if (buffet.getVendedor() != null && buffet.getVendedor().getUsuario() != null) {
            String oldCognitoSub = buffet.getVendedor().getUsuario().getIdCognito();
            cognitoService.disableUser(oldCognitoSub);
        }
    }

    private CreatedCognitoUser createCognitoUser(ReplaceSellerRequest req) {
        try {
            return cognitoService.createSellerUser(req.username(), req.email(),
                    req.firstName(), req.lastName());
        } catch (UsernameExistsException e) {
            String suffix = UUID.randomUUID().toString().substring(0, 4);
            return cognitoService.createSellerUser(req.username() + "-" + suffix,
                    req.email(), req.firstName(), req.lastName());
        }
    }

    private Usuario buildAndSaveUsuario(ReplaceSellerRequest req, Colegio colegio, String sub) {
        Usuario usuario = new Usuario();
        usuario.setColegio(colegio);
        usuario.setIdCognito(sub);
        usuario.setNombre(req.firstName());
        usuario.setApellido(req.lastName());
        usuario.setEmail(req.email());
        usuario.setTelefono(req.phone());
        usuario.setDni(req.dni());
        usuario.setRol(RolUsuario.VENDEDOR);
        usuario.setCreadoEn(LocalDateTime.now());
        usuario.setActualizadoEn(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }

    private Kiosquero buildAndSaveKiosquero(ReplaceSellerRequest req, Usuario user, Buffet buf) {
        Kiosquero kiosquero = new Kiosquero();
        kiosquero.setUsuario(user);
        kiosquero.setBuffet(buf);
        kiosquero.setCuit(req.cuit());
        kiosquero.setCreadoEn(LocalDateTime.now());
        kiosquero.setActualizadoEn(LocalDateTime.now());
        return kiosqueroRepository.save(kiosquero);
    }
}
