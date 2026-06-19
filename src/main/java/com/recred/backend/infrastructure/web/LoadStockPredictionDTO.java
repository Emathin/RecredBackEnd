package com.recred.backend.infrastructure.web;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoadStockPredictionDTO {
    @JsonProperty("nombre")
    private String nombre;
    @JsonProperty("descripcion")
    private String descripcion;
    @JsonProperty("peso")
    private String peso;
    @JsonProperty("contiene_azucar")
    private String contieneAzucar;
    @JsonProperty("contiene_mani")
    private String contieneMani;
    @JsonProperty("contiene_lactosa")
    private String contieneLactosa;
    @JsonProperty("contiene-tacc")
    private String contieneTacc;
    @JsonProperty("url_imagen")
    private String urlImagen;
}
