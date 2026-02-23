package com.app.modules.weather.service;

import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.weather.dao.EstacionesDAO;
import com.app.modules.weather.dto.EstacionesDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EstacionesService {

    @Autowired
    private EstacionesDAO estacionesDAO;

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
                                    // Cambiado .asString() por .asText()
                                    if (nodo.get("provincia").asText().equals(provincia)) {
                                        EstacionesDTO estacionesDTO = new EstacionesDTO();

                                        estacionesDTO.setLatitud(nodo.get("latitud").asText());
                                        estacionesDTO.setProvincia(nodo.get("provincia").asText());

                                        // Jackson usa asInt(), asLong() o asDouble(). Para un short, asInt() es lo más seguro.
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
}
