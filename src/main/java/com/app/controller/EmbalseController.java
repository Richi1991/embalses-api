package com.app.controller;

import com.app.dto.EmbalseDTO;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.app.service.ScrapingService;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/embalses")
@CrossOrigin(origins = "*", allowedHeaders = "*") // El asterisco da permiso total para pruebas
public class EmbalseController {


    private final ScrapingService scrapingService;

    public EmbalseController(ScrapingService scrapingService) {
        this.scrapingService = scrapingService;
    }

    @GetMapping("/top-movimientos")
    public List<EmbalseDTO> getTopMovimientos() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        // Por ahora, el service devolverá la lista parseada de la web que me pasaste
        return scrapingService.obtenerDatosDeLaWeb();
    }
}
