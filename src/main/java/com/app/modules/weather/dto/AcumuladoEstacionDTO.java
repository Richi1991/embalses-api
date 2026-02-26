package com.app.modules.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AcumuladoEstacionDTO {

    @JsonProperty("indicativo")
    private String indicativo;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("valorAcumulado")
    private Double valorAcumulado;

    @JsonProperty("longitud")
    private Double longitud;

    @JsonProperty("latitud")
    private Double latitud;
}