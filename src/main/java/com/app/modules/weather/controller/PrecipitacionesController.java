package com.app.modules.weather.controller;

import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
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

    @GetMapping("/insert_precipitaciones_last_value")
    public ResponseEntity<String> extraerAndGuardarPrecipitacionesRealTime() {
        new Thread(() -> precipitacionesService.getAndSavePrecipitacionesRealTime()).start();

        return ResponseEntity.ok("Extracción iniciada en background");
    }

    @GetMapping("/get_precipitaciones_last_value")
    public List<EstacionesDTO> getPrecipitacionesLastValue() throws FunctionalExceptions {

        List<EstacionesDTO> estacionesDTOWithPrecipitacionesList = new ArrayList<>();

        try {
            estacionesDTOWithPrecipitacionesList = precipitacionesService.extraerPrecipitacionesRealTime();
        } catch (Exception e) {
            Exceptions.EMB_E_0006.lanzarExcepcionCausada(e);
        }
        return estacionesDTOWithPrecipitacionesList;
    }

}
