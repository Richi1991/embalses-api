package com.app.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "precipitaciones")
public class Precipitaciones {
    @EmbeddedId
    private PrecipitacioneId id;

    @Column(name = "nombre", length = 50)
    private String nombre;

    @Column(name = "precipitacion_1h")
    private Double precipitacion1h;

    @Column(name = "precipitacion_3h")
    private Double precipitacion3h;

    @Column(name = "precipitacion_6h")
    private Double precipitacion6h;

    @Column(name = "precipitacion_12h")
    private Double precipitacion12h;

    @Column(name = "precipitacion_24h")
    private Double precipitacion24h;

    @ManyToOne
    @JoinColumn(name = "indicativo", referencedColumnName = "indicativo", insertable = false, updatable = false)
    private EstacionesMeteorologicas estacion;

    public PrecipitacioneId getId() {
        return id;
    }

    public void setId(PrecipitacioneId id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecipitacion1h() {
        return precipitacion1h;
    }

    public void setPrecipitacion1h(Double precipitacion1h) {
        this.precipitacion1h = precipitacion1h;
    }

    public Double getPrecipitacion3h() {
        return precipitacion3h;
    }

    public void setPrecipitacion3h(Double precipitacion3h) {
        this.precipitacion3h = precipitacion3h;
    }

    public Double getPrecipitacion6h() {
        return precipitacion6h;
    }

    public void setPrecipitacion6h(Double precipitacion6h) {
        this.precipitacion6h = precipitacion6h;
    }

    public Double getPrecipitacion12h() {
        return precipitacion12h;
    }

    public void setPrecipitacion12h(Double precipitacion12h) {
        this.precipitacion12h = precipitacion12h;
    }

    public Double getPrecipitacion24h() {
        return precipitacion24h;
    }

    public void setPrecipitacion24h(Double precipitacion24h) {
        this.precipitacion24h = precipitacion24h;
    }

    public EstacionesMeteorologicas getEstacion() {
        return estacion;
    }

    public void setEstacion(EstacionesMeteorologicas estacion) {
        this.estacion = estacion;
    }
}