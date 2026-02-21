package com.app.modules.weather.service;

import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.core.model.EstacionesMeteorologicas;
import com.app.core.model.HistoricoPrecipitaciones;
import com.app.core.model.PrecipitacionLastDays;
import com.app.core.repository.*;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import com.app.modules.weather.dto.TemperaturasDTO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HistoricoPrecipitacionesService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HistoricoPrecipitacionesRepository historicoPrecipitacionesRepository;

    @Autowired
    private EstacionesMeteorologicasRepository estacionesMeteorologicasRepository;

    @Autowired
    private PrecipitacionesRepository precipitacionesRepository;

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

        List<HistoricoPrecipitaciones> historicoPrecipitacionesList = estacionesDTOListToInsert.stream()
                .map(dto -> {
                    HistoricoPrecipitaciones entidad = new HistoricoPrecipitaciones();
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

    private void insertarHistoricoPrecipitacionesList(List<HistoricoPrecipitaciones> historicoPrecipitacionesList) {
        String sql = "INSERT INTO historico_precipitaciones (indicativo, nombre, valor_24h, fecha_registro, tmax, tmin, tmed) " +
                " VALUES (?, ?, ?, ?, ?, ?, ?) " +
                " ON CONFLICT (indicativo, fecha_registro) DO UPDATE SET " +
                " valor_24h = EXCLUDED.valor_24h, " +
                " tmax = EXCLUDED.tmax, " +
                " tmin = EXCLUDED.tmin, " +
                " tmed = EXCLUDED.tmed";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                HistoricoPrecipitaciones h = historicoPrecipitacionesList.get(i);
                ps.setString(1, h.getIndicativo());
                ps.setString(2, h.getNombre());
                ps.setObject(3, h.getValor24h());
                ps.setTimestamp(4, h.getFechaRegistro());
                ps.setObject(5, h.getTmax());
                ps.setObject(6, h.getTmin());
                ps.setObject(7, h.getTmed());
            }

            @Override
            public int getBatchSize() {
                return historicoPrecipitacionesList.size();
            }
        });
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
                                    if (nodo.get("provincia").asString().equals(provincia)) {
                                        EstacionesDTO estacionesDTO = new EstacionesDTO();
                                        estacionesDTO.setLatitud(nodo.get("latitud").asString());
                                        estacionesDTO.setProvincia(nodo.get("provincia").asString());
                                        estacionesDTO.setAltitud(nodo.get("altitud").asLong());
                                        estacionesDTO.setIndicativo(nodo.get("indicativo").asString());
                                        estacionesDTO.setNombre(nodo.get("nombre").asString());
                                        estacionesDTO.setIndsinop(nodo.get("indsinop").asString());
                                        estacionesDTO.setLongitud(nodo.get("longitud").asString());
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

        // Definimos el formateador una sola vez fuera del bucle para mejor rendimiento
        DateTimeFormatter fmtCompacto = DateTimeFormatter.ofPattern("yyyyMMdd");

        List<HistoricoPrecipitaciones> historicoPrecipitacionesList = new ArrayList<>();

        while (localDateFechaInicio.isBefore(localDateFechaFin) || localDateFechaInicio.isEqual(localDateFechaFin)) {

            Timestamp timestampFechaInicio = Timestamp.from(localDateFechaInicio.atStartOfDay(ZoneId.systemDefault()).toInstant());

            System.out.println("Se comienza a extrar datos del pdf para la fecha: " +timestampFechaInicio);

            String anoHidrologico = obtenerAnoHidrologico(localDateFechaInicio);

            // Usamos el formato yyyyMMdd para la URL (ej: 20251201)
            String fechaUrl = localDateFechaInicio.format(fmtCompacto);

            try {
                Request request = new Request.Builder()
                        .url("https://www.chsegura.es/static/hidro_SAIH/" + anoHidrologico + "/InformeDiarioPrecipSAIH_" + fechaUrl + ".pdf")
                        .get()
                        .addHeader("cache-control", "no-cache")
                        .build();
                try (Response responseEstaciones = client.newCall(request).execute()) {
                    if (responseEstaciones.isSuccessful() && responseEstaciones.body() != null) {
                        // 1. Obtenemos el InputStream desde el cuerpo de la respuesta
                        try (InputStream inputStream = responseEstaciones.body().byteStream();
                             BufferedInputStream bufferedIn = new BufferedInputStream(inputStream);
                             PDDocument document = PDDocument.load(bufferedIn)) {

                            // 2. Usamos PDFTextStripper para extraer el texto
                            PDFTextStripper stripper = new PDFTextStripper();
                            String contenidoPdf = stripper.getText(document);

                            Map<String, Double> mapIndicativoPrecipitacionDiaria = extraerTotalesDesdePDF(contenidoPdf);

                            mapIndicativoPrecipitacionDiaria.entrySet().stream().forEach(indicativoPreciDiaria -> {
                                EstacionesMeteorologicas estacionMeteo = estacionesMeteorologicasRepository.findByIndicativo(indicativoPreciDiaria.getKey());

                                if (estacionMeteo != null) {
                                    HistoricoPrecipitaciones historicoPrecipitaciones = new HistoricoPrecipitaciones();
                                    historicoPrecipitaciones.setIndicativo(estacionMeteo.getIndicativo());
                                    historicoPrecipitaciones.setNombre(estacionMeteo.getNombre());
                                    historicoPrecipitaciones.setValor24h(indicativoPreciDiaria.getValue());
                                    historicoPrecipitaciones.setFechaRegistro(timestampFechaInicio);
                                    historicoPrecipitacionesList.add(historicoPrecipitaciones);
                                }
                            });
                        } catch (IOException e) {
                            System.err.println("Error al procesar el PDF: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error en la response obteniendo el informe diario de precipitaciones del SAIH CHS: " +e);
                    Exceptions.EMB_E_0011.lanzarExcepcionWithParams(e.getMessage().concat("Error en la response obteniendo el informe diario de precipitaciones del SAIH CHS"));
                }

            } catch (Exception e) {
                Exceptions.EMB_E_0010.lanzarExcepcionWithParams(e.getMessage().concat("error en la request obteniendo el informe diario de precipitaciones del SAIH CHS"));
            }


            // HACER AQUÍ: Ejecutar la llamada (client.newCall(request).execute()...)

            // AVANZAR UN DÍA: Reasignamos la variable para evitar bucle infinito
            localDateFechaInicio = localDateFechaInicio.plusDays(1);
        }

        this.insertarHistoricoPrecipitacionesList(historicoPrecipitacionesList);
        System.out.println("Valores Insertados en tabla Historico Precipitaciones");
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

    public List<AcumuladoEstacion> obtenerValoresPrecipitacionesAcumulados(String rango) {

        return historicoPrecipitacionesRepository.findAcumuladosDinamicos(rango);
    }

    public Timestamp fechaStringToTimestamp(String fechaStr) {
        // 1. Definimos el formato de 8 dígitos que recibes
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // 2. Parseamos como LocalDate (esto NO fallará porque solo busca la fecha)
        LocalDate fecha = parseStringToLocalDate(fechaStr, formatter);

        // 3. Convertimos a Timestamp asignando las 00:00:00 horas
        return Timestamp.valueOf(fecha.atStartOfDay());
    }

    public void insertarHistoricoPrecipitacionesChsFromPrecipitaciones(int days) {

        List<PrecipitacionLastDays> precipitacionesList = precipitacionesRepository.findPrecipitacionesLastDays(days);

        List<HistoricoPrecipitaciones> historicoPrecipitacionesList = precipitacionesList.stream()
                .map(prec -> new HistoricoPrecipitaciones(
                        java.sql.Timestamp.valueOf(prec.getFechaActualizacion()),
                        prec.getNombre(),
                        prec.getIndicativo(),
                        prec.getMaximo24h()
                ))
                .toList();

        this.insertarHistoricoPrecipitacionesList(historicoPrecipitacionesList);
        System.out.println("Valores Insertados en tabla Historico Precipitaciones");
    }
}
