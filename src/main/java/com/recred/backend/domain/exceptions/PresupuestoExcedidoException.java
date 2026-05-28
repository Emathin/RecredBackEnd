package com.recred.backend.domain.exceptions;

public class PresupuestoExcedidoException extends RuntimeException {
    public PresupuestoExcedidoException(String message) {
        super(message);
    }
}
