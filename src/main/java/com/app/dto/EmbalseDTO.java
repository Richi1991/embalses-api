package com.app.dto;

public record EmbalseDTO(
    String nombre,
    double volumen,
    double porcentaje,
    double variacion, // Este será el % de cambio
    String tendencia  // "subida", "bajada" o "estable"
) {}


