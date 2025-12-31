package com.app.core.repository;

import com.app.core.model.EstacionesMeteorologicas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstacionesMeteorologicasRepository  extends JpaRepository<EstacionesMeteorologicas, Long> {

    @Query(value = "SELECT * FROM estaciones_meteorologicas WHERE indicativo = :indicativo", nativeQuery = true)
    EstacionesMeteorologicas findByIndicativo(String indicativo);

}
