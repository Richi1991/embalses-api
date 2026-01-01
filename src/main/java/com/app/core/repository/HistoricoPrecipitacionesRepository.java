package com.app.core.repository;

import com.app.core.model.HistoricoPrecipitaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface HistoricoPrecipitacionesRepository extends JpaRepository<HistoricoPrecipitaciones, Long> {

    @Query(value= "SELECT * FROM historico_precipitaciones WHERE fecha_registro BETWEEN :fechaInicio AND :fechaFin", nativeQuery = true )
    List<HistoricoPrecipitaciones> getValoresHistoricoPrecipitacionesBetweenTwoDates(Timestamp fechaInicio, Timestamp fechaFin);
}

