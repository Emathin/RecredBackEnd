package com.recred.backend.domain.models;

public enum PurchaseState {
    PENDIENTE("A preparar"),
    EN_PREPARACION("En preparacion"),
    LISTO("Listo para retirar"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String label;

    PurchaseState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
