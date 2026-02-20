package com.app.modules.hidrology.scheduler;

import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.service.EmbalseService;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmbalseScheduler {

    @Autowired
    private EmbalseService embalseService;


    @Autowired
    private DSLContext dsl;

    // Se ejecuta cada hora (3600000 ms)
    @Scheduled(cron = "0 0 * * * *")
    public void getAndSaveLecturasEmbalses() {
        try {
            System.out.println("Iniciando scraping automático...");
            embalseService.getEmbalsesDataAndInsertInLecturasEmbalses();
        } catch (Exception e) {
            System.err.println("Error al obtener o insertar en la tabla lecturas embalses: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 240000)
    public void keepNeonDespierto() {
        try {
            // Una consulta que no pesa nada pero cuenta como actividad
            dsl.selectOne().execute();
            // Solo para que lo veas en los logs de Render al principio
            System.out.println(">>> Keep-Alive: Neon sigue despierto.");
        } catch (Exception e) {
            // Si falla, probablemente es que Neon ya se estaba durmiendo,
            // la siguiente ejecución lo despertará.
        }
    }

    @Scheduled(cron = "0 30 0 * * *")
    public void getAndSaveHistoricoCuencaSeguraHorario() {
        try {
            System.out.println("Iniciando scraping automático...");
            embalseService.getAndInsertHistoricoCuencaSeguraHorario();
        } catch (Exception e) {
            System.err.println("Error al obtener o insertar en el historico cuenca segura horario: " + e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 1 * * *") // A la 1:00 AM cada día
    public void getAndInsertHistoricoCuencaSegura() {
        try {
            embalseService.getAndInsertHistoricoCuencaSegura();
        } catch (Exception e) {
            System.err.println("Error al obtener o insertar en el historico cuenca segura: " + e.getMessage());

        }
    }
}
