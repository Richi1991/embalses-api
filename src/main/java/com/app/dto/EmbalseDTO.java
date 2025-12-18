package com.app.dto;

import com.app.constantes.Tendencia;

public record EmbalseDTO(
        String nombre,
        double hm3,
        double porcentaje,
        double variacion, // Este será el % de cambio
        Tendencia tendencia  // "subida", "bajada" o "estable"
) {}


