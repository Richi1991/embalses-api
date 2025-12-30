package com.app.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
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

    @Column(name = "indicativo", length = 8)
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


}