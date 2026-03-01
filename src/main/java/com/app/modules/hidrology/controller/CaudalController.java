package com.app.modules.hidrology.controller;

import com.app.core.exceptions.FunctionalExceptions;
import com.app.modules.hidrology.service.CaudalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
