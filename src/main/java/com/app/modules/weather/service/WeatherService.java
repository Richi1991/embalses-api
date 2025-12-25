package com.app.modules.weather.service;

import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.weather.dao.WeatherDAO;
import com.app.modules.weather.dto.EstacionesAemetDTO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

@Service
public class WeatherService {

    @Autowired
    private WeatherDAO weatherDAO;

    public void insertarEstacionesAemetPorProvincia(String provincia, String apiKeyAemet) throws IOException, FunctionalExceptions {
        OkHttpClient client = new OkHttpClient();

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
                                List<EstacionesAemetDTO> estacionesAemetDTOListFilterByProvincia = mapper.readValue(jsonDataEstaciones, new TypeReference<List<EstacionesAemetDTO>>() {
                                        })
                                        .stream()
                                        .filter(e -> e.getProvincia().equalsIgnoreCase(provincia))
                                        .toList();

                                weatherDAO.insertarEstacionesAemetFilterByProvincia(provincia, estacionesAemetDTOListFilterByProvincia);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
        }

    }

}
