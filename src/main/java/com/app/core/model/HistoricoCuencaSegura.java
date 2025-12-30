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
@Table(name = "historico_cuenca_segura")
public class HistoricoCuencaSegura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "volumen_total", nullable = false, precision = 12, scale = 3)
    private BigDecimal volumenTotal;

    @Column(name = "porcentaje_total", precision = 5, scale = 2)
    private BigDecimal porcentajeTotal;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_registro")
    private Instant fechaRegistro;

    @ColumnDefault("0.0")
    @Column(name = "precipitacion_mm_chs", precision = 5, scale = 1)
    private BigDecimal precipitacionMmChs;


}