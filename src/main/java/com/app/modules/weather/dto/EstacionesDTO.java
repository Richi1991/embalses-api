package com.app.modules.weather.dto;

import java.sql.Timestamp;

public class EstacionesDTO {

    private String latitud;
    private String provincia;
    private Long altitud;
    private String indicativo;
    private String nombre;
    private String indsinop;
    private String longitud;
    private String redOrigen;
    private String geom;
    private Timestamp fechaActualizacion;
    private PrecipitacionesDTO precipitacionesDTO;

    public EstacionesDTO() {
    }

    public EstacionesDTO(String latitud, String provincia, long altitud, String indicativo, String nombre, String indsinop, String longitud, String redOrigen) {
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

    public String getRedOrigen() {
        return redOrigen;
    }

    public void setRedOrigen(String redOrigen) {
        this.redOrigen = redOrigen;
    }

    public String getGeom() {
        return geom;
    }

    public void setGeom(String geom) {
        this.geom = geom;
    }

    public Timestamp getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Timestamp fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public PrecipitacionesDTO getPrecipitacionesDTO() {
        return precipitacionesDTO;
    }

    public void setPrecipitacionesDTO(PrecipitacionesDTO precipitacionesDTO) {
        this.precipitacionesDTO = precipitacionesDTO;
    }
}
