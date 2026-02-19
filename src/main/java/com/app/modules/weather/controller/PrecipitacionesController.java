package com.app.modules.weather.controller;

import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.weather.service.PrecipitacionesService;
import com.app.modules.weather.dto.EstacionesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/weather/precipitaciones")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class PrecipitacionesController {

    @Autowired
    private PrecipitacionesService precipitacionesService;

    @Value("${CRON_JOB_KEY}")
    private String cronKey;

    @Value("${API_KEY_AEMET}")
    private String apiKeyAemet;

    @GetMapping("/insert_precipitaciones_last_value")
    public ResponseEntity<String> extraerAndGuardarPrecipitacionesRealTime() {
        new Thread(() -> precipitacionesService.getAndSavePrecipitacionesRealTime()).start();

        return ResponseEntity.ok("Extracción iniciada en background");
    }

    @GetMapping("/get_precipitaciones_last_value")
    public List<EstacionesDTO> getPrecipitacionesLastValue() throws FunctionalExceptions {

        List<EstacionesDTO> estacionesDTOWithPrecipitacionesList = new ArrayList<>();

        try {
            estacionesDTOWithPrecipitacionesList = precipitacionesService.obtenerMapaRapido();
        } catch (Exception e) {
            Exceptions.EMB_E_0006.lanzarExcepcionCausada(e);
        }
        return estacionesDTOWithPrecipitacionesList;
    }

    @GetMapping("/get_values_of_today_aemet/{provincia}")
    public void getValuesOfTodayAemetByProvincia(@PathVariable (value= "provincia") String provincia) throws FunctionalExceptions {
        precipitacionesService.getValuesOfTodayAemetByProvincia(provincia, apiKeyAemet);
    }

}
