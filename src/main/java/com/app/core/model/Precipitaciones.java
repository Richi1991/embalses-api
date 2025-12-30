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

    @Column(name = "precipitacion_48h")
    private Double precipitacion48h;

    @Column(name = "precipitacion_7d")
    private Double precipitacion7d;

    @Column(name = "precipitacion_30d")
    private Double precipitacion30d;

    @Column(name = "precipitacion_hidrologic_year")
    private Double precipitacionHidrologicYear;

    @Column(name = "precipitacion_ytd")
    private Double precipitacionYtd;

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

    public Double getPrecipitacion48h() {
        return precipitacion48h;
    }

    public void setPrecipitacion48h(Double precipitacion48h) {
        this.precipitacion48h = precipitacion48h;
    }

    public Double getPrecipitacion7d() {
        return precipitacion7d;
    }

    public void setPrecipitacion7d(Double precipitacion7d) {
        this.precipitacion7d = precipitacion7d;
    }

    public Double getPrecipitacion30d() {
        return precipitacion30d;
    }

    public void setPrecipitacion30d(Double precipitacion30d) {
        this.precipitacion30d = precipitacion30d;
    }

    public Double getPrecipitacionHidrologicYear() {
        return precipitacionHidrologicYear;
    }

    public void setPrecipitacionHidrologicYear(Double precipitacionHidrologicYear) {
        this.precipitacionHidrologicYear = precipitacionHidrologicYear;
    }

    public Double getPrecipitacionYtd() {
        return precipitacionYtd;
    }

    public void setPrecipitacionYtd(Double precipitacionYtd) {
        this.precipitacionYtd = precipitacionYtd;
    }

    public EstacionesMeteorologicas getEstacion() {
        return estacion;
    }

    public void setEstacion(EstacionesMeteorologicas estacion) {
        this.estacion = estacion;
    }
}