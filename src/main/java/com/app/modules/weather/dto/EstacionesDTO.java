package com.app.modules.weather.dto;

public class EstacionesDTO {

    private String latitud;
    private String provincia;
    private Long altitud;
    private String indicativo;
    private String nombre;
    private String indsinop;
    private String longitud;
    private Double precipitacion1h;
    private Double precipitacion3h;
    private Double precipitacion6h;
    private Double precipitacion12h;
    private Double precipitacion24h;
    private Double precipitacionAcumuladaAñoHidrologico;

    public EstacionesDTO() {
    }

    public EstacionesDTO(String latitud, String provincia, long altitud, String indicativo, String nombre, String indsinop, String longitud) {
    }

    public String getLatitud() {
        return latitud;
    }

    public void setLatitud(String latitud) {
        this.latitud = latitud;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public Long getAltitud() {
        return altitud;
    }

    public void setAltitud(Long altitud) {
        this.altitud = altitud;
    }

    public String getIndicativo() {
        return indicativo;
    }

    public void setIndicativo(String indicativo) {
        this.indicativo = indicativo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIndsinop() {
        return indsinop;
    }

    public void setIndsinop(String indsinop) {
        this.indsinop = indsinop;
    }

    public String getLongitud() {
        return longitud;
    }

    public void setLongitud(String longitud) {
        this.longitud = longitud;
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
}
