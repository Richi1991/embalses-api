package com.app.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "estaciones_meteorologicas")
public class EstacionesMeteorologicas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "latitud", length = 30)
    private String latitud;

    @Column(name = "provincia", length = 25)
    private String provincia;

    @Column(name = "altitud", precision = 4)
    private BigDecimal altitud;

    @Column(name = "indicativo", length = 8, unique = true)
    private String indicativo;

    @Column(name = "nombre", length = 50)
    private String nombre;

    @Column(name = "indsinop", length = 10)
    private String indsinop;

    @Column(name = "longitud", length = 30)
    private String longitud;

    @Column(name = "red_origen", length = 20)
    private String redOrigen;

    @Column(name = "geom", columnDefinition = "geography")
    private Object geom;

    @Column(name = "municipio")
    private String municipio;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public BigDecimal getAltitud() {
        return altitud;
    }

    public void setAltitud(BigDecimal altitud) {
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

    public Object getGeom() {
        return geom;
    }

    public void setGeom(Object geom) {
        this.geom = geom;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }
}