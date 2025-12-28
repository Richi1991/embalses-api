package com.app.modules.weather.service;

import com.app.core.constantes.Constants;
import com.app.modules.weather.dao.PrecipitacionesDAO;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrecipitacionesService {

    @Autowired
    private PrecipitacionesDAO precipitacionesDAO;

    public void getAndSavePrecipitacionesRealTime() {
        WebDriver driver = createDriver();

        try {
            System.out.println("--- Iniciando captura de Precipitaciones CHSegura ---");
            driver.get("https://www.chsegura.es/es/cuenca/redes-de-control/saih/informe-horario-de-precipitaciones/");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tablaVisorPrecipitaciones")));

            try {
                System.out.println("Enviando comando JS para expandir tabla...");
                org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
                js.executeScript(
                        "var table = $('#tablaVisorPrecipitaciones').DataTable();" +
                                "table.page.len(500).draw();"
                );

                wait.until(d -> d.findElements(By.cssSelector("#tablaVisorPrecipitaciones tbody tr")).size() > 25);

                System.out.println("Tabla expandida exitosamente mediante JS.");
            } catch (Exception e) {
                System.out.println("Error al expandir con JS: " + e.getMessage());
                try {
                    WebElement lengthMenu = driver.findElement(By.name("tablaVisorPrecipitaciones_length"));
                    new Select(lengthMenu).selectByValue("100");
                    Thread.sleep(5000);
                } catch (Exception ex) {
                    System.out.println("No se pudo expandir de ninguna forma.");
                }
            }

            // Localizar todas las filas
            List<WebElement> filas = driver.findElements(By.cssSelector("#tablaVisorPrecipitaciones tbody tr"));

            int batchSize = 20; // Ajusta según memoria
            List<EstacionesDTO> batch = new ArrayList<>();

            for (int i = 0; i < filas.size(); i++) {
                WebElement fila = filas.get(i);
                List<WebElement> celdas = fila.findElements(By.tagName("td"));

                if (celdas.size() >= 8) {
                    String denominacion = celdas.get(0).getText().trim();
                    String punto = celdas.get(1).getText().trim();

                    if (!punto.isEmpty() && !punto.equalsIgnoreCase("No data available in table")) {
                        PrecipitacionesDTO precipitacionesDTO = new PrecipitacionesDTO();

                        EstacionesDTO estacionesDTO = new EstacionesDTO();
                        estacionesDTO.setIndicativo(punto);
                        estacionesDTO.setNombre(denominacion);

                        precipitacionesDTO.setPrecipitacion1h(limpiarValor(celdas.get(3).getText().trim()));
                        precipitacionesDTO.setPrecipitacion3h(limpiarValor(celdas.get(4).getText().trim()));
                        precipitacionesDTO.setPrecipitacion6h(limpiarValor(celdas.get(5).getText().trim()));
                        precipitacionesDTO.setPrecipitacion12h(limpiarValor(celdas.get(6).getText().trim()));
                        precipitacionesDTO.setPrecipitacion24h(limpiarValor(celdas.get(7).getText().trim()));

                        estacionesDTO.setPrecipitacionesDTO(precipitacionesDTO);
                        batch.add(estacionesDTO);
                    }
                }

                // Guardar batch si alcanza el tamaño o es la última fila
                if (batch.size() == batchSize || i == filas.size() - 1) {
                    precipitacionesDAO.guardarValoresPrecipitaciones(batch);
                    batch.clear(); // liberar memoria
                }
            }

            System.out.println("Extracción de precipitaciones completada. Total filas procesadas: " + filas.size());

        } catch (Exception e) {
            System.err.println("Error crítico: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }


    public void insertarEstaciones() {
        try {
            System.out.println("--- Iniciando captura de estaciones CHSegura con Jsoup ---");

            // 1️⃣ Conectarse a la página y parsear HTML
            Document doc = Jsoup.connect("https://www.chsegura.es/es/cuenca/redes-de-control/saih/informe-horario-de-precipitaciones/")
                    .timeout(30000) // 30 segundos timeout
                    .get();

            // 2️⃣ Seleccionar todas las filas de la tabla
            Elements filas = doc.select("#tablaVisorPrecipitaciones tbody tr");

            List<EstacionesDTO> estacionesDTOList = new ArrayList<>();

            for (Element fila : filas) {
                Elements celdas = fila.select("td");

                if (celdas.size() >= 8) {
                    String denominacion = celdas.get(0).text().trim();
                    String punto = celdas.get(1).text().trim();

                    if (!punto.isEmpty() && !punto.equalsIgnoreCase("No data available in table")) {
                        EstacionesDTO estacionesDTO = new EstacionesDTO();
                        estacionesDTO.setNombre(denominacion);
                        estacionesDTO.setIndicativo(punto);
                        estacionesDTO.setRedOrigen(Constants.CHS);
                        estacionesDTOList.add(estacionesDTO);
                    }
                }
            }

            // 3️⃣ Insertar en base de datos
            precipitacionesDAO.insertarEstacionesChs(estacionesDTOList);
            System.out.println("Extracción de estaciones completada. Filas procesadas: " + estacionesDTOList.size());

        } catch (Exception e) {
            System.err.println("Error crítico al insertar estaciones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Función auxiliar para evitar errores de parseo
    private double limpiarValor(String valor) {
        try {
            if (valor == null || valor.equals("-") || valor.isEmpty()) return 0.0;
            return Double.parseDouble(valor.replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public List<EstacionesDTO> extraerPrecipitacionesRealTime() {
        return precipitacionesDAO.getPrecipitacionesRealTime();
    }

    public WebDriver createDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);
        return driver;
    }



}