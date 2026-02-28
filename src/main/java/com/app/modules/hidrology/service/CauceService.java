package com.app.modules.hidrology.service;

import com.app.core.common.Utils;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.dao.CauceDAO;
import com.app.modules.hidrology.dto.CauceDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Array;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.app.core.jooq.generated.Tables.CAUCES;
import static com.app.core.jooq.generated.Tables.LECTURA_CAUCES_HORARIA;

@Service
public class CauceService {

    @Autowired
    private CauceDAO cauceDAO;

    @Autowired
    private DSLContext dsl;

    public CauceService() throws NoSuchAlgorithmException, KeyManagementException {
        this.configureSSL();
    }

    public void getAndInsertCaucesChs() throws FunctionalExceptions {
        try {
            Document doc = Jsoup.connect("https://saihweb.chsegura.es/apps/iVisor/obtener_datos.php")
                    .data("action", "consultar_cauces_topo")
                    .timeout(10000)
                    .post();

            String jsonCaudales = doc.body().text();
            ObjectMapper mapper = new ObjectMapper();
            List<CauceDTO> cauceDTOList = Stream.of(mapper.readValue(jsonCaudales, CauceDTO[].class)).toList();

            List<Query> inserts = new ArrayList<>();
            cauceDTOList.stream().forEach(cauceDto -> {

                Utils.Coordinates coordinates = Utils.convert(cauceDto.latitud(), cauceDto.latitud());

                Double cotaMaxima = 0.0;
                if (!cauceDto.cotaMaxima().contains("-")) {
                    cotaMaxima = Double.parseDouble(cauceDto.cotaMaxima().replace(",", "."));
                }

                inserts.add(dsl.insertInto(CAUCES)
                        .set(CAUCES.CODIGO, cauceDto.codigoPuntoMedicion())
                        .set(CAUCES.NOMBRE, cauceDto.nombre())
                        .set(CAUCES.COTAMAXIMA, cotaMaxima)
                        .set(CAUCES.CREATED_AT, OffsetDateTime.now())
                        .set(CAUCES.LATITUD, coordinates.latitud())
                        .set(CAUCES.LONGITUD, coordinates.longitud()));
            });

            if (!inserts.isEmpty()) {
                dsl.batch(inserts).execute();
            }

        } catch (IOException e) {
            Exceptions.EMB_E_0016.lanzarExcepcionCausada(e);
        }
    }

    public void insertCaudalesRealTime() throws FunctionalExceptions {
        try {
            Document doc = Jsoup.connect("https://saihweb.chsegura.es/apps/iVisor/obtener_datos.php")
                    .data("action", "consultar_cauces_topo")
                    .timeout(10000)
                    .post();

            String jsonCaudales = doc.body().text();
            ObjectMapper mapper = new ObjectMapper();
            List<CauceDTO> cauceDTOList = Stream.of(mapper.readValue(jsonCaudales, CauceDTO[].class)).toList();

            List<Query> inserts = new ArrayList<>();
            cauceDTOList.stream().forEach(cauceDto -> {

                Utils.Coordinates coordinates = Utils.convert(cauceDto.latitud(), cauceDto.latitud());

                Double cotaMaxima = 0.0;
                if (!cauceDto.cotaMaxima().contains("-")) {
                    cotaMaxima = Double.parseDouble(cauceDto.cotaMaxima().replace(",", "."));
                }

                Double ultimoDatoNivel = 0.0;
                if (!cauceDto.ultimoDatoNivel().contains("-")) {
                    ultimoDatoNivel = Double.parseDouble(cauceDto.ultimoDatoNivel().replace(",", "."));
                }

                Double ultimoDatoCaudal = 0.0;
                if (!cauceDto.ultimoDatoCaudal().contains("-")) {
                    ultimoDatoCaudal = Double.parseDouble(cauceDto.ultimoDatoCaudal().replace(",", "."));
                }

                Double porcentajeNivel = 0.0;
                if (!cauceDto.porcentaje().contains("-")) {
                    porcentajeNivel = Double.parseDouble(cauceDto.porcentaje().replace(",", "."));
                }

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
            });

            if (!inserts.isEmpty()) {
                dsl.batch(inserts).execute();
            }

        } catch (IOException e) {
            Exceptions.EMB_E_0016.lanzarExcepcionCausada(e);
        }
    }

    public void configureSSL() throws NoSuchAlgorithmException, KeyManagementException {
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
}
