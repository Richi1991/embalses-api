package com.app.modules.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluvioChsDTO(
        @JsonProperty("CodPuntoMedicion") String codPuntoMedicion,
        @JsonProperty("NombreCortoPM") String nombreCortoPM,
        @JsonProperty("ValorPrecip") String valorPrecip,
        @JsonProperty("FechaUltimoDato") String fechaUltimoDato
) {}
