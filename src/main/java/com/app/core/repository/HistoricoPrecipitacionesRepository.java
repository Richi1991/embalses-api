package com.app.core.repository;

import com.app.core.model.HistoricoPrecipitaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoricoPrecipitacionesRepository extends JpaRepository<HistoricoPrecipitaciones, Long> {

}

