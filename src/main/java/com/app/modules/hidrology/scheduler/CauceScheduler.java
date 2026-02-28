package com.app.modules.hidrology.scheduler;

import com.app.modules.hidrology.service.CauceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CauceScheduler {

    @Autowired
    private CauceService cauceService;

    // Se ejecuta cada media hora)
    @Scheduled(cron = "0 0/30 * * * *")
    public void insertCaudalesRealTime() {
        try {
            System.out.println("Iniciando scraping automático...");
            cauceService.insertCaudalesRealTime(Boolean.TRUE);
        } catch (Exception e) {
            System.err.println("Error al obtener o insertar en la tabla lecturas caudales: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void insertCaudalesHistorico() {
        try {
            System.out.println("Iniciando scraping automático...");
            cauceService.insertCaudalesRealTime(Boolean.FALSE);
        } catch (Exception e) {
            System.err.println("Error al obtener o insertar en la tabla lecturas caudales: " + e.getMessage());
        }
    }
}
