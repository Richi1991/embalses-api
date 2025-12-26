package com.app.modules.weather.controller;

import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @Value("${API_KEY_AEMET}")
    private String apiKeyAemet;

    @GetMapping("/insertar-estaciones-aemet/{provincia}")
    public void insertarEstacionesAemetPorProvincia(@PathVariable(value = "provincia") String provincia) throws FunctionalExceptions {
        try {
            weatherService.insertarEstacionesAemetPorProvincia(provincia, apiKeyAemet);
        } catch(Exception e) {
            Exceptions.EMB_E_0007.lanzarExcepcionCausada(e);
        }
    }

    @GetMapping("/insertar-estaciones-chs")
    public void insertarEstacionesChsPorProvincia() throws FunctionalExceptions {
        try {
            weatherService.insertarEstacionesChs();
        } catch(Exception e) {
            Exceptions.EMB_E_0007.lanzarExcepcionCausada(e);
        }
    }

    @PostMapping("/guardar-datos-estacion/{fechaInicio}/{fechaFin}/{provincia}")
    public void guardarDatosEstacionByFecha(@PathVariable(value= "fechaInicio") OffsetDateTime fechaInicio, @PathVariable(value = "fechaFin") OffsetDateTime fechaFin, @PathVariable (value ="indicativo") String provincia) {

    }

    //@GetMapping("/get-datos-estaciones/{provincia}")
}
