package com.recred.backend.application;

import com.recred.backend.domain.exceptions.UsuarioAutenticadoNoEncontradoException;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.UsuarioRepository;
import java.util.List;

public class SchoolAssistantContextResolver {

    private final UsuarioRepository usuarioRepository;
    private final List<SchoolAssistantContextProvider> providers;

    public SchoolAssistantContextResolver(
            UsuarioRepository usuarioRepository,
            List<SchoolAssistantContextProvider> providers
    ) {
        this.usuarioRepository = usuarioRepository;
        this.providers = providers;
    }

    public SchoolAssistantResolvedContext resolve(
            SchoolAssistantRole role,
            String cognitoSubject
    ) {
        Usuario user = authenticatedUser(cognitoSubject);
        return provider(role).resolve(user, cognitoSubject);
    }

    private Usuario authenticatedUser(String cognitoSubject) {
        return usuarioRepository.findByIdCognito(cognitoSubject)
                .orElseThrow(() -> new UsuarioAutenticadoNoEncontradoException(cognitoSubject));
    }

    private SchoolAssistantContextProvider provider(SchoolAssistantRole role) {
        return providers.stream()
                .filter(provider -> provider.supports(role))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "unsupported school assistant role"));
    }
}
