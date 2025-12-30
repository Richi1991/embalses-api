package com.app.core.repository;

import com.app.core.model.Precipitaciones;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrecipitacionesRepository extends JpaRepository<Precipitaciones, Long> {
}
