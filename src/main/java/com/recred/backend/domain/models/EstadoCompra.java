package com.recred.backend.domain.models;

public enum EstadoCompra {
    PENDIENTE("A preparar"),
    EN_PREPARACION("En preparacion"),
    LISTO("Listo para retirar"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado"),
    RECHAZADO("Rechazado"),
    VENCIDO("Vencido");

    private final String label;

    EstadoCompra(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
