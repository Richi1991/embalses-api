package com.app.modules.hidrology.controller;

import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.service.PrecipitacionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/embalses/precipitaciones")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class PrecipitacionesController {

    @Autowired
    private PrecipitacionesService precipitacionesService;


    @GetMapping("/extraer_precipitaciones_real_time")
    public void extraerPrecipitacionesRealTime() throws FunctionalExceptions {
        precipitacionesService.extraerPrecipitacionesRealTime();
    }

    @GetMapping("/obtener-coordenadas")
    public void obtenerCoordenadas(){

    }
}
