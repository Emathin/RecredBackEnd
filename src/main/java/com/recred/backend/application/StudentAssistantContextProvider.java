package com.recred.backend.application;

import com.recred.backend.domain.exceptions.AlumnoNoEncontradoException;
import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.Usuario;
import com.recred.backend.domain.repositories.AlumnoRepository;
import java.util.UUID;

public class StudentAssistantContextProvider implements SchoolAssistantContextProvider {

    private final AlumnoRepository alumnoRepository;
    private final StudentAssistantContextBuilder contextBuilder;

    public StudentAssistantContextProvider(
            AlumnoRepository alumnoRepository,
            StudentAssistantContextBuilder contextBuilder
    ) {
        this.alumnoRepository = alumnoRepository;
        this.contextBuilder = contextBuilder;
    }

    @Override
    public boolean supports(SchoolAssistantRole role) {
        return role == SchoolAssistantRole.ALUMNO;
    }

    @Override
    public SchoolAssistantResolvedContext resolve(Usuario user, String cognitoSubject) {
        UUID studentId = resolvedStudentId(user.getId());
        return new SchoolAssistantResolvedContext(
                contextBuilder.build(studentId),
                SchoolAssistantConversationOwner.student(studentId)
        );
    }

    private UUID resolvedStudentId(UUID studentIdOrUserId) {
        return alumnoRepository.existsById(studentIdOrUserId)
                ? studentIdOrUserId
                : alumnoRepository.findStudentIdByUserId(studentIdOrUserId)
                        .orElseThrow(() -> new AlumnoNoEncontradoException(studentIdOrUserId));
    }
}
