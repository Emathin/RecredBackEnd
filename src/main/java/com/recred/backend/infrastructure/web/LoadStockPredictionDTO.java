package com.recred.backend.infrastructure.web;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoadStockPredictionDTO {
    @JsonProperty("name")
    private String nombre;
    @JsonProperty("description")
    private String descripcion;
    @JsonProperty("weight")
    private String peso;
    @JsonProperty("contiene_azucar")
    private String contieneAzucar;
    @JsonProperty("contiene_mani")
    private String contieneMani;
    @JsonProperty("contiene_lactosa")
    private String contieneLactosa;
    @JsonProperty("contiene-tacc")
    private String contieneTacc;
}
