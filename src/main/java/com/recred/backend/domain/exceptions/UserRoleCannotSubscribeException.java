package com.recred.backend.domain.exceptions;

import com.recred.backend.domain.models.RolUsuario;

public class UserRoleCannotSubscribeException extends RuntimeException {

    public UserRoleCannotSubscribeException(RolUsuario role) {
        super("User role cannot buy an individual subscription: " + role);
    }
}
