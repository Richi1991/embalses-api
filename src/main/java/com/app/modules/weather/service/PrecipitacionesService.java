package com.app.modules.weather.service;

import com.app.core.constantes.Constants;
import com.app.modules.weather.dao.PrecipitacionesDAO;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import io.github.bonigarcia.wdm.WebDriverManager;
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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class PrecipitacionesService {

    @Autowired
    private PrecipitacionesDAO precipitacionesDAO;

    public void insertarEstaciones() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);

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
                // Fallback: Intentar al menos el clic físico si el JS falla
                try {
                    WebElement lengthMenu = driver.findElement(By.name("tablaVisorPrecipitaciones_length"));
                    new Select(lengthMenu).selectByValue("100");
                    Thread.sleep(5000);
                } catch (Exception ex) {
                    System.out.println("No se pudo expandir de ninguna forma.");
                }
            }

            // 3. Localizar todas las filas renderizadas
            List<WebElement> filas = driver.findElements(By.cssSelector("#tablaVisorPrecipitaciones tbody tr"));

            List<EstacionesDTO> estacionesDTOList = new ArrayList<>();

            for (WebElement fila : filas) {
                List<WebElement> celdas = fila.findElements(By.tagName("td"));

                // Verificamos que la fila tenga al menos 8 columnas para evitar errores
                if (celdas.size() >= 8) {
                    String denominacion = celdas.get(0).getText().trim();
                    String punto = celdas.get(1).getText().trim();

                    if (!punto.isEmpty() && !punto.equalsIgnoreCase("No data available in table")) {

                        EstacionesDTO estacionesDTO = new EstacionesDTO();
                        estacionesDTO.setNombre(denominacion);
                        estacionesDTO.setIndicativo(punto);
                        estacionesDTO.setRedOrigen(Constants.CHS);
                        estacionesDTOList.add(estacionesDTO);
                    }
                }
            }
            precipitacionesDAO.insertarEstacionesChs(estacionesDTOList);
        } catch (Exception e) {
            System.err.println("Error crítico: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    public void getAndSavePrecipitacionesRealTime() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

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
                // Fallback: Intentar al menos el clic físico si el JS falla
                try {
                    WebElement lengthMenu = driver.findElement(By.name("tablaVisorPrecipitaciones_length"));
                    new Select(lengthMenu).selectByValue("100");
                    Thread.sleep(5000);
                } catch (Exception ex) {
                    System.out.println("No se pudo expandir de ninguna forma.");
                }
            }

            // 3. Localizar todas las filas renderizadas
            List<WebElement> filas = driver.findElements(By.cssSelector("#tablaVisorPrecipitaciones tbody tr"));

            List<EstacionesDTO> estacionesDTOList = new ArrayList<>();

            for (WebElement fila : filas) {
                List<WebElement> celdas = fila.findElements(By.tagName("td"));

                // Verificamos que la fila tenga al menos 8 columnas para evitar errores
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
                        estacionesDTOList.add(estacionesDTO);
                    }
                }
            }

            precipitacionesDAO.guardarValoresPrecipitaciones(estacionesDTOList);
        } catch (Exception e) {
            System.err.println("Error crítico: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
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

}