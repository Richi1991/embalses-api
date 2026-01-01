package com.app.core.repository;

import com.app.core.model.EstacionesMeteorologicas;
import com.app.modules.hidrology.dto.PrecipitacionMapaDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface MapaPrecipitacionRepository extends JpaRepository<EstacionesMeteorologicas, String> {

    @Query(value = "SELECT res_geometry, res_indicativo, res_nombre, res_mm_acumulados, res_tipo " +
            "FROM calcular_mapa_precipitacion(:inicio, :fin)",
            nativeQuery = true)
    List<PrecipitacionMapaDTO> obtenerMapaPrecipitacion(
            @Param("inicio") OffsetDateTime inicio,
            @Param("fin") OffsetDateTime fin
    );
}
