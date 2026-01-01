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

import java.io.IOException;
import java.sql.SQLException;
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
    public void insertHistoricoPrecipitacionesAemet(@PathVariable(value = "provincia") String provincia, @PathVariable(value = "fechaInicio") String fechaInicio, @PathVariable(value = "fechaFin") String fechaFin) throws IOException, FunctionalExceptions, SQLException {
        historicoPrecipitacionesService.insertarHistoricoPrecipitacionesAemet(provincia, apiKeyAemet, fechaInicio, fechaFin);
    }

    /**
     * fechaInicio ej. 20251201
     * fechaFin ej. 20251225
     */
    @GetMapping("/insertar_historico_precipitaciones_chs/{fechaInicio}/{fechaFin}")
    public void insertHistoricoPrecipitacionesChs(@PathVariable(value= "fechaInicio") String fechaInicio,
                                                  @PathVariable(value= "fechaFin") String fechaFin) throws FunctionalExceptions {
        historicoPrecipitacionesService.insertarHistoricoPrecipitacionesChs(fechaInicio, fechaFin);
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
