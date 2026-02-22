package com.app.modules.weather.dao;

import com.app.core.constantes.Constants;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.weather.dto.EstacionesDTO;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.exception.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.app.core.jooq.generated.Tables.ESTACIONES_METEOROLOGICAS;

@Repository
public class EstacionesDAO {

    @Autowired
    private DSLContext dsl;

    public void insertarEstacionesAemetFilterByProvincia(List<EstacionesDTO> estaciones) throws FunctionalExceptions {
        int intentos = 0;
        boolean exito = false;

        while (intentos < 3 && !exito) {
            try {
                List<Query> inserts = estaciones.stream()
                        .map(dto -> (Query) dsl.insertInto(ESTACIONES_METEOROLOGICAS,
                                        ESTACIONES_METEOROLOGICAS.LATITUD,
                                        ESTACIONES_METEOROLOGICAS.PROVINCIA,
                                        ESTACIONES_METEOROLOGICAS.ALTITUD,
                                        ESTACIONES_METEOROLOGICAS.INDICATIVO,
                                        ESTACIONES_METEOROLOGICAS.NOMBRE,
                                        ESTACIONES_METEOROLOGICAS.INDSINOP,
                                        ESTACIONES_METEOROLOGICAS.LONGITUD,
                                        ESTACIONES_METEOROLOGICAS.RED_ORIGEN)
                                .values(
                                        dto.getLatitud(),
                                        dto.getProvincia(),
                                        dto.getAltitud(),
                                        dto.getIndicativo(),
                                        dto.getNombre(),
                                        dto.getIndsinop(),
                                        dto.getLongitud(),
                                        Constants.AEMET))
                        .collect(Collectors.toList());

                dsl.batch(inserts).execute();
                exito = true;

            } catch (DataAccessException e) {
                intentos++;
                if (intentos >= 3) {
                    Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
                }
                manejarEspera(3000L);
            }
        }
    }

    public void manejarEspera(Long time) {
        try {
            Thread.sleep(time); // Espera X segundos antes de reintentar
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
