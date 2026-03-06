package com.app.modules.hidrology.dao;

import com.app.core.common.Utils;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.dto.CaudalDTO;
import com.app.modules.hidrology.dto.EstadisticaCuencaDTO;
import com.app.modules.hidrology.dto.UltimaLecturaCaudalDTO;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.app.core.jooq.generated.Tables.LECTURA_CAUDALES_DIARIA;
import static com.app.core.jooq.generated.Tables.LECTURA_CAUDALES_HORARIA;
import static org.jooq.impl.DSL.avg;

@Repository
public class CaudalDAO {

    @Autowired
    private DSLContext dsl;

    public void insertarDatosCaudalesHorarios(CaudalDTO caudalDTO, List<Query> inserts, Double ultimoDatoNivel, Double ultimoDatoCaudal, Double porcentajeNivel, Double cotaMaxima, Double[] coordinates) {
        inserts.add(dsl.insertInto(LECTURA_CAUDALES_HORARIA)
                .set(LECTURA_CAUDALES_HORARIA.CODIGO, caudalDTO.codigoPuntoMedicion())
                .set(LECTURA_CAUDALES_HORARIA.NOMBRE, caudalDTO.nombre())
                .set(LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_NIVEL, ultimoDatoNivel)
                .set(LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_CAUDAL, ultimoDatoCaudal)
                .set(LECTURA_CAUDALES_HORARIA.PORCENTAJE_NIVEL, porcentajeNivel)
                .set(LECTURA_CAUDALES_HORARIA.COTA_MAXIMA_SECCION, cotaMaxima)
                .set(LECTURA_CAUDALES_HORARIA.LATITUD, coordinates[0])
                .set(LECTURA_CAUDALES_HORARIA.LONGITUD, coordinates[1])
                .set(LECTURA_CAUDALES_HORARIA.CREATED_AT, OffsetDateTime.now(ZoneId.of("Europe/Madrid"))));
    }

    public void insertarDatosCaudalesDiarios(CaudalDTO caudalDTO, List<Query> inserts, Double ultimoDatoNivel, Double ultimoDatoCaudal, Double porcentajeNivel, Double cotaMaxima, Double[] coordinates) {
        inserts.add(dsl.insertInto(LECTURA_CAUDALES_DIARIA)
                .set(LECTURA_CAUDALES_DIARIA.CODIGO, caudalDTO.codigoPuntoMedicion())
                .set(LECTURA_CAUDALES_DIARIA.NOMBRE, caudalDTO.nombre())
                .set(LECTURA_CAUDALES_DIARIA.ULTIMO_DATO_NIVEL, ultimoDatoNivel)
                .set(LECTURA_CAUDALES_DIARIA.ULTIMO_DATO_CAUDAL, ultimoDatoCaudal)
                .set(LECTURA_CAUDALES_DIARIA.PORCENTAJE_NIVEL, porcentajeNivel)
                .set(LECTURA_CAUDALES_DIARIA.COTA_MAXIMA_SECCION, cotaMaxima)
                .set(LECTURA_CAUDALES_DIARIA.LATITUD, coordinates[0])
                .set(LECTURA_CAUDALES_DIARIA.LONGITUD, coordinates[1])
                .set(LECTURA_CAUDALES_DIARIA.CREATED_AT, OffsetDateTime.now(ZoneId.of("Europe/Madrid"))));
    }

    public List<EstadisticaCuencaDTO> getCaudalesHorariosChsFilteredByDay(int days) throws FunctionalExceptions {
        List<EstadisticaCuencaDTO> estadisticaCuencaDTOList = new ArrayList<>();
        try {
            Field<LocalDateTime> intervalo = DSL.field(
                    "date_trunc('minute', {0}) - INTERVAL '1 minute' * (EXTRACT(minute FROM {0})::int % 30)",
                    LocalDateTime.class,
                    LECTURA_CAUDALES_HORARIA.CREATED_AT
            ).as("intervalo");

            estadisticaCuencaDTOList = dsl.select(
                            intervalo,
                            DSL.round(avg(LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_NIVEL).cast(BigDecimal.class), 2).as("mediaUltimoDatoNivel"),
                            DSL.round(avg(LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_CAUDAL).cast(BigDecimal.class), 2).as("mediaUltimoDatoCaudal"),
                            DSL.round(avg(LECTURA_CAUDALES_HORARIA.PORCENTAJE_NIVEL).cast(BigDecimal.class), 2).as("mediaPorcentajeNivel"))
                    .from(LECTURA_CAUDALES_HORARIA)
                    .where(LECTURA_CAUDALES_HORARIA.CREATED_AT.greaterOrEqual(OffsetDateTime.now().minusDays(24)))
                    .groupBy(intervalo)
                    .orderBy(intervalo.desc())
                    .fetch()
                    .map(record -> new EstadisticaCuencaDTO(
                            record.get("intervalo", LocalDateTime.class),
                            record.get("mediaUltimoDatoNivel", Double.class),
                            record.get("mediaUltimoDatoCaudal", Double.class),
                            record.get("mediaPorcentajeNivel", Double.class)
                    ));
        } catch (RuntimeException e) {
            Exceptions.CAU_E_0002.lanzarExcepcionCausada(e);
        }
        return estadisticaCuencaDTOList;
    }

    public List<UltimaLecturaCaudalDTO> getLastCaudalAndPosition() throws FunctionalExceptions {

        List<UltimaLecturaCaudalDTO> ultimaLecturaCaudalDTO = null;
        try {
            ultimaLecturaCaudalDTO = dsl.select(
                    LECTURA_CAUDALES_HORARIA.CODIGO,
                    LECTURA_CAUDALES_HORARIA.NOMBRE,
                    LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_NIVEL,
                    LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_CAUDAL,
                    LECTURA_CAUDALES_HORARIA.PORCENTAJE_NIVEL,
                            LECTURA_CAUDALES_HORARIA.COTA_MAXIMA_SECCION,
                    LECTURA_CAUDALES_HORARIA.LATITUD,
                    LECTURA_CAUDALES_HORARIA.LONGITUD,
                    LECTURA_CAUDALES_HORARIA.CREATED_AT.cast(Timestamp.class)
            )
                    .distinctOn(LECTURA_CAUDALES_HORARIA.CODIGO)
                    .from(LECTURA_CAUDALES_HORARIA)
                    .orderBy(LECTURA_CAUDALES_HORARIA.CODIGO,
                            LECTURA_CAUDALES_HORARIA.CREATED_AT.desc())
                    .fetch(record -> new UltimaLecturaCaudalDTO(
                            record.get(LECTURA_CAUDALES_HORARIA.CODIGO),
                            record.get(LECTURA_CAUDALES_HORARIA.NOMBRE),
                            record.get(LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_NIVEL),
                            record.get(LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_CAUDAL),
                            record.get(LECTURA_CAUDALES_HORARIA.PORCENTAJE_NIVEL),
                            record.get(LECTURA_CAUDALES_HORARIA.COTA_MAXIMA_SECCION),
                            record.get(LECTURA_CAUDALES_HORARIA.LATITUD),
                            record.get(LECTURA_CAUDALES_HORARIA.LONGITUD),
                            record.get(LECTURA_CAUDALES_HORARIA.CREATED_AT.cast(Timestamp.class))
                    ));

        } catch(RuntimeException e) {
            Exceptions.CAU_E_0001.lanzarExcepcionCausada(e);
        }
        return ultimaLecturaCaudalDTO;
    }
}
