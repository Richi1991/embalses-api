package com.app.modules.hidrology.scheduler;

import com.app.modules.hidrology.service.CaudalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CaudalScheduler {

    @Autowired
    private CaudalService caudalService;

    // Se ejecuta cada media hora)
    @Scheduled(cron = "0 0/30 * * * *")
    public void insertCaudalesRealTime() {
        try {
            System.out.println("Iniciando scraping automático...");
            caudalService.insertCaudalesRealTime(Boolean.TRUE);
        } catch (Exception e) {
            System.err.println("Error al obtener o insertar en la tabla lecturas cauces horaria: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 30 6 * * *")
    public void insertCaudalesHistorico() {
        try {
            System.out.println("Iniciando scraping automático...");
            caudalService.insertCaudalesRealTime(Boolean.FALSE);
        } catch (Exception e) {
            System.err.println("Error al obtener o insertar en la tabla lecturas cauces diaria: " + e.getMessage());
        }
    }
}
