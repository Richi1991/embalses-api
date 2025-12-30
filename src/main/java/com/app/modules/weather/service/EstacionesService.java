package com.app.modules.weather.service;

import com.app.core.constantes.Constants;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.core.model.HistoricoPrecipitaciones;
import com.app.core.repository.HistoricoPrecipitacionesRepository;
import com.app.modules.weather.dao.EstacionesDAO;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import com.app.modules.weather.dto.TemperaturasDTO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class EstacionesService {

    @Autowired
    private EstacionesDAO estacionesDAO;

    @Autowired
    private HistoricoPrecipitacionesRepository historicoPrecipitacionesRepository;

    public void insertarEstacionesAemetPorProvincia(String provincia, String apiKeyAemet) throws FunctionalExceptions {
        List<EstacionesDTO> estacionesAemetDTOListFilterByProvincia = obtenerEstacionesAemetPorProvincia(provincia, apiKeyAemet);
        estacionesDAO.insertarEstacionesAemetFilterByProvincia(estacionesAemetDTOListFilterByProvincia);
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

    public void insertarHistoricoPrecipitacionesAemet(String provincia, String apiKeyAemet, String fechaInicio, String fechaFin) throws FunctionalExceptions, SQLException {

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
                                        EstacionesDTO estacionesDTOToInsert = new EstacionesDTO();
                                        PrecipitacionesDTO precipitaciones = new PrecipitacionesDTO();
                                        TemperaturasDTO temperaturas = new TemperaturasDTO();

                                        estacionesDTOToInsert.setIndicativo(nodo.get("indicativo").asText());

                                        if (nodo.get("fecha") != null) {
                                            String fecha = nodo.get("fecha").asText();
                                            LocalDate localDate = LocalDate.parse(fecha);
                                            Timestamp fechaDato = Timestamp.valueOf(localDate.atStartOfDay());
                                            estacionesDTOToInsert.setFechaActualizacion(fechaDato);
                                        }
                                        if (nodo.get("prec") != null) {
                                            String precipitacionDiaria = nodo.get("prec").asText();
                                            if (precipitacionDiaria != null && precipitacionDiaria == "^-?\\d+([.,]\\d+)?$") {
                                                String precipitacionDiariaDouble = precipitacionDiaria.replace(Constants.COMA, Constants.PUNTO);
                                                precipitaciones.setPrecipitacion24h(Double.parseDouble(precipitacionDiariaDouble));
                                            } else {
                                                precipitaciones.setPrecipitacion24h(0.0);
                                            }
                                        }
                                        if (nodo.get("tmed") != null) {
                                            String tmed = nodo.get("tmed").asText();
                                            if (tmed != null) {
                                                String tmedFormated = tmed.replace(Constants.COMA, Constants.PUNTO);
                                                temperaturas.setTmed(Double.parseDouble(tmedFormated));
                                            }
                                        }
                                        if (nodo.get("tmin") != null) {
                                            String tmin = nodo.get("tmin").asText();
                                            if (tmin != null) {
                                                String tminFormated = tmin.replace(Constants.COMA, Constants.PUNTO);
                                                temperaturas.setTmin(Double.parseDouble(tminFormated));
                                            }
                                        }
                                        if (nodo.get("tmax") != null) {
                                            String tmax = nodo.get("tmax").asText();
                                            if (tmax != null) {
                                                String tmaxFormated = tmax.replace(Constants.COMA, Constants.PUNTO);
                                                temperaturas.setTmax(Double.parseDouble(tmaxFormated));
                                            }
                                        }
                                        estacionesDTOToInsert.setPrecipitacionesDTO(precipitaciones);
                                        estacionesDTOToInsert.setTemperaturasDTO(temperaturas);
                                        estacionesDTOListToInsert.add(estacionesDTOToInsert);
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

        List<HistoricoPrecipitaciones> entities = estacionesDTOListToInsert.stream()
                .map(dto -> {
                    HistoricoPrecipitaciones entidad = new HistoricoPrecipitaciones();
                    entidad.setValor24h(dto.getPrecipitacionesDTO().getPrecipitacion24h());
                    entidad.setFechaRegistro(Instant.from(dto.getFechaActualizacion().toLocalDateTime()));
                    entidad.setTmax(dto.getTemperaturasDTO().getTmax());
                    entidad.setTmin(dto.getTemperaturasDTO().getTmin());
                    entidad.setTmed(dto.getTemperaturasDTO().getTmed());
                    return entidad;
                }).toList();

        historicoPrecipitacionesRepository.saveAll(entities);
    }

    public List<EstacionesDTO> obtenerEstaciones() throws FunctionalExceptions {
        return estacionesDAO.obtenerEstacionesMeteorologicas();
    }

}
