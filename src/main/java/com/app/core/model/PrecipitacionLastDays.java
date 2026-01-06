package com.app.core.model;

import java.time.LocalDateTime;

public interface PrecipitacionLastDays {

    String getIndicativo();
    String getNombre();
    Double getMaximo24h();
    LocalDateTime getFechaActualizacion();
}
