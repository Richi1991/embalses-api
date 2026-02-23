package com.app.modules.weather.service;

import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.core.jooq.generated.tables.records.EstacionesMeteorologicasRecord;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.HistoricoPrecipitacionesDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import com.app.modules.weather.dto.TemperaturasDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jooq.DSLContext;
import org.jooq.DatePart;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.app.core.jooq.generated.Tables.HISTORICO_PRECIPITACIONES;
import static com.app.core.jooq.generated.Tables.PRECIPITACIONES;
import static com.app.core.jooq.generated.tables.EstacionesMeteorologicas.ESTACIONES_METEOROLOGICAS;
import static org.jooq.impl.DSL.excluded;

@Service
public class HistoricoPrecipitacionesService {

    @Autowired
    private DSLContext dslContext;

    public HistoricoPrecipitacionesService() throws NoSuchAlgorithmException, KeyManagementException {
        this.configureSSL();
    }

    public void insertarHistoricoPrecipitacionesAemet(String provincia, String apiKeyAemet, String fechaInicio, String fechaFin) throws FunctionalExceptions {

        List<EstacionesDTO> estacionesDTOList = this.obtenerEstacionesAemetPorProvincia(provincia, apiKeyAemet);

        List<EstacionesDTO> estacionesDTOListToInsert = new ArrayList<>();

        OkHttpClient client = new OkHttpClient();

        for (EstacionesDTO estacionesDTO : estacionesDTOList) {
            String newUrl = null;
            System.out.println("Inicio de obtención de datos para estacion:" +estacionesDTO.getNombre());
            try {
                Request request = new Request.Builder()
                        .url("https://opendata.aemet.es/opendata/api/valores/climatologicos/diarios/datos/fechaini/"+fechaInicio+"/fechafin/"+fechaFin+"/estacion/"+estacionesDTO.getIndicativo()+"/?api_key=".concat(apiKeyAemet))
                        .get()
                        .addHeader("cache-control", "no-cache")
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonData = response.body().string();
                        JsonNode node = new ObjectMapper().readTree(jsonData);
                        if (node.get("datos").asText() != null) {
                            newUrl = node.get("datos").asText();
                        }
                        if (newUrl != null) {
                            Request requestUrlDatosClimaEstacion = new Request.Builder()
                                    .url(newUrl)
                                    .get()
                                    .addHeader("cache-control", "no-cache")
                                    .build();

                            try (Response responseDatosClimaEstacion = client.newCall(requestUrlDatosClimaEstacion).execute()) {
                                if (responseDatosClimaEstacion.isSuccessful() && responseDatosClimaEstacion.body() != null) {
                                    String jsonDatosClimaEstacion = responseDatosClimaEstacion.body().string();

                                    ObjectMapper mapper = new ObjectMapper();
                                    JsonNode rootNode = mapper.readTree(jsonDatosClimaEstacion);

                                    for (JsonNode nodo : rootNode) {
                                        if (nodo.path("nombre").asText() != null) {
                                            System.out.println("Procesando nodo: " + nodo.path("indicativo").asText() + " fecha: " + nodo.path("fecha").asText());

                                            try {
                                                // Usamos path() para todo. Si el campo no existe, asText es "" y asDouble es 0.0
                                                String indicativo = nodo.path("indicativo").asText();
                                                String fechaStr = nodo.path("fecha").asText();

                                                if (indicativo.isEmpty() || fechaStr.isEmpty()) {
                                                    continue; // Saltamos registros incompletos
                                                }

                                                EstacionesDTO dto = new EstacionesDTO();
                                                dto.setIndicativo(indicativo);
                                                dto.setNombre(nodo.path("nombre").asText());

                                                LocalDate localDate = LocalDate.parse(fechaStr);
                                                dto.setFechaActualizacion(Timestamp.valueOf(localDate.atStartOfDay()));

                                                PrecipitacionesDTO prec = new PrecipitacionesDTO();
                                                // Usamos un helper para limpiar las comas y manejar "Ip"
                                                prec.setPrecipitacion24h(parsearDouble(nodo.path("prec").asText()));
                                                dto.setPrecipitacionesDTO(prec);

                                                TemperaturasDTO temp = new TemperaturasDTO();
                                                temp.setTmed(parsearDouble(nodo.path("tmed").asText()));
                                                temp.setTmin(parsearDouble(nodo.path("tmin").asText()));
                                                temp.setTmax(parsearDouble(nodo.path("tmax").asText()));
                                                dto.setTemperaturasDTO(temp);

                                                estacionesDTOListToInsert.add(dto);

                                            } catch (Exception e) {
                                                System.err.println("Error en nodo de estación " + nodo.path("indicativo").asText() + ": " + e.getMessage());
                                            }
                                        }

                                    }
                                } else {
                                    System.out.println("No se han obtenido datos:" +responseDatosClimaEstacion.body());
                                }
                            }catch (IOException e) {
                                System.out.println("Error en la response DatosClimaEstacion:" + requestUrlDatosClimaEstacion.body());

                            }
                        }

                    } else {
                        System.out.println("No se han obtenido datos:" +response.body());
                    }
                }catch (IOException e) {
                    System.out.println("Error en la response:" + request.body());
                }
            }  catch (Exception e) {
                System.out.println("No hay datos para la estacion:" + estacionesDTO.getNombre());
            }
        }
        System.out.println("estacionesDTOListToInsert: "+estacionesDTOListToInsert);
        this.insertarDatosClimatologicosAemetFilterByProvincia(estacionesDTOListToInsert);
    }

    public void insertarDatosClimatologicosAemetFilterByProvincia(List<EstacionesDTO> estacionesDTOListToInsert) {

        List<HistoricoPrecipitacionesDTO> historicoPrecipitacionesList = estacionesDTOListToInsert.stream()
                .map(dto -> {
                    HistoricoPrecipitacionesDTO entidad = new HistoricoPrecipitacionesDTO();
                    entidad.setIndicativo(dto.getIndicativo());
                    entidad.setNombre(dto.getNombre());
                    entidad.setValor24h(dto.getPrecipitacionesDTO() != null ? dto.getPrecipitacionesDTO().getPrecipitacion24h() : null);
                    entidad.setFechaRegistro(dto.getFechaActualizacion());
                    entidad.setTmax(dto.getTemperaturasDTO() != null ? dto.getTemperaturasDTO().getTmax() : null);
                    entidad.setTmin(dto.getTemperaturasDTO() != null ? dto.getTemperaturasDTO().getTmin() : null);
                    entidad.setTmed(dto.getTemperaturasDTO() != null ? dto.getTemperaturasDTO().getTmed() : null);
                    return entidad;
                }).toList();


        insertarHistoricoPrecipitacionesList(historicoPrecipitacionesList);

        System.out.println("Guardado de datos realizado correctamente en Tabla HistoricoPrecipitaciones");
    }

    private void insertarHistoricoPrecipitacionesList(List<HistoricoPrecipitacionesDTO> lista) {
        // Definimos la query base
        var query = dslContext.insertInto(HISTORICO_PRECIPITACIONES)
                .columns(
                        HISTORICO_PRECIPITACIONES.INDICATIVO,
                        HISTORICO_PRECIPITACIONES.NOMBRE,
                        HISTORICO_PRECIPITACIONES.VALOR_24H,
                        HISTORICO_PRECIPITACIONES.FECHA_REGISTRO,
                        HISTORICO_PRECIPITACIONES.TMAX,
                        HISTORICO_PRECIPITACIONES.TMIN,
                        HISTORICO_PRECIPITACIONES.TMED
                )
                .values((String) null, null, null, null, null, null, null)
                .onConflict(HISTORICO_PRECIPITACIONES.INDICATIVO, HISTORICO_PRECIPITACIONES.FECHA_REGISTRO)
                .doUpdate()
                .set(HISTORICO_PRECIPITACIONES.VALOR_24H, excluded(HISTORICO_PRECIPITACIONES.VALOR_24H))
                .set(HISTORICO_PRECIPITACIONES.TMAX, excluded(HISTORICO_PRECIPITACIONES.TMAX))
                .set(HISTORICO_PRECIPITACIONES.TMIN, excluded(HISTORICO_PRECIPITACIONES.TMIN))
                .set(HISTORICO_PRECIPITACIONES.TMED, excluded(HISTORICO_PRECIPITACIONES.TMED));

        // Ejecución en Batch
        dslContext.batch(query)
                .bind(lista.stream().map(h -> new Object[] {
                        h.getIndicativo(),
                        h.getNombre(),
                        h.getValor24h(),
                        h.getFechaRegistro(),
                        h.getTmax(),
                        h.getTmin(),
                        h.getTmed()
                }).toArray(Object[][]::new))
                .execute();
    }

    private List<EstacionesDTO> obtenerEstacionesAemetPorProvincia(String provincia, String apiKeyAemet) throws FunctionalExceptions {
        OkHttpClient client = new OkHttpClient();
        List<EstacionesDTO> estacionesAemetDTOListFilterByProvincia = new ArrayList<>();
        String newUrl = null;
        try {
            Request request = new Request.Builder()
                    .url("https://opendata.aemet.es/opendata/api/valores/climatologicos/inventarioestaciones/todasestaciones/?api_key=".concat(apiKeyAemet))
                    .get()
                    .addHeader("cache-control", "no-cache")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();

                    // 2. Extraer la URL de "datos"
                    JsonNode node = new ObjectMapper().readTree(jsonData);
                    newUrl = node.get("datos").asText();
                    if (newUrl != null) {
                        Request requestUrlEstaciones = new Request.Builder()
                                .url(newUrl)
                                .get()
                                .addHeader("cache-control", "no-cache")
                                .build();
                        try (Response responseEstaciones = client.newCall(requestUrlEstaciones).execute()) {
                            if (responseEstaciones.isSuccessful() && responseEstaciones.body() != null) {
                                String jsonDataEstaciones = responseEstaciones.body().string();

                                ObjectMapper mapper = new ObjectMapper();
                                JsonNode rootNode = mapper.readTree(jsonDataEstaciones);

                                for (JsonNode nodo : rootNode) {
                                    // Usamos .asText() para obtener el String
                                    if (nodo.get("provincia").asText().equals(provincia)) {
                                        EstacionesDTO estacionesDTO = new EstacionesDTO();

                                        estacionesDTO.setLatitud(nodo.get("latitud").asText());
                                        estacionesDTO.setProvincia(nodo.get("provincia").asText());

                                        // Para el short, usamos asInt() y casteamos
                                        estacionesDTO.setAltitud((short) nodo.get("altitud").asInt());

                                        estacionesDTO.setIndicativo(nodo.get("indicativo").asText());
                                        estacionesDTO.setNombre(nodo.get("nombre").asText());
                                        estacionesDTO.setIndsinop(nodo.get("indsinop").asText());
                                        estacionesDTO.setLongitud(nodo.get("longitud").asText());

                                        estacionesAemetDTOListFilterByProvincia.add(estacionesDTO);
                                    }
                                }

                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
        }
        return estacionesAemetDTOListFilterByProvincia;
    }

    private Double parsearDouble(String valor) {
        if (valor == null || valor.isEmpty() || valor.equalsIgnoreCase("Ip")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public void insertarHistoricoPrecipitacionesChs(LocalDate localDateFechaInicio, LocalDate localDateFechaFin) throws FunctionalExceptions {
        OkHttpClient client = new OkHttpClient();
        DateTimeFormatter fmtCompacto = DateTimeFormatter.ofPattern("yyyyMMdd");
        List<HistoricoPrecipitacionesDTO> historicoPrecipitacionesList = new ArrayList<>();

        // 1. CARGA PREVIA: Obtenemos todas las estaciones de una sola vez
        Map<String, EstacionesMeteorologicasRecord> mapaEstaciones = dslContext
                .selectFrom(ESTACIONES_METEOROLOGICAS)
                .fetchMap(ESTACIONES_METEOROLOGICAS.INDICATIVO);

        LocalDate current = localDateFechaInicio;

        while (!current.isAfter(localDateFechaFin)) {
            // Convertimos a Timestamp (o LocalDateTime si tu jOOQ lo prefiere)
            Timestamp timestampFechaInicio = Timestamp.valueOf(current.atStartOfDay());

            System.out.println("Extrayendo datos PDF para: " + timestampFechaInicio);
            String anoHidrologico = obtenerAnoHidrologico(current);
            String fechaUrl = current.format(fmtCompacto);

            try {
                Request request = new Request.Builder()
                        .url("https://www.chsegura.es/static/hidro_SAIH/" + anoHidrologico + "/InformeDiarioPrecipSAIH_" + fechaUrl + ".pdf")
                        .get()
                        .addHeader("cache-control", "no-cache")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        try (InputStream inputStream = response.body().byteStream();
                             PDDocument document = PDDocument.load(new BufferedInputStream(inputStream))) {

                            String contenidoPdf = new PDFTextStripper().getText(document);
                            Map<String, Double> mapPreci = extraerTotalesDesdePDF(contenidoPdf);

                            // 2. PROCESAMIENTO EN MEMORIA: Mucho más rápido
                            mapPreci.forEach((indicativo, valor) -> {
                                EstacionesMeteorologicasRecord estacion = mapaEstaciones.get(indicativo);

                                if (estacion != null) {
                                    HistoricoPrecipitacionesDTO h = new HistoricoPrecipitacionesDTO();
                                    h.setIndicativo(estacion.getIndicativo());
                                    h.setNombre(estacion.getNombre());
                                    h.setValor24h(valor);
                                    h.setFechaRegistro(timestampFechaInicio);
                                    historicoPrecipitacionesList.add(h);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando PDF del día " + current + ": " + e.getMessage());
                    // Aquí podrías decidir si continuar con el siguiente día o lanzar excepción
                }
            } catch (Exception e) {
                Exceptions.EMB_E_0010.lanzarExcepcionWithParams("Error en request SAIH: " + e.getMessage());
            }

            current = current.plusDays(1);
        }

        // 3. INSERCIÓN BATCH: Usando el método que creamos antes
        if (!historicoPrecipitacionesList.isEmpty()) {
            this.insertarHistoricoPrecipitacionesList(historicoPrecipitacionesList);
            System.out.println("Insertados " + historicoPrecipitacionesList.size() + " registros.");
        }
    }

    public LocalDate parseStringToLocalDate(String fecha, DateTimeFormatter formatter) {
        return LocalDate.parse(fecha, formatter);
    }

    private String obtenerAnoHidrologico(LocalDate localDateFechaInicio) {
        String anoHidrologico;
        // Si el mes es >= 10, el año de inicio es el año de la fecha.
        // Si no, el año de inicio es el año anterior.
        int year = (localDateFechaInicio.getMonthValue() >= 10)
                ? localDateFechaInicio.getYear()
                : localDateFechaInicio.getYear() - 1;

        String anioActual = String.valueOf(year);
        String anioSiguienteCorto = String.valueOf((year + 1) % 100);

        // Asegurar que si el año es 2004-2005, el final sea "05" y no "5"
        if (anioSiguienteCorto.length() == 1) anioSiguienteCorto = "0" + anioSiguienteCorto;

        anoHidrologico = anioActual.concat("-").concat(anioSiguienteCorto);
        return anoHidrologico;
    }

    public Map<String, Double> extraerTotalesDesdePDF(String pdfTexto) {
        Map<String, Double> resultados = new HashMap<>();

        // Explicación del Regex Actualizado:
        // ([0-9]{2}[A-Z][0-9A-Z]{4})   -> Grupo 1: Captura exactamente 7 caracteres alfanuméricos
        //                                 (2 números, 1 letra, 4 alfanuméricos).
        // .*?                          -> Salta el nombre de la estación.
        // (\d+,\d+)                    -> Grupo 2: Captura un valor decimal.
        // (?!.*\d+,\d+)                -> Asegura que sea el ÚLTIMO valor decimal de esa línea.

        String regex = "([0-9]{2}[A-Z][0-9A-Z]{4}).*?(\\d+,\\d+)(?!.*\\d+,\\d+)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(pdfTexto);

        while (matcher.find()) {
            String indicativoPDF = matcher.group(1);
            String valorStr = matcher.group(2);

            // Aplicamos la normalización para pasarlo de 7 a 8 caracteres (P1 -> P01)
            String indicativoBD = normalizarIndicativo(indicativoPDF);

            try {
                Double valor = Double.parseDouble(valorStr.replace(",", "."));
                resultados.put(indicativoBD, valor);
            } catch (NumberFormatException e) {
                // Error de parseo, se ignora
            }
        }
        return resultados;
    }

    private String normalizarIndicativo(String indicativo) {
        // Si mide 7 y termina en P1, lo transformamos a P01 (8 caracteres)
        if (indicativo != null && indicativo.length() == 7 && indicativo.endsWith("P1")) {
            return indicativo.substring(0, 5) + "P01";
        }
        return indicativo; // Si no cumple, lo devuelve igual (aunque el regex ya filtra por 7)
    }

    public void configureSSL() throws NoSuchAlgorithmException, KeyManagementException {
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                }
        };

        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    public void insertarHistoricoPrecipitacionesChsFromPrecipitaciones(int days) {

        java.time.LocalDateTime fechaLimite = java.time.LocalDateTime.now().minusDays(days);

        List<HistoricoPrecipitacionesDTO> historicoPrecipitacionesList = dslContext
                .select(
                        PRECIPITACIONES.INDICATIVO,
                        PRECIPITACIONES.NOMBRE,
                        DSL.trunc(PRECIPITACIONES.FECHA_ACTUALIZACION, DatePart.DAY).as("fechaActualizacion"),
                        DSL.max(PRECIPITACIONES.PRECIPITACION_24H).as("maximo24h")
                )
                .from(PRECIPITACIONES)
                .where(PRECIPITACIONES.FECHA_ACTUALIZACION.greaterOrEqual(fechaLimite))
                .groupBy(
                        PRECIPITACIONES.INDICATIVO,
                        PRECIPITACIONES.NOMBRE,
                        DSL.trunc(PRECIPITACIONES.FECHA_ACTUALIZACION, DatePart.DAY)
                )
                .orderBy(DSL.field("fechaActualizacion").desc(), PRECIPITACIONES.INDICATIVO.asc())
                .fetch(record -> {
                    HistoricoPrecipitacionesDTO dto = new HistoricoPrecipitacionesDTO();
                    dto.setFechaRegistro(record.get("fechaActualizacion", Timestamp.class));
                    dto.setNombre(record.get(PRECIPITACIONES.NOMBRE));
                    dto.setIndicativo(record.get(PRECIPITACIONES.INDICATIVO));
                    dto.setValor24h(record.get("maximo24h", Double.class));
                    return dto;
                });

        this.insertarHistoricoPrecipitacionesList(historicoPrecipitacionesList);
        System.out.println("Valores Insertados en tabla Historico Precipitaciones");
    }
}
