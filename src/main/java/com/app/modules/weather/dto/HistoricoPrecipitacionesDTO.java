package com.app.modules.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoPrecipitacionesDTO {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("indicativo")
    private String indicativo;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("valor24h")
    private Double valor24h;

    @JsonProperty("fechaRegistro")
    private Timestamp fechaRegistro;

    @JsonProperty("tmax")
    private Double tmax;

    @JsonProperty("tmin")
    private Double tmin;

    @JsonProperty("tmed")
    private Double tmed;
}