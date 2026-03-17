package com.app.modules.weather.service;

import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.PluvioChsDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.app.core.jooq.generated.Tables.ESTACIONES_METEOROLOGICAS;
import static com.app.core.jooq.generated.Tables.PRECIPITACIONES;

@Service
public class PrecipitacionesService {

    @Autowired
    private DSLContext dslContext;

    private double limpiarValor(String valor) {
        try {
            if (valor == null || valor.equals("-") || valor.isEmpty()) return 0.0;
            return Double.parseDouble(valor.replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public List<EstacionesDTO> obtenerMapaRapido() {
        return dslContext.select(
                        // Seleccionamos los campos de la tabla generada por jOOQ
                        ESTACIONES_METEOROLOGICAS.NOMBRE,
                        ESTACIONES_METEOROLOGICAS.INDICATIVO,
                        ESTACIONES_METEOROLOGICAS.LATITUD,
                        ESTACIONES_METEOROLOGICAS.LONGITUD,
                        PRECIPITACIONES.PRECIPITACION_1H,
                        PRECIPITACIONES.PRECIPITACION_3H,
                        PRECIPITACIONES.PRECIPITACION_6H,
                        PRECIPITACIONES.PRECIPITACION_12H,
                        PRECIPITACIONES.PRECIPITACION_24H,
                        PRECIPITACIONES.FECHA_ACTUALIZACION
                )
                .distinctOn(ESTACIONES_METEOROLOGICAS.INDICATIVO) // Si falla, usa dsl.selectDistinctOn(...)
                .from(ESTACIONES_METEOROLOGICAS)
                .leftJoin(PRECIPITACIONES)
                .on(ESTACIONES_METEOROLOGICAS.INDICATIVO.eq(PRECIPITACIONES.INDICATIVO))
                .orderBy(ESTACIONES_METEOROLOGICAS.INDICATIVO, PRECIPITACIONES.FECHA_ACTUALIZACION.desc())
                .fetchInto(EstacionesDTO.class);
    }

    private HttpClient buildHttpClient() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
        }, new SecureRandom());

        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private List<PluvioChsDTO> fetchPluvios(HttpClient client, int tipo) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://saihweb.chsegura.es/apps/iVisor/obtener_datos.php"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("action=consultar_pluvios&tipo=" + tipo))
                .timeout(Duration.ofSeconds(15))
                .build();

        String body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
        return Arrays.asList(new ObjectMapper().readValue(body, PluvioChsDTO[].class));
    }

    public void getAndSavePrecipitacionesRealTime() {
        try {
            HttpClient client = buildHttpClient();

            // Obtenemos los 6 tipos de acumulado (1h, 3h, 4h, 6h, 12h, 24h)
            // tipo=0 es instantánea, la omitimos porque no la guardábamos antes
            List<PluvioChsDTO> t1  = fetchPluvios(client, 1);
            List<PluvioChsDTO> t2  = fetchPluvios(client, 2);
            List<PluvioChsDTO> t4  = fetchPluvios(client, 4);
            List<PluvioChsDTO> t5  = fetchPluvios(client, 5);
            List<PluvioChsDTO> t6  = fetchPluvios(client, 6);

            // Indexamos por código para cruzar los 5 tipos fácilmente
            Map<String, PluvioChsDTO> map1h  = t1.stream().collect(Collectors.toMap(PluvioChsDTO::codPuntoMedicion, d -> d, (a, b) -> a));
            Map<String, PluvioChsDTO> map3h  = t2.stream().collect(Collectors.toMap(PluvioChsDTO::codPuntoMedicion, d -> d, (a, b) -> a));
            Map<String, PluvioChsDTO> map6h  = t4.stream().collect(Collectors.toMap(PluvioChsDTO::codPuntoMedicion, d -> d, (a, b) -> a));
            Map<String, PluvioChsDTO> map12h = t5.stream().collect(Collectors.toMap(PluvioChsDTO::codPuntoMedicion, d -> d, (a, b) -> a));
            Map<String, PluvioChsDTO> map24h = t6.stream().collect(Collectors.toMap(PluvioChsDTO::codPuntoMedicion, d -> d, (a, b) -> a));

            Timestamp fechaCaptura = new Timestamp(System.currentTimeMillis());
            List<EstacionesDTO> batchDTO = new ArrayList<>();

            for (PluvioChsDTO base : t1) {
                String cod = base.codPuntoMedicion();

                PrecipitacionesDTO pDTO = new PrecipitacionesDTO();
                pDTO.setPrecipitacion1h (limpiarValor(map1h .getOrDefault(cod, base).valorPrecip()));
                pDTO.setPrecipitacion3h (limpiarValor(map3h .getOrDefault(cod, base).valorPrecip()));
                pDTO.setPrecipitacion6h (limpiarValor(map6h .getOrDefault(cod, base).valorPrecip()));
                pDTO.setPrecipitacion12h(limpiarValor(map12h.getOrDefault(cod, base).valorPrecip()));
                pDTO.setPrecipitacion24h(limpiarValor(map24h.getOrDefault(cod, base).valorPrecip()));

                EstacionesDTO eDTO = new EstacionesDTO();
                eDTO.setIndicativo(cod);
                eDTO.setNombre(base.nombreCortoPM());
                eDTO.setFechaActualizacion(fechaCaptura);
                eDTO.setPrecipitacionesDTO(pDTO);

                batchDTO.add(eDTO);

                if (batchDTO.size() >= 25) {
                    guardarLote(batchDTO);
                    batchDTO.clear();
                }
            }

            if (!batchDTO.isEmpty()) {
                guardarLote(batchDTO);
            }

        } catch (Exception e) {
            System.err.println("Error precipitaciones: " + e.getMessage());
        }
    }

    private void guardarLote(List<EstacionesDTO> dtos) {

        // 1. Obtenemos todos los indicativos que sí existen en la tabla maestra
        // Esto es una consulta rápida que devuelve un Set para búsquedas O(1)
        Set<String> indicativosExistentes = dslContext
                .select(ESTACIONES_METEOROLOGICAS.INDICATIVO)
                .from(ESTACIONES_METEOROLOGICAS)
                .fetchSet(ESTACIONES_METEOROLOGICAS.INDICATIVO);

        // 2. Filtramos los DTOs: solo nos quedamos con los que tienen un indicativo válido
        List<EstacionesDTO> dtosValidos = dtos.stream()
                .filter(dto -> {
                    String original = dto.getIndicativo();
                    // Si termina en P + un solo número, le metemos el cero
                    // Ejemplo: 01A01P1 -> 01A01P01
                    String normalizado = original.replaceAll("P(\\d)$", "P0$1");

                    boolean existe = indicativosExistentes.contains(normalizado);

                    // Seteamos el normalizado para que la FK no falle al insertar
                    if (existe) {
                        dto.setIndicativo(normalizado);
                    }
                    return existe;
                })
                .toList();

        if (dtosValidos.isEmpty()) {
            System.out.println("Saltando lote: Ninguna estación del lote existe en la base de datos.");
            return;
        }

        // 3. Definimos la estructura de la consulta
        var query = dslContext.insertInto(PRECIPITACIONES)
                .columns(
                        PRECIPITACIONES.INDICATIVO,            // Parte de la PK
                        PRECIPITACIONES.FECHA_ACTUALIZACION,   // Parte de la PK
                        PRECIPITACIONES.NOMBRE,
                        PRECIPITACIONES.PRECIPITACION_1H,
                        PRECIPITACIONES.PRECIPITACION_3H,
                        PRECIPITACIONES.PRECIPITACION_6H,
                        PRECIPITACIONES.PRECIPITACION_12H,
                        PRECIPITACIONES.PRECIPITACION_24H
                )
                .values((String) null, null, null, null, null, null, null, null); // Placeholders

        // 4. Ejecutamos el batch mapeando los DTOs a un array de objetos
        dslContext.batch(query)
                .bind(dtosValidos.stream().map(dto -> new Object[] {
                        dto.getIndicativo(),
                        dto.getFechaActualizacion(),
                        dto.getNombre(),
                        dto.getPrecipitacionesDTO().getPrecipitacion1h(),
                        dto.getPrecipitacionesDTO().getPrecipitacion3h(),
                        dto.getPrecipitacionesDTO().getPrecipitacion6h(),
                        dto.getPrecipitacionesDTO().getPrecipitacion12h(),
                        dto.getPrecipitacionesDTO().getPrecipitacion24h()
                }).toArray(Object[][]::new))
                .execute();

        System.out.println("Lote de " + dtosValidos.size() + " registros guardado con éxito en jOOQ.");
    }

}