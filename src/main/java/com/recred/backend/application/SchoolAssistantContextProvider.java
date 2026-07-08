package com.recred.backend.application;

import com.recred.backend.domain.models.SchoolAssistantRole;
import com.recred.backend.domain.models.Usuario;

public interface SchoolAssistantContextProvider {

    boolean supports(SchoolAssistantRole role);

    SchoolAssistantResolvedContext resolve(Usuario user, String cognitoSubject);
}
