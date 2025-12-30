package com.app.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "historico_precipitaciones")
public class HistoricoPrecipitaciones {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historico", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indicativo", referencedColumnName = "indicativo")
    private EstacionesMeteorologicas indicativo;

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
    private Double tMax;

    @Column(name = "tmin", precision = 4, scale = 1)
    private Double tMin;

    @Column(name = "tmed", precision = 4, scale = 1)
    private Double tMed;


}