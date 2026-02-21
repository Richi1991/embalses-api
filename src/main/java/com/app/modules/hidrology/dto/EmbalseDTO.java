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
        Timestamp fechaRegistro,
        String latitud,
        String longitud
) {
    // Constructor personalizado
    public EmbalseDTO(Integer idEmbalse, String nombreEmbalse, Double hm3,
                      Double porcentaje, Double capacidadMaximaEmbalse,
                      Double variacion, TendenciaEnum tendencia, Timestamp fechaRegistro) {

        // LLAMADA OBLIGATORIA al constructor canónico (this)
        this(
                idEmbalse != null ? idEmbalse : 0,
                nombreEmbalse,
                hm3 != null ? hm3 : 0.0,
                porcentaje != null ? porcentaje : 0.0,
                capacidadMaximaEmbalse != null ? capacidadMaximaEmbalse : 0.0,
                variacion != null ? variacion : 0.0,
                tendencia,
                fechaRegistro,
                null, // latitud por defecto
                null  // longitud por defecto
        );
    }

    public EmbalseDTO(Integer idEmbalse, String nombreEmbalse, Double hm3,
                      Double porcentaje, Double capacidadMaximaEmbalse,
                      Double variacion, TendenciaEnum tendencia, Timestamp fechaRegistro, Double latitud, Double longitud) {
        this(
                idEmbalse != null ? idEmbalse : 0,
                nombreEmbalse,
                hm3 != null ? hm3 : 0.0,
                porcentaje != null ? porcentaje : 0.0,
                capacidadMaximaEmbalse != null ? capacidadMaximaEmbalse : 0.0,
                variacion != null ? variacion : 0.0,
                tendencia,
                fechaRegistro,
                latitud != null ? String.valueOf(latitud) : null, // Conversión a String
                longitud != null ? String.valueOf(longitud) : null // Conversión a String
        );
    }
}


