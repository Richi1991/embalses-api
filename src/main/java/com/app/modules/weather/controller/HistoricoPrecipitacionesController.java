package com.app.modules.weather.controller;

import com.app.core.exceptions.FunctionalExceptions;
import com.app.core.model.HistoricoPrecipitaciones;
import com.app.core.repository.AcumuladoEstacion;
import com.app.modules.hidrology.dto.PrecipitacionMapaDTO;
import com.app.modules.weather.service.HistoricoPrecipitacionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/weather/historicoprecipitaciones")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HistoricoPrecipitacionesController {

    @Autowired
    private HistoricoPrecipitacionesService historicoPrecipitacionesService;

    @Value("${API_KEY_AEMET}")
    private String apiKeyAemet;

    @PostMapping("/insert_historico_precipitaciones_aemet/{provincia}/{fechaInicio}/{fechaFin}")
    public void insertHistoricoPrecipitacionesAemet(@PathVariable(value = "provincia") String provincia, @PathVariable(value = "fechaInicio") String fechaInicio, @PathVariable(value = "fechaFin") String fechaFin) throws FunctionalExceptions {
        historicoPrecipitacionesService.insertarHistoricoPrecipitacionesAemet(provincia, apiKeyAemet, fechaInicio, fechaFin);
    }

    @PostMapping("/insert_historico_precipitaciones_aemet_job/{provincia}/{days}")
    public ResponseEntity<String> insertHistoricoPrecipitacionesAemetJob(@PathVariable(value = "provincia") String provincia, @PathVariable(value = "days") int days) throws FunctionalExceptions {

        OffsetDateTime OffsetDateTimefechaFin = OffsetDateTime.now();
        OffsetDateTime OffsetDateTimefechaInicio = OffsetDateTime.now().minusDays(days);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'UTC'");

        String fechaFin = OffsetDateTimefechaFin.format(formatter);
        String fechaInicio = OffsetDateTimefechaInicio.format(formatter);

        new Thread(() -> {
            try {
                historicoPrecipitacionesService.insertarHistoricoPrecipitacionesAemet(provincia, apiKeyAemet, fechaInicio, fechaFin);
            } catch (FunctionalExceptions e) {
                throw new RuntimeException(e);
            }
        }).start();
        return ResponseEntity.ok("Respuesta correcta");
    }

    /**
     * fechaInicio ej. 20251201
     * fechaFin ej. 20251225
     */
    @GetMapping("/insertar_historico_precipitaciones_chs/{fechaInicio}/{fechaFin}")
    public void insertHistoricoPrecipitacionesChs(@PathVariable(value= "fechaInicio") String fechaInicio,
                                                  @PathVariable(value= "fechaFin") String fechaFin) throws FunctionalExceptions {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        LocalDate localDateFechaInicio = historicoPrecipitacionesService.parseStringToLocalDate(fechaInicio, formatter);

        LocalDate localDateFechaFin = historicoPrecipitacionesService.parseStringToLocalDate(fechaFin, formatter);

        historicoPrecipitacionesService.insertarHistoricoPrecipitacionesChs(localDateFechaInicio, localDateFechaFin);
    }

    /**
     * fechaInicio ej. 20251201
     * fechaFin ej. 20251225
     */
    @GetMapping("/insertar_historico_precipitaciones_chs_job/{days}")
    public ResponseEntity<String> insertHistoricoPrecipitacionesChs(@PathVariable(value= "days") int days) throws RuntimeException {

        LocalDate localDateFechaFin = LocalDate.now();

        LocalDate localDateFechaInicio = localDateFechaFin.minusDays(days);

        new Thread(() -> {
            try {
                historicoPrecipitacionesService.insertarHistoricoPrecipitacionesChs(localDateFechaInicio, localDateFechaFin);
            } catch (FunctionalExceptions e) {
                throw new RuntimeException(e);
            }
        }).start();

        return ResponseEntity.ok("Insercción historico precipitaciones chs iniciada en background");
    }

    @GetMapping("/obtener_valores_historico_precipitaciones/{fechaInicio}/{fechaFin}")
    public List<HistoricoPrecipitaciones> obtenerValoresHistoricoPrecipitaciones(@PathVariable(value ="fechaInicio") String fechaInicio, @PathVariable(value="fechaFin") String fechaFin) {
        return historicoPrecipitacionesService.obtenerValoresHistoricoPrecipitaciones(fechaInicio, fechaFin);
    }

    @GetMapping("/obtener_valores_precipitaciones_acumulados/{rango}")
    public List<AcumuladoEstacion> obtenerValoresPrecipitacionesAcumulados(@PathVariable(value ="rango") String rango) {
        return historicoPrecipitacionesService.obtenerValoresPrecipitacionesAcumulados(rango);
    }

    @GetMapping("/obtener_datos_mapa_precipitaciones/{rango}")
    public ResponseEntity<List<PrecipitacionMapaDTO>> obtenerDatosMapaPrecipitaciones(@PathVariable(value ="rango") String rango) {
        return ResponseEntity.ok(historicoPrecipitacionesService.getDatosMapa(rango));
    }
}
