package com.app.modules.weather.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum RangoTemporalEnum {
    ULTIMO_DIA(1, ChronoUnit.DAYS),
    ULTIMA_SEMANA(7, ChronoUnit.DAYS),
    ULTIMAS_DOS_SEMANAS(14, ChronoUnit.WEEKS),
    ULTIMO_MES(1, ChronoUnit.MONTHS),
    ULTIMOS_TRES_MESES(3, ChronoUnit.MONTHS),
    ULTIMOS_SEIS_MESES(6, ChronoUnit.MONTHS),
    ULTIMO_ANIO(1, ChronoUnit.YEARS);

    private final Integer cantidad;
    private final ChronoUnit unidad;

    RangoTemporalEnum(Integer cantidad, ChronoUnit unidad) {
        this.cantidad = cantidad;
        this.unidad = unidad;
    }

    public LocalDate calcularFechaDesde(LocalDate hoy) {
        return hoy.minus(cantidad, unidad);
    }
}
