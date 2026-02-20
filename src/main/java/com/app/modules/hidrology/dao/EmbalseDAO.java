package com.app.modules.hidrology.dao;

import com.app.core.config.DatabaseConfig;
import com.app.core.constantes.Constants;
import com.app.modules.hidrology.dto.TendenciaEnum;
import com.app.modules.hidrology.dto.EmbalseDTO;
import com.app.modules.hidrology.dto.HistoricoCuencaDTO;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.app.core.jooq.generated.Tables.EMBALSES;
import static com.app.core.jooq.generated.Tables.LECTURAS_EMBALSES;
import static org.jooq.impl.DSL.*;

@Repository
public class EmbalseDAO {

    @Autowired
    private DSLContext dsl;

    public List<EmbalseDTO> obtenerUltimasLecturasConVariacionPorIntervalo(String intervalo) {

        // 1. Lectura Actual usando DISTINCT ON (Mucho más rápido que rowNumber)
        var lecturaActual = select(
                LECTURAS_EMBALSES.EMBALSE_ID,
                LECTURAS_EMBALSES.HM3_ACTUAL,
                LECTURAS_EMBALSES.PORCENTAJE,
                LECTURAS_EMBALSES.FECHA_REGISTRO
        )
                .distinctOn(LECTURAS_EMBALSES.EMBALSE_ID) // <--- CLAVE DE RENDIMIENTO
                .from(LECTURAS_EMBALSES)
                .orderBy(LECTURAS_EMBALSES.EMBALSE_ID, LECTURAS_EMBALSES.FECHA_REGISTRO.desc())
                .asTable("curr");

        // 2. Lectura Pasada usando DISTINCT ON
        var lecturaPasada = select(
                LECTURAS_EMBALSES.EMBALSE_ID,
                LECTURAS_EMBALSES.HM3_ACTUAL
        )
                .distinctOn(LECTURAS_EMBALSES.EMBALSE_ID) // <--- CLAVE DE RENDIMIENTO
                .from(LECTURAS_EMBALSES)
                .where(LECTURAS_EMBALSES.FECHA_REGISTRO.le(field("NOW() - CAST({0} AS INTERVAL)", LocalDateTime.class, intervalo)))
                .orderBy(LECTURAS_EMBALSES.EMBALSE_ID, LECTURAS_EMBALSES.FECHA_REGISTRO.desc())
                .asTable("prev");

        // 3. Consulta Final
        return dsl.select(
                        EMBALSES.ID.as("idEmbalse"),
                        EMBALSES.NOMBRE.as("nombre"),
                        lecturaActual.field(LECTURAS_EMBALSES.HM3_ACTUAL).coerce(Double.class).as("hm3"),
                        lecturaActual.field(LECTURAS_EMBALSES.PORCENTAJE).coerce(Double.class).as("porcentaje"),
                        EMBALSES.CAPACIDAD_MAXIMA.as("capacidadMaximaEmbalse"),

                        // Variación
                        lecturaActual.field(LECTURAS_EMBALSES.HM3_ACTUAL)
                                .minus(coalesce(lecturaPasada.field(LECTURAS_EMBALSES.HM3_ACTUAL),
                                        lecturaActual.field(LECTURAS_EMBALSES.HM3_ACTUAL)))
                                .coerce(Double.class)
                                .as("variacion"),

                        val(TendenciaEnum.ESTABLE.name()).as("tendencia"),
                        lecturaActual.field(LECTURAS_EMBALSES.FECHA_REGISTRO).as("fechaRegistro")
                )
                .from(EMBALSES)
                .join(lecturaActual).on(EMBALSES.ID.eq(lecturaActual.field(LECTURAS_EMBALSES.EMBALSE_ID)))
                .leftJoin(lecturaPasada).on(EMBALSES.ID.eq(lecturaPasada.field(LECTURAS_EMBALSES.EMBALSE_ID)))
                .fetchInto(EmbalseDTO.class);
    }

    public void insertarValoresEnHistoricoCuencaSegura(double volumenActualCuenca, double porc, String nombreTabla) {
        dsl.insertInto(DSL.table(DSL.name(nombreTabla)))
                .columns(
                        DSL.field("volumen_total"),
                        DSL.field("porcentaje_total"),
                        DSL.field("fecha_registro")
                )
                .values(
                        volumenActualCuenca,
                        porc,
                        DSL.currentTimestamp() // Usa la función nativa de la BD
                )
                .execute();
    }

    public List<HistoricoCuencaDTO> getHistoricoCuencaSeguraList(String nombreTabla) {
        return dsl.select(
                        DSL.field("volumen_total", Double.class),
                        DSL.field("porcentaje_total", Double.class),
                        DSL.field("fecha_registro", Timestamp.class)
                )
                .from(DSL.table(DSL.name(nombreTabla))) // Uso de name() para evitar SQL Injection
                .orderBy(DSL.field("fecha_registro").asc())
                .fetchInto(HistoricoCuencaDTO.class); // Mapeo automático ultra rápido
    }


    public void checkDatabaseConnection() throws FunctionalExceptions {
        int intentos = 0;
        boolean conectado = false;

        while (intentos < 3 && !conectado) {
            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.executeQuery("SELECT 1");
                conectado = true; // Si llega aquí, todo ok

            } catch (Exception e) {
                intentos++;
                if (intentos >= 3) {
                    Exceptions.EMB_E_0003.lanzarExcepcionCausada(e);
                }
                manejarEspera(8000L);
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

    public List<EmbalseDTO> obtenerHistoricoEmbalsePorIdEmbalse(int idEmbalse) {
        return dsl.select(
                        LECTURAS_EMBALSES.EMBALSE_ID,
                        EMBALSES.NOMBRE.as("nombre_embalse"),
                        LECTURAS_EMBALSES.HM3_ACTUAL,
                        LECTURAS_EMBALSES.PORCENTAJE,
                        EMBALSES.CAPACIDAD_MAXIMA,
                        LECTURAS_EMBALSES.VARIACION,
                        LECTURAS_EMBALSES.TENDENCIA,
                        LECTURAS_EMBALSES.FECHA_REGISTRO
                )
                .from(LECTURAS_EMBALSES)
                .join(EMBALSES).on(LECTURAS_EMBALSES.EMBALSE_ID.eq(EMBALSES.ID))
                .where(LECTURAS_EMBALSES.EMBALSE_ID.eq(idEmbalse))
                .orderBy(LECTURAS_EMBALSES.FECHA_REGISTRO.asc())
                .fetch(record -> {
                    // Mapeo personalizado para manejar el Enum de tendencia
                    String tendenciaStr = record.get(LECTURAS_EMBALSES.TENDENCIA);
                    TendenciaEnum tendencia = (tendenciaStr != null)
                            ? TendenciaEnum.valueOf(tendenciaStr.toUpperCase())
                            : TendenciaEnum.ESTABLE;

                    return new EmbalseDTO(
                            record.get(LECTURAS_EMBALSES.EMBALSE_ID),
                            record.get(EMBALSES.NOMBRE.as("nombre_embalse")),
                            record.get(LECTURAS_EMBALSES.HM3_ACTUAL, Double.class), // jOOQ lo convierte automáticamente
                            record.get(LECTURAS_EMBALSES.PORCENTAJE, Double.class),
                            record.get(EMBALSES.CAPACIDAD_MAXIMA, Double.class),
                            record.get(LECTURAS_EMBALSES.VARIACION, Double.class),
                            tendencia,
                            Timestamp.valueOf(record.get(LECTURAS_EMBALSES.FECHA_REGISTRO))
                    );
                });
    }

}
