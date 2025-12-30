package com.app.core.repository;

import com.app.core.model.Precipitaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrecipitacionesRepository extends JpaRepository<Precipitaciones, Long> {

    @Query(value = "SELECT DISTINCT ON (pre.indicativo) pre.* " +
            "FROM precipitaciones pre " +
            "ORDER BY pre.indicativo, pre.fecha_actualizacion DESC",
            nativeQuery = true)
    List<Precipitaciones> findLatestPrecipitacionesForEachEstacion();
}
