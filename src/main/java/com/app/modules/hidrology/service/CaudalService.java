package com.app.modules.hidrology.service;

import com.app.core.common.Utils;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.dao.CaudalDAO;
import com.app.modules.hidrology.dto.CaudalDTO;
import com.app.modules.hidrology.dto.EstadisticaCuencaDTO;
import com.app.modules.hidrology.dto.UltimaLecturaCaudalDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.app.core.jooq.generated.Tables.CAUCES;

@Service
public class CaudalService {

    @Autowired
    private CaudalDAO caudalDAO;

    @Autowired
    private DSLContext dsl;

    public CaudalService() throws NoSuchAlgorithmException, KeyManagementException {
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
            List<CaudalDTO> caudalDTOList = Stream.of(mapper.readValue(jsonCaudales, CaudalDTO[].class)).toList();

            List<Query> inserts = new ArrayList<>();
            caudalDTOList.stream().forEach(caudalDTO -> {

                Utils.Coordinates coordinates = Utils.convert(caudalDTO.latitud(), caudalDTO.latitud());

                Double cotaMaxima = 0.0;
                if (!caudalDTO.cotaMaxima().contains("-")) {
                    cotaMaxima = Double.parseDouble(caudalDTO.cotaMaxima().replace(",", "."));
                }

                inserts.add(dsl.insertInto(CAUCES)
                        .set(CAUCES.CODIGO, caudalDTO.codigoPuntoMedicion())
                        .set(CAUCES.NOMBRE, caudalDTO.nombre())
                        .set(CAUCES.COTAMAXIMA, cotaMaxima)
                        .set(CAUCES.CREATED_AT, OffsetDateTime.now())
                        .set(CAUCES.LATITUD, coordinates.latitud())
                        .set(CAUCES.LONGITUD, coordinates.longitud()));
            });

            if (!inserts.isEmpty()) {
                dsl.batch(inserts).execute();
            }

        } catch (IOException e) {
            Exceptions.CAU_E_0001.lanzarExcepcionCausada(e);
        }
    }

    public void insertCaudalesRealTime(Boolean isHorario) throws FunctionalExceptions {
        try {
            Document doc = Jsoup.connect("https://saihweb.chsegura.es/apps/iVisor/obtener_datos.php")
                    .data("action", "consultar_cauces_topo")
                    .timeout(10000)
                    .post();

            String jsonCaudales = doc.body().text();
            ObjectMapper mapper = new ObjectMapper();
            List<CaudalDTO> caudalDTOList = Stream.of(mapper.readValue(jsonCaudales, CaudalDTO[].class)).toList();

            List<Query> inserts = new ArrayList<>();
            caudalDTOList.stream().forEach(caudalDTO -> {

                Utils.Coordinates coordinates = Utils.convert(caudalDTO.latitud(), caudalDTO.latitud());

                Double cotaMaxima = 0.0;
                if (!caudalDTO.cotaMaxima().contains("-")) {
                    cotaMaxima = Double.parseDouble(caudalDTO.cotaMaxima().replace(",", "."));
                }

                Double ultimoDatoNivel = 0.0;
                if (!caudalDTO.ultimoDatoNivel().contains("-")) {
                    ultimoDatoNivel = Double.parseDouble(caudalDTO.ultimoDatoNivel().replace(",", "."));
                }

                Double ultimoDatoCaudal = 0.0;
                if (!caudalDTO.ultimoDatoCaudal().contains("-")) {
                    ultimoDatoCaudal = Double.parseDouble(caudalDTO.ultimoDatoCaudal().replace(",", "."));
                }

                Double porcentajeNivel = 0.0;
                if (!caudalDTO.porcentaje().contains("-")) {
                    porcentajeNivel = Double.parseDouble(caudalDTO.porcentaje().replace(",", "."));
                }

                if (isHorario) {
                    caudalDAO.insertarDatosCaudalesHorarios(caudalDTO, inserts, ultimoDatoNivel, ultimoDatoCaudal, porcentajeNivel, cotaMaxima, coordinates);
                } else {
                    caudalDAO.insertarDatosCaudalesDiarios(caudalDTO, inserts, ultimoDatoNivel, ultimoDatoCaudal, porcentajeNivel, cotaMaxima, coordinates);
                }

            });

            if (!inserts.isEmpty()) {
                dsl.batch(inserts).execute();
            }

        } catch (IOException e) {
            Exceptions.CAU_E_0001.lanzarExcepcionCausada(e);
        }
    }

    public List<EstadisticaCuencaDTO> getCaudalesHorariosChsFilteredByDay(int days) throws FunctionalExceptions {
        return caudalDAO.getCaudalesHorariosChsFilteredByDay(days);
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

    public List<UltimaLecturaCaudalDTO> getLastCaudalAndPosition() throws FunctionalExceptions {
        return caudalDAO.getLastCaudalAndPosition();
    }
}
