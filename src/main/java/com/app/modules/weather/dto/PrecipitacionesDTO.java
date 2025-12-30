package com.app.modules.weather.dto;

import lombok.ToString;

import java.util.Date;
@ToString
public class PrecipitacionesDTO {

    private Double precipitacion1h;
    private Double precipitacion3h;
    private Double precipitacion6h;
    private Double precipitacion12h;
    private Double precipitacion24h;
    private Double precipitacionAcumuladaAñoHidrologico;
    private Double precipitacionYTD;
    private Date fecha;

    public PrecipitacionesDTO() {
    }

    public Double getPrecipitacionAcumuladaAñoHidrologico() {
        return precipitacionAcumuladaAñoHidrologico;
    }

    public void setPrecipitacionAcumuladaAñoHidrologico(Double precipitacionAcumuladaAñoHidrologico) {
        this.precipitacionAcumuladaAñoHidrologico = precipitacionAcumuladaAñoHidrologico;
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

    public Double getPrecipitacionYTD() {
        return precipitacionYTD;
    }

    public void setPrecipitacionYTD(Double precipitacionYTD) {
        this.precipitacionYTD = precipitacionYTD;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }
}
