package com.app.modules.hidrology.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import okhttp3.Request;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class PrecipitacionesService {

    public void extraerPrecipitacionesRealTime() {
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

            System.out.printf("%-12s | %-35s | %-10s%n", "PUNTO", "ESTACIÓN", "LLUVIA 24H");
            System.out.println("-------------------------------------------------------------------------");

            int estacionesEncontradas = 0;
            for (WebElement fila : filas) {
                List<WebElement> celdas = fila.findElements(By.tagName("td"));

                // Verificamos que la fila tenga al menos 8 columnas para evitar errores
                if (celdas.size() >= 8) {
                    String denominacion = celdas.get(0).getText().trim();
                    String punto = celdas.get(1).getText().trim();

                    if (!punto.isEmpty() && !punto.equalsIgnoreCase("No data available in table")) {

                        // Extraemos los textos de cada intervalo
                        String lluvia1hStr = celdas.get(3).getText().trim();
                        String lluvia3hStr = celdas.get(4).getText().trim();
                        String lluvia6hStr = celdas.get(5).getText().trim();
                        String lluvia12hStr = celdas.get(6).getText().trim();
                        String lluvia24hStr = celdas.get(7).getText().trim();

                        // Convertimos a Double usando una función auxiliar para limpiar la coma
                        double h1 = limpiarValor(lluvia1hStr);
                        double h3 = limpiarValor(lluvia3hStr);
                        double h6 = limpiarValor(lluvia6hStr);
                        double h12 = limpiarValor(lluvia12hStr);
                        double h24 = limpiarValor(lluvia24hStr);

                        System.out.printf("%-12s | %-25s | 1h: %.1f | 3h: %.1f | 6h: %.1f | 12h: %.1f | 24h: %.1f%n",
                                punto, denominacion.substring(0, Math.min(denominacion.length(), 25)),
                                h1, h3, h6, h12, h24);

                        // Aquí enviarías al DAO con todos los parámetros
                        // embalseDAO.guardarPrecipitacionCompleta(punto, denominacion, h1, h3, h6, h12, h24);
                    }
                }
            }

            System.out.println("-------------------------------------------------------------------------");
            System.out.println("Extracción finalizada. Estaciones capturadas: " + estacionesEncontradas);

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
}