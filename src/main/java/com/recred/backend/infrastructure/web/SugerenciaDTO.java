package com.recred.backend.infrastructure.web;

import com.recred.backend.application.SeasonalSuggestionResponse;

public class SugerenciaDTO {
    private String categoria;
    private String accion;
    private String motivo;
    private String icono;
    
    @com.fasterxml.jackson.annotation.JsonProperty("top_productos")
    private java.util.List<com.recred.backend.infrastructure.web.MenuProductoDTO> topProductos;

    public SugerenciaDTO() {
    }

    public SugerenciaDTO(
            String categoria, String accion, String motivo, String icono) {
        this.categoria = categoria;
        this.accion = accion;
        this.motivo = motivo;
        this.icono = icono;
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

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public java.util.List<com.recred.backend.infrastructure.web.MenuProductoDTO> getTopProductos() {
        return topProductos;
    }

    public void setTopProductos(
            java.util.List<com.recred.backend.infrastructure.web.MenuProductoDTO> topProductos) {
        this.topProductos = topProductos;
    }

    public static SugerenciaDTO fromApplication(SeasonalSuggestionResponse response) {
        SugerenciaDTO dto = new SugerenciaDTO(
                response.category(),
                response.action(),
                response.reason(),
                response.icon()
        );
        dto.setTopProductos(response.topProducts() == null ? null : response.topProducts()
                .stream()
                .map(MenuProductoDTO::fromApplication)
                .toList());
        return dto;
    }
}
