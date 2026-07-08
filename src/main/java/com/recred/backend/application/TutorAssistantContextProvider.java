package com.recred.backend.application;

import com.recred.backend.domain.exceptions.TutorNoEncontradoException;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.Tutor;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.TutorRepository;

public class TutorAssistantContextProvider implements SchoolAssistantContextProvider {

    private final TutorRepository tutorRepository;
    private final TutorAssistantContextBuilder contextBuilder;

    public TutorAssistantContextProvider(
            TutorRepository tutorRepository,
            TutorAssistantContextBuilder contextBuilder
    ) {
        this.tutorRepository = tutorRepository;
        this.contextBuilder = contextBuilder;
    }

    @Override
    public boolean supports(SchoolAssistantRole role) {
        return role == SchoolAssistantRole.TUTOR;
    }

    @Override
    public SchoolAssistantResolvedContext resolve(Usuario user, String cognitoSubject) {
        Tutor tutor = tutorRepository.findByUsuarioIdCognito(cognitoSubject)
                .orElseThrow(() -> new TutorNoEncontradoException(cognitoSubject));
        return new SchoolAssistantResolvedContext(
                contextBuilder.build(user.getId(), tutor),
                SchoolAssistantConversationOwner.user(user.getId())
        );
    }
}
