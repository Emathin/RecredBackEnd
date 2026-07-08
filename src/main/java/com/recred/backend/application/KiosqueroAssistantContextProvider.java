package com.recred.backend.application;

import com.recred.backend.domain.exceptions.KiosqueroNotFoundException;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.KiosqueroRepository;
import java.util.UUID;

public class KiosqueroAssistantContextProvider implements SchoolAssistantContextProvider {

    private final KiosqueroRepository kiosqueroRepository;
    private final KiosqueroAssistantContextBuilder contextBuilder;

    public KiosqueroAssistantContextProvider(
            KiosqueroRepository kiosqueroRepository,
            KiosqueroAssistantContextBuilder contextBuilder
    ) {
        this.kiosqueroRepository = kiosqueroRepository;
        this.contextBuilder = contextBuilder;
    }

    @Override
    public boolean supports(SchoolAssistantRole role) {
        return role == SchoolAssistantRole.KIOSQUERO;
    }

    @Override
    public SchoolAssistantResolvedContext resolve(Usuario user, String cognitoSubject) {
        UUID buffetId = kiosqueroRepository.findBuffetByUsuarioId(user.getId());

        if (buffetId == null) {
            throw new KiosqueroNotFoundException(cognitoSubject);
        }

        return new SchoolAssistantResolvedContext(
                contextBuilder.build(user.getId(), buffetId),
                SchoolAssistantConversationOwner.user(user.getId())
        );
    }
}
