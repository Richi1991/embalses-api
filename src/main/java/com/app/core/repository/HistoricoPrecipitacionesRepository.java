package com.app.core.repository;

import com.app.core.model.HistoricoPrecipitaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface HistoricoPrecipitacionesRepository extends JpaRepository<HistoricoPrecipitaciones, Long> {

    @Query(value = "SELECT " +
            "est.indicativo, " +
            "est.nombre, " +
            "ROUND(SUM(hist.valor_24h)::numeric, 1) as valor_acumulado, " +
            "ST_X(est.geom::geometry) as lng, " +
            "ST_Y(est.geom::geometry) as lat " +
            "FROM historico_precipitaciones hist " +
            "JOIN estaciones_meteorologicas est ON hist.indicativo = est.indicativo " +
            "WHERE hist.fecha_registro >= CURRENT_DATE - CAST(:periodo AS interval) " +
            "GROUP BY est.indicativo, est.nombre, est.geom " +
            "ORDER BY valor_acumulado DESC", nativeQuery = true)
    List<AcumuladoEstacion> findAcumuladosDinamicos(@Param("periodo") String periodo);
}

