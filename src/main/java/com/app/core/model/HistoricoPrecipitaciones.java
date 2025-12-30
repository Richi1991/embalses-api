package com.app.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
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

    @Column(name = "valor_1h", precision = 4, scale = 1)
    private Double valor1h;

    @Column(name = "valor_24h", precision = 4, scale = 1)
    private Double valor24h;

    @Column(name = "valor_ytd", precision = 4, scale = 1)
    private Double valorYtd;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_registro")
    private Instant fechaRegistro;

    @Column(name = "tmax", precision = 4, scale = 1)
    private Double tmax;

    @Column(name = "tmin", precision = 4, scale = 1)
    private Double tmin;

    @Column(name = "tmed", precision = 4, scale = 1)
    private Double tmed;

    public void setIndicativo(String indicativo) { this.indicativo = indicativo; }
    public void setValor24h(Double valor24h) { this.valor24h = valor24h; }
    public void setFechaRegistro(Instant fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public void setTmax(Double tmax) { this.tmax = tmax; }
    public void setTmin(Double tmin) { this.tmin = tmin; }
    public void setTmed(Double tmed) { this.tmed = tmed; }

}