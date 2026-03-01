package com.app.modules.hidrology.dao;

import com.app.core.common.Utils;
import com.app.modules.hidrology.dto.CaudalDTO;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static com.app.core.jooq.generated.Tables.LECTURA_CAUDALES_DIARIA;
import static com.app.core.jooq.generated.Tables.LECTURA_CAUDALES_HORARIA;

@Repository
public class CaudalDAO {

    @Autowired
    private DSLContext dsl;

    public void insertarDatosCaudalesHorarios(CaudalDTO caudalDTO, List<Query> inserts, Double ultimoDatoNivel, Double ultimoDatoCaudal, Double porcentajeNivel, Double cotaMaxima, Utils.Coordinates coordinates) {
        inserts.add(dsl.insertInto(LECTURA_CAUDALES_HORARIA)
                .set(LECTURA_CAUDALES_HORARIA.CODIGO, caudalDTO.codigoPuntoMedicion())
                .set(LECTURA_CAUDALES_HORARIA.NOMBRE, caudalDTO.nombre())
                .set(LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_NIVEL, ultimoDatoNivel)
                .set(LECTURA_CAUDALES_HORARIA.ULTIMO_DATO_CAUDAL, ultimoDatoCaudal)
                .set(LECTURA_CAUDALES_HORARIA.PORCENTAJE_NIVEL, porcentajeNivel)
                .set(LECTURA_CAUDALES_HORARIA.COTA_MAXIMA_SECCION, cotaMaxima)
                .set(LECTURA_CAUDALES_HORARIA.LATITUD, coordinates.latitud())
                .set(LECTURA_CAUDALES_HORARIA.LONGITUD, coordinates.longitud())
                .set(LECTURA_CAUDALES_HORARIA.CREATED_AT, OffsetDateTime.now(ZoneId.of("Europe/Madrid"))));
    }

    public void insertarDatosCaudalesDiarios(CaudalDTO caudalDTO, List<Query> inserts, Double ultimoDatoNivel, Double ultimoDatoCaudal, Double porcentajeNivel, Double cotaMaxima, Utils.Coordinates coordinates) {
        inserts.add(dsl.insertInto(LECTURA_CAUDALES_DIARIA)
                .set(LECTURA_CAUDALES_DIARIA.CODIGO, caudalDTO.codigoPuntoMedicion())
                .set(LECTURA_CAUDALES_DIARIA.NOMBRE, caudalDTO.nombre())
                .set(LECTURA_CAUDALES_DIARIA.ULTIMO_DATO_NIVEL, ultimoDatoNivel)
                .set(LECTURA_CAUDALES_DIARIA.ULTIMO_DATO_CAUDAL, ultimoDatoCaudal)
                .set(LECTURA_CAUDALES_DIARIA.PORCENTAJE_NIVEL, porcentajeNivel)
                .set(LECTURA_CAUDALES_DIARIA.COTA_MAXIMA_SECCION, cotaMaxima)
                .set(LECTURA_CAUDALES_DIARIA.LATITUD, coordinates.latitud())
                .set(LECTURA_CAUDALES_DIARIA.LONGITUD, coordinates.longitud())
                .set(LECTURA_CAUDALES_DIARIA.CREATED_AT, OffsetDateTime.now(ZoneId.of("Europe/Madrid"))));
    }
}
