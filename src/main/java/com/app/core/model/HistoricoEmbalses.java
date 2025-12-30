package com.app.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "historico_embalses")
public class HistoricoEmbalses {
    @Id
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "hm3_anterior", nullable = false, precision = 10, scale = 2)
    private BigDecimal hm3Anterior;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion;


}