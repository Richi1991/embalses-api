package com.app.core.repository;

import com.app.core.model.HistoricoPrecipitaciones;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface HistoricoPrecipitacionesRepository extends JpaRepository<HistoricoPrecipitaciones, Long> {

}

