package com.app.core.repository;

import com.app.core.model.PrecipitacionLastDays;
import com.app.core.model.Precipitaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrecipitacionesRepository extends JpaRepository<Precipitaciones, Long> {

    @Query(value = "SELECT DISTINCT ON (pre.indicativo) pre.* " +
            "FROM precipitaciones pre " +
            "ORDER BY pre.indicativo, pre.fecha_actualizacion DESC",
            nativeQuery = true)
    List<Precipitaciones> findLatestPrecipitacionesForEachEstacion();

    @Query(value = "SELECT pre.indicativo AS indicativo, " +
            " pre.nombre AS nombre, " +
            " date_trunc('day', pre.fecha_actualizacion) AS fechaActualizacion, " +
            " MAX(pre.precipitacion_24h) AS maximo24h " +
            " FROM precipitaciones pre " +
            " WHERE pre.fecha_actualizacion >= CURRENT_DATE - (:days * INTERVAL '1 day') " +
            " GROUP BY pre.indicativo, pre.nombre, date_trunc('day', pre.fecha_actualizacion) " +
            " ORDER BY fechaActualizacion DESC, indicativo ASC",
            nativeQuery = true)
    List<PrecipitacionLastDays> findPrecipitacionesLastDays(@Param("days") int days);
}
