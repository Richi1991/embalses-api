package com.app.service;

import com.app.dto.EmbalseDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PDFExtractorService {


    public List<EmbalseDTO> extraerDatosDesdePdf(String urlPdf, LocalDate date) {

        List<EmbalseDTO> lecturas = new ArrayList<>();

        try (BufferedInputStream in = new BufferedInputStream(new URL(urlPdf).openStream());
             PDDocument document = PDDocument.load(in)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String texto = stripper.getText(document);
            String[] lineas = texto.split("\\n");

            for (String linea : lineas) {
                // Buscamos si la línea comienza con el nombre de un embalse que nos interese
                // Ejemplo de línea en PDF: "CENAJO 437,0 80,9 18,5 ..."
                Integer embalseId = obtenerIdPorNombre(linea);

                if (embalseId != null) {
                    EmbalseDTO lectura = mapearLineaALectura(linea, embalseId, date);
                    if (lectura != null) {
                        lecturas.add(lectura);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al procesar el PDF del día " + date + ": " + e.getMessage());
        }
        return lecturas;
    }

    private Integer obtenerIdPorNombre(String linea) {
        // Definición de los nombres tal cual aparecen en la primera columna del PDF
        if (linea.startsWith("FUENSANTA")) return 1;
        if (linea.startsWith("CENAJO")) return 2;
        if (linea.startsWith("TALAVE")) return 3;
        if (linea.startsWith("CAMARILLAS")) return 4;
        if (linea.startsWith("BOQUERÓN")) return 5;
        if (linea.startsWith("CHARCOS")) return 6;
        if (linea.startsWith("BAYCO")) return 7;
        if (linea.startsWith("LA RISCA")) return 8;
        if (linea.startsWith("MORATALLA")) return 9;
        if (linea.startsWith("ARGOS")) return 10;
        if (linea.startsWith("ALFONSO XIII")) return 11;
        if (linea.startsWith("JUDÍO")) return 12;
        if (linea.startsWith("MORO")) return 13;
        if (linea.startsWith("CÁRCABO")) return 14;
        if (linea.startsWith("LA CIERVA")) return 15;
        if (linea.startsWith("PLIEGO")) return 16;
        if (linea.startsWith("DOÑA ANA")) return 17;
        if (linea.startsWith("LOS RODEOS")) return 18;
        if (linea.startsWith("MAYÉS")) return 19;
        if (linea.startsWith("SANTOMERA")) return 20;
        if (linea.startsWith("VALDEINFIERNO")) return 21;
        if (linea.startsWith("PUENTES")) return 22;
        if (linea.startsWith("ALGECIRAS")) return 23;
        if (linea.startsWith("JOSÉ BAUTISTA")) return 24;
        if (linea.startsWith("LA PEDRERA")) return 25;
        if (linea.startsWith("CREVILLENTE")) return 26;
        return null;
    }

    private EmbalseDTO mapearLineaALectura(String linea, int id, LocalDate fecha) {
        try {
            // Dividimos la línea por espacios en blanco
            String[] partes = linea.trim().split("\\s+");

            // Normalmente en el PDF de la CHS tras el nombre viene:
            // Capacidad, Volumen Actual (hm3), Porcentaje...
            // Usamos índices calculados según la estructura del PDF oficial
            String nombre = partes[1];
            double hm3 = limpiarNumero(partes[3]);
            double porcentaje = limpiarNumero(partes[4]);
            double variacion = limpiarNumero(partes[5]);

            return new EmbalseDTO(id, nombre, hm3, porcentaje, variacion, null, Timestamp.valueOf(fecha.atStartOfDay()));

        } catch (Exception e) {
            return null; // Si la línea no tiene el formato esperado, la saltamos
        }
    }

    private double limpiarNumero(String numeroStr) {
        return Double.parseDouble(
                numeroStr.replace(".", "")  // 1. Quita el punto de los miles (1.140,6 -> 1140,6)
                        .replace(",", ".") // 2. Cambia la coma por un punto (1140,6 -> 1140.6)
        );
    }

}
