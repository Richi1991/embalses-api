package com.app.modules.hidrology.dao;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.dto.TendenciaEnum;
import com.app.modules.hidrology.dto.EmbalseDTO;
import com.app.modules.hidrology.dto.HistoricoCuencaDTO;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.app.core.jooq.generated.Tables.EMBALSES;
import static com.app.core.jooq.generated.Tables.LECTURAS_EMBALSES;
import static org.jooq.impl.DSL.*;

@Repository
public class EmbalseDAO {

    @Autowired
    private DSLContext dsl;

    public List<EmbalseDTO> obtenerUltimasLecturasConVariacionPorIntervalo(String intervalo) throws FunctionalExceptions {
        List<EmbalseDTO> listaEmbalses = new ArrayList<>();
        try {
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
            listaEmbalses = dsl.select(
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
        } catch (RuntimeException e) {
            Exceptions.EMB_E_0012.lanzarExcepcionCausada(e);
        }
        return listaEmbalses;
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

    public List<HistoricoCuencaDTO> getHistoricoCuencaSeguraList(String nombreTabla) throws FunctionalExceptions {

        List<HistoricoCuencaDTO> historicoCuencaDTOList = new ArrayList<>();
        try {
            historicoCuencaDTOList = dsl.select(
                            DSL.field("volumen_total", Double.class),
                            DSL.field("porcentaje_total", Double.class),
                            DSL.field("fecha_registro", Timestamp.class)
                    )
                    .from(DSL.table(DSL.name(nombreTabla))) // Uso de name() para evitar SQL Injection
                    .orderBy(DSL.field("fecha_registro").asc())
                    .fetchInto(HistoricoCuencaDTO.class);
        } catch (RuntimeException e) {
            Exceptions.EMB_E_0013.lanzarExcepcionWithParams(nombreTabla);
        }

        return historicoCuencaDTOList;
    }

    public List<EmbalseDTO> obtenerHistoricoEmbalsePorIdEmbalse(int idEmbalse) throws FunctionalExceptions {
        List<EmbalseDTO> embalseDTOList = new ArrayList<>();
        try {
            embalseDTOList = dsl.select(
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
        } catch (RuntimeException e) {
            Exceptions.EMB_E_0014.lanzarExcepcionCausada(e);
        }
        return embalseDTOList;
    }


    public List<EmbalseDTO> getEmbalsesLastValueAndPosition() throws FunctionalExceptions {
        List<EmbalseDTO> embalseDTOList = new ArrayList<>();
        try {
            // 1. Definimos el ranking: particionamos por embalse y ordenamos por fecha descendente
            var rowNumberField = DSL.rowNumber()
                    .over(DSL.partitionBy(LECTURAS_EMBALSES.EMBALSE_ID)
                            .orderBy(LECTURAS_EMBALSES.FECHA_REGISTRO.desc()))
                    .as("fila_numero");

            // 2. Creamos una subconsulta que incluya ese número de fila
            var subquery = dsl.select(
                            LECTURAS_EMBALSES.EMBALSE_ID,
                            EMBALSES.NOMBRE,
                            LECTURAS_EMBALSES.HM3_ACTUAL,
                            LECTURAS_EMBALSES.PORCENTAJE,
                            EMBALSES.CAPACIDAD_MAXIMA,
                            LECTURAS_EMBALSES.VARIACION,
                            LECTURAS_EMBALSES.TENDENCIA,
                            LECTURAS_EMBALSES.FECHA_REGISTRO,
                            EMBALSES.LATITUD,
                            EMBALSES.LONGITUD,
                            rowNumberField
                    )
                    .from(LECTURAS_EMBALSES)
                    .join(EMBALSES).on(LECTURAS_EMBALSES.EMBALSE_ID.eq(EMBALSES.ID));

            // 3. Filtramos la subconsulta para quedarnos solo con las filas número 1
            embalseDTOList = dsl.selectFrom(subquery.asTable("ultima_lectura"))
                    .where(DSL.field(DSL.name("ultima_lectura", "fila_numero")).eq(1))
                    .fetch(record -> {
                        String tendenciaStr = record.get(LECTURAS_EMBALSES.TENDENCIA);
                        TendenciaEnum tendencia = (tendenciaStr != null)
                                ? TendenciaEnum.valueOf(tendenciaStr.toUpperCase())
                                : TendenciaEnum.ESTABLE;

                        return new EmbalseDTO(
                                record.get(LECTURAS_EMBALSES.EMBALSE_ID),
                                record.get(EMBALSES.NOMBRE),
                                record.get(LECTURAS_EMBALSES.HM3_ACTUAL, Double.class),
                                record.get(LECTURAS_EMBALSES.PORCENTAJE, Double.class),
                                record.get(EMBALSES.CAPACIDAD_MAXIMA, Double.class),
                                record.get(LECTURAS_EMBALSES.VARIACION, Double.class),
                                tendencia,
                                record.get(LECTURAS_EMBALSES.FECHA_REGISTRO, Timestamp.class),
                                record.get(EMBALSES.LATITUD),
                                record.get(EMBALSES.LONGITUD)
                        );
                    });
        } catch (RuntimeException e) {
            Exceptions.EMB_E_0015.lanzarExcepcionCausada(e);
        }
        return embalseDTOList;
    }

}
