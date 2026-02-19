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

    public List<EstacionesDTO> obtenerEstacionesAemetPorProvincia(String provincia, String apiKeyAemet) throws FunctionalExceptions {
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
}
