package com.app.modules.hidrology.controller;

import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.service.PrecipitacionesService;
import com.app.modules.weather.dto.EstacionesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/embalses/precipitaciones")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class PrecipitacionesController {

    @Autowired
    private PrecipitacionesService precipitacionesService;


    @GetMapping("/insertar_estaciones")
    public void insertarEstaciones() throws FunctionalExceptions {
        precipitacionesService.insertarEstaciones();
    }

    @GetMapping("/insert_precipitaciones_last_value")
    public void extraerAndGuardarPrecipitacionesRealTime() throws FunctionalExceptions {
        precipitacionesService.getAndSavePrecipitacionesRealTime();
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
