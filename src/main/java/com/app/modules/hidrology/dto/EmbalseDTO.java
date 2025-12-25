package com.app.modules.hidrology.dto;

import java.sql.Timestamp;

public record EmbalseDTO(
        int idEmbalse,
        String nombre,
        double hm3,
        double porcentaje,
        double capacidadMaximaEmbalse,
        double variacion, // Este será el % de cambio
        TendenciaEnum tendencia,
        Timestamp fechaRegistro// "subida", "bajada" o "estable"
) {}


