package com.app.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jdk.jfr.Timespan;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class PrecipitacioneId implements Serializable {
    private static final long serialVersionUID = 1361898944530548992L;
    @Column(name = "indicativo", nullable = false, length = 8)
    private String indicativo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_actualizacion", nullable = false)
    private Timestamp fechaActualizacion;

    public String getIndicativo() {
        return indicativo;
    }

    public void setIndicativo(String indicativo) {
        this.indicativo = indicativo;
    }

    public Timestamp getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Timestamp fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}