package com.app.modules.weather.scheduler;

import com.app.modules.weather.service.PrecipitacionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PrecipitacionesScheduler {

    @Autowired
    private PrecipitacionesService precipitacionesService;

    // Se ejecuta cada hora (3600000 ms)
    @Scheduled(cron = "0 0 * * * *")
    public void getAndSavePrecipitacionesRealTime() {
        try {
            System.out.println("Iniciando obtención de precipitaciones y guardado en tabla precipitaciones");
            precipitacionesService.getAndSavePrecipitacionesRealTime();
        } catch (Exception e) {
            System.err.println("Error al obtener o insertar en la tabla precipitaciones: " + e.getMessage());
        }
    }

}
