package com.app.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;


@Embeddable
public class PrecipitacioneId implements Serializable {
    private static final long serialVersionUID = 1361898944530548992L;

    @Column(name = "indicativo", nullable = false, length = 8)
    private String indicativo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_actualizacion", nullable = false)
    private Timestamp fechaActualizacion;


    public PrecipitacioneId() {}

    public void setIndicativo(String indicativo) {
        this.indicativo = indicativo;
    }

    public void setFechaActualizacion(Timestamp fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getIndicativo() { return indicativo; }
    public Timestamp getFechaActualizacion() { return fechaActualizacion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PrecipitacioneId that)) return false;
        return Objects.equals(indicativo, that.indicativo) &&
                Objects.equals(fechaActualizacion, that.fechaActualizacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indicativo, fechaActualizacion);
    }

}