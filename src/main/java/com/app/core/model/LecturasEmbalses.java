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
@Table(name = "lecturas_embalses")
public class LecturasEmbalses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "embalse_id")
    private Embalses embalse;

    @Column(name = "hm3_actual", nullable = false, precision = 10, scale = 3)
    private BigDecimal hm3Actual;

    @Column(name = "porcentaje", precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fecha_registro")
    private Instant fechaRegistro;

    @Column(name = "tendencia", length = 10)
    private String tendencia;

    @Column(name = "variacion", precision = 10, scale = 3)
    private BigDecimal variacion;

    @ColumnDefault("0.0")
    @Column(name = "precipitacion_mm_chs", precision = 5, scale = 2)
    private BigDecimal precipitacionMmChs;


}