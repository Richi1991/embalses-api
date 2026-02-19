package com.app.modules.hidrology.scheduler;

import com.app.modules.hidrology.service.EmbalseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmbalseScheduler {

    @Autowired
    private EmbalseService embalseService;

    // Se ejecuta cada hora (3600000 ms)
    @Scheduled(fixedRate = 3600000)
    public void refrescarDatosEmbalses() {
        try {
            System.out.println("Iniciando scraping automático...");
            embalseService.obtenerAndActualizarDatosDeLaWeb();
        } catch (Exception e) {
            // Loguea el error pero no rompes el flujo de la app
            System.err.println("Error en el refresco horario: " + e.getMessage());
        }
    }
}
