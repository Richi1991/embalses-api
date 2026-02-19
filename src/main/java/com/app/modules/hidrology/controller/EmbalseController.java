package com.app.modules.hidrology.controller;

import com.app.modules.hidrology.dto.EmbalseDTO;
import com.app.modules.hidrology.dto.HistoricoCuencaDTO;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.app.modules.hidrology.service.EmbalseService;

import java.util.List;

@RestController
@RequestMapping("/api/embalses")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class EmbalseController {

    @Autowired
    private EmbalseService embalseService;

    @Value("${CRON_JOB_KEY}")
    private String cronKey;

    /**
     * Primero obtiene los ultimos datos de la web de la chs y los guarda en la tabla lecturas_embalses
     * Después obtiene las Ultimas Lecturas Con Variacion Por Intervalo de fechas de la tabla lecturas_embalses
     * @param intervalo
     * @return
     * @throws FunctionalExceptions
     */
    @GetMapping("/top-movimientos")
    public List<EmbalseDTO> getTopMovimientos(@RequestParam(value = "intervalo", defaultValue = "1 day") String intervalo) throws FunctionalExceptions {
        return embalseService.obtenerUltimasLecturasConVariacionPorIntervalo(intervalo);
    }

    /**
     * Obtiene los datos de historico de cuenca del Segura
     * Los muestra en la gráfica principal
     * @return
     * @throws FunctionalExceptions
     */
    @GetMapping("/historico-cuenca")
    public ResponseEntity<List<HistoricoCuencaDTO>> getHistoricoCuencaSegura() throws FunctionalExceptions {
        List<HistoricoCuencaDTO> datos = embalseService.getHistoricoCuencaSegura();
        return ResponseEntity.ok(datos);
    }


    /**
     * Obtiene los datos de historico de cuenca del Segura diarios
     * Los muestra en la gráfica principal
     * @return
     * @throws FunctionalExceptions
     */
    @GetMapping("/historico-cuenca-diario")
    public ResponseEntity<List<HistoricoCuencaDTO>> getHistoricoCuencaSeguraDiaros() throws FunctionalExceptions {
        List<HistoricoCuencaDTO> datos = embalseService.getHistoricoCuencaSeguraUltimoDia();

        return ResponseEntity.ok(datos);
    }

    /**
     * Este método lo llamará un Cron-job 1 vez cada hora
     * guarda los datos en la tabla historico_cuenca_segura_diario
     * Se usa en el grafico principal del front
     * SÍ hace scraping y guarda en la base de datos.
     */
    @PostMapping("/insertar-cuenca-segura-diario")
    public ResponseEntity<String> getDataWebAndUpdateEveryHour(@RequestHeader(value = "X-Cron-Key", required = false) String key) throws FunctionalExceptions {
        if (key == null || !key.equals(cronKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acceso denegado");
        }
        embalseService.getHistoricoCuencaSeguraUltimoDia();
        return ResponseEntity.ok("Datos historico_cuenca_segura_diario insertados en Neon");
    }

    /**
     * Este método lo llamará un Cron-job 1 vez al día y
     * guarda los datos en la tabla historico_cuenca_segura
     * Se usa en el grafico principal del front
     * SÍ hace scraping y guarda en la base de datos.
     */
    @PostMapping("/internal-refresh")
    public ResponseEntity<String> getDataWebAndUpdateEveryDay(@RequestHeader(value = "X-Cron-Key", required = false) String key) throws FunctionalExceptions {
        if (key == null || !key.equals(cronKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Acceso denegado");
        }

        embalseService.obtenerDatosWebAndUpdateEveryDay();
        return ResponseEntity.ok("Datos actualizados en Neon");
    }

    /**
     * Cada vez que abrimos el detalle de un embalse
     * obtiene los datos de la tabla lecturas_embalses
     * y muestra el grafico de la pantalla de cada embalse
     */
    @GetMapping("/obtener_historico_embalse{idEmbalse}")
    public ResponseEntity<List<EmbalseDTO>> obtenerHistoricoEmbalsePorIdEmbalse(@PathVariable("idEmbalse") int idEmbalse) throws FunctionalExceptions {
        List<EmbalseDTO> historicoEmbalseList = embalseService.obtenerHistoricoEmbalsePorIdEmbalse(idEmbalse);
        return ResponseEntity.ok(historicoEmbalseList);
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

}
