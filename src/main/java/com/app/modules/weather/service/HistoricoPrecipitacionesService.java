package com.app.modules.weather.service;

import com.app.core.constantes.Constants;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.core.model.HistoricoPrecipitaciones;
import com.app.core.repository.HistoricoPrecipitacionesRepository;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import com.app.modules.weather.dto.TemperaturasDTO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HistoricoPrecipitacionesService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HistoricoPrecipitacionesRepository historicoPrecipitacionesRepository;

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
                        newUrl = node.get("datos").asText();
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
                            }catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    }
                }catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }  catch (Exception e) {
                Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
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


        String sql = "INSERT INTO historico_precipitaciones (indicativo, nombre, valor_24h, fecha_registro, tmax, tmin, tmed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (indicativo, fecha_registro) DO NOTHING";

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

        System.out.println("Guardado de datos realizado correctamente en Tabla HistoricoPrecipitaciones");
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
}
