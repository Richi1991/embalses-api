package com.app.modules.weather.service;

import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.weather.dao.EstacionesDAO;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
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
                                        PrecipitacionesDTO precipitaciones = new PrecipitacionesDTO();

                                        String precipitacionDiaria = nodo.get("prec").asText();
                                        if (precipitacionDiaria != null) {
                                            String precipitacionDiariaDouble = precipitacionDiaria.replace(",", ".");
                                            precipitaciones.setPrecipitacion24h(Double.parseDouble(precipitacionDiariaDouble));
                                        } else {
                                            precipitaciones.setPrecipitacion24h(0.0);
                                        }


                                        estacionesDTO.setPrecipitacionesDTO(precipitaciones);
                                        estacionesDTOListToInsert.add(estacionesDTO);
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

        estacionesDAO.insertarDatosClimatologicosAemetFilterByProvincia(estacionesDTOListToInsert);


    }

    public List<EstacionesDTO> obtenerEstaciones() throws FunctionalExceptions {
        return estacionesDAO.obtenerEstacionesMeteorologicas();
    }

}
