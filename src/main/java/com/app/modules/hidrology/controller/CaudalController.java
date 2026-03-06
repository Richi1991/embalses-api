package com.app.modules.hidrology.controller;

import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.dto.EstadisticaCuencaDTO;
import com.app.modules.hidrology.dto.UltimaLecturaCaudalDTO;
import com.app.modules.hidrology.service.CaudalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caudales")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CaudalController {

    @Autowired
    private CaudalService caudalService;

    @GetMapping("/get_and_insert_cauces_chs")
    public void getAndInsertCaucesChs() throws FunctionalExceptions {
        caudalService.getAndInsertCaucesChs();
    }

    @GetMapping("/get_caudales_horarios_chs/{days}")
    public List<EstadisticaCuencaDTO> getCaudalesHorariosChsFilteredByDay(@PathVariable int days) throws FunctionalExceptions {
        return caudalService.getCaudalesHorariosChsFilteredByDay(days);
    }

    @GetMapping("/get_last_caudales_and_position")
    public List<UltimaLecturaCaudalDTO> getLastCaudalAndPosition() throws FunctionalExceptions {
        return caudalService.getLastCaudalAndPosition();
    }

}
