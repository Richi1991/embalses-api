package com.app.modules.hidrology.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.sql.Timestamp;

public record CaudalDTO(
        @JsonProperty("CodPuntoMedicion")
        String codigoPuntoMedicion,
        @JsonProperty("NombreCortoPM")
        String nombre,
        @JsonProperty("CodVariableHidrologicaNivel")
        String codigoVariableHidrologicaNivel,
        @JsonProperty("CodVariableHidrologicaCaudal")
        String codigoVariableHidrologicaCaudal,
        @JsonProperty("x")
        int latitud,
        @JsonProperty("y")
        int longitud,
        @JsonProperty("UltimoDatoNivel")
        String ultimoDatoNivel,
        @JsonProperty("UltimoDatoCaudal")
        String ultimoDatoCaudal,
        @JsonProperty("ColorFondo")
        String colorFondo,
        @JsonProperty("ColorTexto")
        String colorTexto,
        @JsonProperty("CotaMaximaSeccion")
        String cotaMaxima,
        @JsonProperty("PorcentajeNivel")
        String porcentaje,
        Timestamp fecha,
        Double variacion,
        TendenciaEnum tendencia) {
}
