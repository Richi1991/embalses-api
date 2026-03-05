package com.app.modules.hidrology.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.Timestamp;

public record UltimaLecturaCaudalDTO(
        @JsonProperty("codigo")
        String codigo,
        @JsonProperty("nombre")
        String nombre,
        @JsonProperty("ultimoDatoNivel")
        Double ultimoDatoNivel,
        @JsonProperty("ultimoDatoCaudal")
        Double ultimoDatoCaudal,
        @JsonProperty("porcentajeNivel")
        Double porcentajeNivel,
        @JsonProperty("cotaMaximaSeccion")
        Double cotaMaximaSeccion,
        @JsonProperty("latitud")
        Double latitud,
        @JsonProperty("longitud")
        Double longitud,
        @JsonProperty("createdAt")
        Timestamp createdAt
) {
}
