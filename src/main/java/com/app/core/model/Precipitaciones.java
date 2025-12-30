package com.app.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "precipitaciones")
public class Precipitaciones {
    @EmbeddedId
    private PrecipitacioneId id;

    @Column(name = "nombre", length = 50)
    private String nombre;

    @Column(name = "precipitacion_1h", precision = 4)
    private Double precipitacion1h;

    @Column(name = "precipitacion_3h", precision = 4)
    private Double precipitacion3h;

    @Column(name = "precipitacion_6h", precision = 4)
    private Double precipitacion6h;

    @Column(name = "precipitacion_12h", precision = 4)
    private Double precipitacion12h;

    @Column(name = "precipitacion_24h", precision = 4)
    private Double precipitacion24h;

    @Column(name = "precipitacion_48h", precision = 4)
    private Double precipitacion48h;

    @Column(name = "precipitacion_7d", precision = 4)
    private Double precipitacion7d;

    @Column(name = "precipitacion_30d", precision = 4)
    private Double precipitacion30d;

    @Column(name = "precipitacion_hidrologic_year", precision = 4)
    private Double precipitacionHidrologicYear;

    @Column(name = "precipitacion_ytd", precision = 4)
    private Double precipitacionYtd;


}