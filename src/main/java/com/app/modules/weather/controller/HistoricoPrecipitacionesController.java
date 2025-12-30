package com.app.modules.weather.controller;

import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.weather.service.HistoricoPrecipitacionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/weather/historicoprecipitaciones")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HistoricoPrecipitacionesController {

    @Autowired
    private HistoricoPrecipitacionesService historicoPrecipitacionesService;

    @Value("${API_KEY_AEMET}")
    private String apiKeyAemet;

    @GetMapping("/insert_historico_precipitaciones_aemet/{provincia}/{fechaInicio}/{fechaFin}")
    public void insertHistoricoPrecipitacionesAemet(@PathVariable(value = "provincia") String provincia, @PathVariable(value = "fechaInicio") String fechaInicio, @PathVariable(value = "fechaFin") String fechaFin) throws IOException, FunctionalExceptions, SQLException {
        historicoPrecipitacionesService.insertarHistoricoPrecipitacionesAemet(provincia, apiKeyAemet, fechaInicio, fechaFin);
    }
}
