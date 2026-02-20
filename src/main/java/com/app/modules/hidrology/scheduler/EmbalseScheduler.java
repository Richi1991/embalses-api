package com.app.modules.hidrology.scheduler;

import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.service.EmbalseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmbalseScheduler {

    @Autowired
    private EmbalseService embalseService;

    // Se ejecuta cada hora (3600000 ms)
    @Scheduled(fixedRate = 3600000)
    public void getAndSaveLecturasEmbalses() {
        try {
            System.out.println("Iniciando scraping automático...");
            embalseService.getEmbalsesDataAndInsertInLecturasEmbalses();
        } catch (Exception e) {
            System.err.println("Error en el refresco horario: " + e.getMessage());
        }
    }

    // Se ejecuta cada dia (3600000 ms)
    @Scheduled(fixedRate = 3600000)
    public void getAndSaveHistoricoCuencaSeguraHorario() {
        try {
            System.out.println("Iniciando scraping automático...");
            embalseService.getAndInsertHistoricoCuencaSeguraHorario();
        } catch (Exception e) {
            System.err.println("Error en el refresco horario: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 86400000)
    public ResponseEntity<String> getAndInsertHistoricoCuencaSegura() throws FunctionalExceptions {
        embalseService.getAndInsertHistoricoCuencaSegura();
        return ResponseEntity.ok("Datos actualizados en Neon");
    }
}
