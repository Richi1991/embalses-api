package com.app.modules.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
public class EstacionesDTO {

    @JsonProperty("latitud")
    private String latitud;

    @JsonProperty("provincia")
    private String provincia;

    @JsonProperty("altitud")
    private Short altitud;

    @JsonProperty("indicativo")
    private String indicativo;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("indsinop")
    private String indsinop;

    @JsonProperty("longitud")
    private String longitud;

    @JsonProperty("precipitacion_1h")
    private Double precipitacion_1h;

    @JsonProperty("precipitacion_3h")
    private Double precipitacion_3h;

    @JsonProperty("precipitacion_6h")
    private Double precipitacion_6h;

    @JsonProperty("precipitacion_12h")
    private Double precipitacion_12h;

    @JsonProperty("precipitacion_24h")
    private Double precipitacion_24h;

    private String redOrigen;
    private String geom;
    private Timestamp fechaActualizacion;
    private PrecipitacionesDTO precipitacionesDTO;
    private TemperaturasDTO temperaturasDTO;

}
