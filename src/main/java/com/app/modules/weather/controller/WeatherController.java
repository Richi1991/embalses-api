package com.app.modules.weather.controller;

import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.weather.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class WeatherController {

    @Autowired
    private WeatherService weatherService;

    @Value("${API_KEY_AEMET}")
    private String apiKeyAemet;

    @GetMapping("/insertar-estaciones/{provincia}")
    public void insertarEstacionesAemetPorProvincia(@PathVariable(value = "provincia") String provincia) throws FunctionalExceptions {
        try {
            weatherService.insertarEstacionesAemetPorProvincia(provincia, apiKeyAemet);
        } catch(Exception e) {
            Exceptions.EMB_E_0007.lanzarExcepcionCausada(e);
        }

    }
}
