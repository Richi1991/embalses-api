package com.app.modules.hidrology.dto;

import java.time.LocalDateTime;

public record EstadisticaCuencaDTO(
        LocalDateTime intervalo,
        Double mediaUtimoDatoNivel,
        Double mediaUltimoDatoCaudal,
        Double mediaUltimoDataPorcentaje
) { }
