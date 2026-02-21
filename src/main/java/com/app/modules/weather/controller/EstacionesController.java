package com.app.modules.weather.controller;

import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.weather.service.EstacionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather/estaciones")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class EstacionesController {

    @Autowired
    private EstacionesService weatherService;

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
}
