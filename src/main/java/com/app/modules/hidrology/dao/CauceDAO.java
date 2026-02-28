package com.app.modules.hidrology.dao;

import com.app.core.common.Utils;
import com.app.modules.hidrology.dto.CauceDTO;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

import static com.app.core.jooq.generated.Tables.LECTURA_CAUCES_DIARIA;
import static com.app.core.jooq.generated.Tables.LECTURA_CAUCES_HORARIA;

@Repository
public class CauceDAO {

    @Autowired
    private DSLContext dsl;

    public void insertarDatosCaudalesHorarios(CauceDTO cauceDto, List<Query> inserts, Double ultimoDatoNivel, Double ultimoDatoCaudal, Double porcentajeNivel, Double cotaMaxima, Utils.Coordinates coordinates) {
        inserts.add(dsl.insertInto(LECTURA_CAUCES_HORARIA)
                .set(LECTURA_CAUCES_HORARIA.CODIGO, cauceDto.codigoPuntoMedicion())
                .set(LECTURA_CAUCES_HORARIA.NOMBRE, cauceDto.nombre())
                .set(LECTURA_CAUCES_HORARIA.ULTIMO_DATO_NIVEL, ultimoDatoNivel)
                .set(LECTURA_CAUCES_HORARIA.ULTIMO_DATO_CAUDAL, ultimoDatoCaudal)
                .set(LECTURA_CAUCES_HORARIA.PORCENTAJE_NIVEL, porcentajeNivel)
                .set(LECTURA_CAUCES_HORARIA.COTA_MAXIMA_SECCION, cotaMaxima)
                .set(LECTURA_CAUCES_HORARIA.LATITUD, coordinates.latitud())
                .set(LECTURA_CAUCES_HORARIA.LONGITUD, coordinates.longitud())
                .set(LECTURA_CAUCES_HORARIA.CREATED_AT, OffsetDateTime.now()));
    }

    public void insertarDatosCaudalesDiarios(CauceDTO cauceDto, List<Query> inserts, Double ultimoDatoNivel, Double ultimoDatoCaudal, Double porcentajeNivel, Double cotaMaxima, Utils.Coordinates coordinates) {
        inserts.add(dsl.insertInto(LECTURA_CAUCES_DIARIA)
                .set(LECTURA_CAUCES_DIARIA.CODIGO, cauceDto.codigoPuntoMedicion())
                .set(LECTURA_CAUCES_DIARIA.NOMBRE, cauceDto.nombre())
                .set(LECTURA_CAUCES_DIARIA.ULTIMO_DATO_NIVEL, ultimoDatoNivel)
                .set(LECTURA_CAUCES_DIARIA.ULTIMO_DATO_CAUDAL, ultimoDatoCaudal)
                .set(LECTURA_CAUCES_DIARIA.PORCENTAJE_NIVEL, porcentajeNivel)
                .set(LECTURA_CAUCES_DIARIA.COTA_MAXIMA_SECCION, cotaMaxima)
                .set(LECTURA_CAUCES_DIARIA.LATITUD, coordinates.latitud())
                .set(LECTURA_CAUCES_DIARIA.LONGITUD, coordinates.longitud())
                .set(LECTURA_CAUCES_DIARIA.CREATED_AT, OffsetDateTime.now()));
    }
}
