package com.recred.backend.infrastructure.web;

public class SugerenciaDTO {
    private String categoria;
    private String accion;
    private String motivo;

    public SugerenciaDTO() {
    }

    public SugerenciaDTO(String categoria, String accion, String motivo) {
        this.categoria = categoria;
        this.accion = accion;
        this.motivo = motivo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
