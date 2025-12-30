package com.app.modules.weather.controller;

import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.service.EstacionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    @GetMapping("/insert_historico_precipitaciones_aemet/{provincia}/{fechaInicio}/{fechaFin}")
    public void insertHistoricoPrecipitacionesAemet(@PathVariable(value = "provincia") String provincia, @PathVariable(value = "fechaInicio") String fechaInicio, @PathVariable(value = "fechaFin") String fechaFin) throws IOException, FunctionalExceptions, SQLException {
        weatherService.insertarHistoricoPrecipitacionesAemet(provincia, apiKeyAemet, fechaInicio, fechaFin);
    }

    @GetMapping("/obtener_estaciones")
    public ResponseEntity<List<EstacionesDTO>> obtenerEstaciones() throws FunctionalExceptions {
        List<EstacionesDTO> estacionesDTOList = new ArrayList<>();
        try {
            estacionesDTOList = weatherService.obtenerEstaciones();
        } catch(Exception e) {
            Exceptions.EMB_E_0008.lanzarExcepcionCausada(e);
        }
        return ResponseEntity.ok(estacionesDTOList);
    }

}
