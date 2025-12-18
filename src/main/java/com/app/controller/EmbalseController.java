package com.app.controller;

import com.app.dto.EmbalseDTO;
import com.app.exceptions.Exceptions;
import com.app.exceptions.FunctionalExceptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.app.service.EmbalseService;
import java.util.List;

@RestController
@RequestMapping("/api/embalses")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class EmbalseController {

    @Autowired
    private EmbalseService embalseService;

    @Value("${cron.job.key}")
    private String cronKey;

    @GetMapping("/top-movimientos")
    public List<EmbalseDTO> getTopMovimientos() throws FunctionalExceptions {
        try {
            // 1. Ejecuta el scraping y guarda los nuevos datos en Neon
            embalseService.obtenerAndActualizarDatosDeLaWeb();

        } catch (Exception e) {
            Exceptions.EMB_E_0001.lanzarExcepcionCausada(e);
        }
        // 2. Recupera el listado final con la variación real (calculada por SQL LAG)
        return embalseService.obtenerListadoParaFront();
    }

    /**
     * Este método lo llamará un Cron-job cada 10 minutos.
     * NO guarda en BD, solo hace una query rápida para despertar a Render y Neon.
     */
    @GetMapping("/keep-alive")
    public ResponseEntity<String> keepAlive() {
        try {
            embalseService.checkDatabaseNeonConnection();
            return ResponseEntity.ok("Servidor despierto y Neon conectado");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al despertar");
        }
    }

    /**
     * Este método lo llamará un Cron-job cada 3 horas.
     * SÍ hace scraping y guarda en la base de datos.
     */
    @PostMapping("/internal-refresh")
    public ResponseEntity<String> triggerUpdate(@RequestHeader(value = "X-Cron-Key", required = false) String key) throws FunctionalExceptions {
        if (key == null || !key.equals(cronKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acceso denegado");
        }

        embalseService.obtenerAndActualizarDatosDeLaWeb();
        return ResponseEntity.ok("Datos actualizados en Neon");
    }

}
