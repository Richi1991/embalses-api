package com.app.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "historico_precipitaciones")
public class HistoricoPrecipitaciones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historico", nullable = false)
    private Integer id;

    @Column(name = "indicativo")
    private String indicativo;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "valor_24h")
    private Double valor24h;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_registro")
    private Timestamp fechaRegistro;

    @Column(name = "tmax")
    private Double tmax;

    @Column(name = "tmin")
    private Double tmin;

    @Column(name = "tmed")
    private Double tmed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicativo", referencedColumnName = "indicativo", insertable = false, updatable = false)
    private EstacionesMeteorologicas estacionesMeteorologicas;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Double getValor24h() {
        return valor24h;
    }

    public void setValor24h(Double valor24h) {
        this.valor24h = valor24h;
    }

    public Timestamp getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Timestamp fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public Double getTmax() {
        return tmax;
    }

    public void setTmax(Double tmax) {
        this.tmax = tmax;
    }

    public Double getTmin() {
        return tmin;
    }

    public void setTmin(Double tmin) {
        this.tmin = tmin;
    }

    public Double getTmed() {
        return tmed;
    }

    public void setTmed(Double tmed) {
        this.tmed = tmed;
    }

    public EstacionesMeteorologicas getEstacionesMeteorologicas() {
        return estacionesMeteorologicas;
    }

    public void setEstacionesMeteorologicas(EstacionesMeteorologicas estacionesMeteorologicas) {
        this.estacionesMeteorologicas = estacionesMeteorologicas;
    }
}