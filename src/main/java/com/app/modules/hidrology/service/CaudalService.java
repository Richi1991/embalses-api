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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
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

            Map<String, Double[]> mapaCodigoCoordenadas = Utils.obtenerMapaCoordenadasCauces();

            List<Query> inserts = new ArrayList<>();
            caudalDTOList.stream().forEach(caudalDTO -> {

                Double[] coordinates = mapaCodigoCoordenadas.get(caudalDTO.codigoPuntoMedicion());

                Double cotaMaxima = 0.0;
                if (!caudalDTO.cotaMaxima().contains("-")) {
                    cotaMaxima = Double.parseDouble(caudalDTO.cotaMaxima().replace(",", "."));
                }

                inserts.add(dsl.insertInto(CAUCES)
                        .set(CAUCES.CODIGO, caudalDTO.codigoPuntoMedicion())
                        .set(CAUCES.NOMBRE, caudalDTO.nombre())
                        .set(CAUCES.COTAMAXIMA, cotaMaxima)
                        .set(CAUCES.CREATED_AT, OffsetDateTime.now())
                        .set(CAUCES.LATITUD, coordinates[0])
                        .set(CAUCES.LONGITUD, coordinates[1]));
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
            List<CaudalDTO> caudalDTOList = getTodosCaudales();

            Map<String, Double[]> mapaCodigoCoordenadas = Utils.obtenerMapaCoordenadasCauces();

            List<Query> inserts = new ArrayList<>();
            caudalDTOList.stream().forEach(caudalDTO -> {

                Double[] coordinates = mapaCodigoCoordenadas.get(caudalDTO.codigoPuntoMedicion());

                if (coordinates != null) {
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
                }
            });

            if (!inserts.isEmpty()) {
                dsl.batch(inserts).execute();
            }

        } catch (IOException| NoSuchAlgorithmException| KeyManagementException  e) {
            Exceptions.CAU_E_0001.lanzarExcepcionCausada(e);
        }
    }

    public List<CaudalDTO> getTodosCaudales() throws IOException, NoSuchAlgorithmException, KeyManagementException {

        // Crear el SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        }, new SecureRandom());

        HttpClient client = HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        List<String> acciones = List.of(
                "action=consultar_cauces_topo",
                "action=consultar_cauces_topo_zona&zonatxt=1cab-bayo",
                "action=consultar_cauces_topo_zona&zonatxt=2bayo-ojos",
                "action=consultar_cauces_topo_zona&zonatxt=3ojos-murcia",
                "action=consultar_cauces_topo_zona&zonatxt=4guadalentin",
                "action=consultar_cauces_topo_zona&zonatxt=5vegabaja1",
                "action=consultar_cauces_topo_zona&zonatxt=6vegabaja2",
                "action=consultar_cauces_topo_zona&zonatxt=7ramblascosteras",
                "action=consultar_cauces_topo_zona&zonatxt=8contrap-beniel");

        ObjectMapper mapper = new ObjectMapper();

        List<CompletableFuture<List<CaudalDTO>>> futures = acciones.stream()
                .map(accion -> {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://saihweb.chsegura.es/apps/iVisor/obtener_datos.php"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(accion))
                            .timeout(Duration.ofSeconds(10))
                            .build();

                    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenApply(HttpResponse::body)
                            .thenApply(body -> {
                                try {
                                    return Arrays.stream(
                                            mapper.readValue(body, CaudalDTO[].class)
                                    ).toList();
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                })
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        CaudalDTO::codigoPuntoMedicion,      // clave única
                        dto -> dto,                           // valor
                        (existing, duplicate) -> existing     // si hay duplicado, quedarse con el primero
                ))
                .values()
                .stream()
                .toList();

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
