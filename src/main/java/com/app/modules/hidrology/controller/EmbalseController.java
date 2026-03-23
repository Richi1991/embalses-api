package com.app.modules.hidrology.controller;

import com.app.modules.hidrology.dto.EmbalseDTO;
import com.app.modules.hidrology.dto.HistoricoCuencaDTO;
import com.app.core.exceptions.FunctionalExceptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.app.modules.hidrology.service.EmbalseService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/embalses")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class EmbalseController {

    @Autowired
    private EmbalseService embalseService;

    /**
     * se hace un get desde cronJob cada 10 min para despertar al back de render
     * @param intervalo
     * @return
     * @throws FunctionalExceptions
     */
    @GetMapping("/top-movimientos-cronjob")
    public ResponseEntity<AtomicReference<List<EmbalseDTO>>> getTopMovimientosCronJob(@RequestParam(value = "intervalo", defaultValue = "1 day") String intervalo) throws FunctionalExceptions {

        AtomicReference<List<EmbalseDTO>> embalseDTOList = new AtomicReference<>(new ArrayList<>());

        new Thread(() -> {
            try {
                embalseDTOList.set(embalseService.obtenerUltimasLecturasConVariacionPorIntervalo(intervalo));
            } catch (FunctionalExceptions e) {
                throw new RuntimeException(e);
            }
        }).start();

        return ResponseEntity.ok(embalseDTOList);
    }

    /**
     * Primero obtiene los ultimos datos de la web de la chs y los guarda en la tabla lecturas_embalses
     * Después obtiene las Ultimas Lecturas Con Variacion Por Intervalo de fechas de la tabla lecturas_embalses
     *
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
     *
     * @return
     * @throws FunctionalExceptions
     */
    @GetMapping("/historico-cuenca")
    public List<HistoricoCuencaDTO> getHistoricoCuencaSegura() throws FunctionalExceptions {
        return embalseService.getHistoricoCuencaSegura();
    }


    /**
     * Obtiene los datos de historico de cuenca del Segura diarios
     * Los muestra en la gráfica principal
     *
     * @return
     * @throws FunctionalExceptions
     */
    @GetMapping("/historico-cuenca-diario")
    public List<HistoricoCuencaDTO> getHistoricoCuencaSeguraDiaros() throws FunctionalExceptions {
        return embalseService.getHistoricoCuencaSeguraUltimoDia();
    }

    /**
     * Cada vez que abrimos el detalle de un embalse
     * obtiene los datos de la tabla lecturas_embalses
     * y muestra el grafico de la pantalla de cada embalse
     */
    @GetMapping("/obtener_historico_embalse{idEmbalse}")
    public List<EmbalseDTO> obtenerHistoricoEmbalsePorIdEmbalse(@PathVariable("idEmbalse") int idEmbalse) throws FunctionalExceptions {
        return embalseService.obtenerHistoricoEmbalsePorIdEmbalse(idEmbalse);
    }

    /**
     * Obtiene el último valor de volumen de los embalses de la cuenca del segura y sus coordendas
     * @return
     */
    @GetMapping("/get_embalses_last_value_and_position")
    public List<EmbalseDTO> getEmbalsesLastValueAndPosition() throws FunctionalExceptions {
        return embalseService.getEmbalsesLastValueAndPosition();
    }

    @GetMapping("/get_embalses_chj")
    public void getEmbalsesChj() throws IOException {
        embalseService.getEmbalsesChj();
    }

}
