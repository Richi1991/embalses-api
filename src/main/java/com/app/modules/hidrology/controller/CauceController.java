package com.app.modules.hidrology.controller;

import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.dto.CauceDTO;
import com.app.modules.hidrology.service.CauceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/caudales")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CauceController {

    @Autowired
    private CauceService caudalService;

    @GetMapping("/get_and_insert_cauces_chs")
    public void getAndInsertCaucesChs() throws FunctionalExceptions {
        caudalService.getAndInsertCaucesChs();
    }

}
