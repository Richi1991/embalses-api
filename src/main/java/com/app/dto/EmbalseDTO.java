package com.app.dto;

import com.app.constantes.Tendencia;

import java.sql.Timestamp;

public record EmbalseDTO(
        int idEmbalse,
        String nombre,
        double hm3,
        double porcentaje,
        double variacion, // Este será el % de cambio
        Tendencia tendencia,
        Timestamp fechaRegistro// "subida", "bajada" o "estable"
) {}


