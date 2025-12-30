package com.app.modules.weather.service;

import com.app.core.model.Precipitaciones;
import com.app.core.repository.PrecipitacionesRepository;
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
import java.util.stream.Collectors;

@Service
public class PrecipitacionesService {

    @Autowired
    private PrecipitacionesDAO precipitacionesDAO;

    @Autowired
    private PrecipitacionesRepository precipitacionesRepository;

    public void getAndSavePrecipitacionesRealTime1() {
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
                if (batch.size() >= batchSize || i == filas.size() - 1) {
                    if (!batch.isEmpty()) {
                        // CONVERSIÓN: De DTO a Entity
                        List<Precipitaciones> entidades = batch.stream().map(dto -> {
                            Precipitaciones p = new Precipitaciones();
                            p.setNombre(dto.getNombre());
                            p.setPrecipitacion1h(dto.getPrecipitacionesDTO().getPrecipitacion1h());
                            p.setPrecipitacion3h(dto.getPrecipitacionesDTO().getPrecipitacion3h());
                            p.setPrecipitacion6h(dto.getPrecipitacionesDTO().getPrecipitacion6h());
                            p.setPrecipitacion12h(dto.getPrecipitacionesDTO().getPrecipitacion12h());
                            p.setPrecipitacion24h(dto.getPrecipitacionesDTO().getPrecipitacion24h());
                            // La fecha se pondrá sola si usas @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
                            return p;
                        }).collect(Collectors.toList());

                        // GUARDADO: Ahora sí usamos el Repository con Entities
                        precipitacionesRepository.saveAll(entidades);

                        System.out.println("Lote de " + entidades.size() + " registros guardado con éxito.");
                        batch.clear(); // ¡IMPORTANTE! Vaciar el batch para la siguiente tanda
                    }
                }
            }

            System.out.println("Extracción de precipitaciones completada. Total filas procesadas: " + filas.size());

        } catch (Exception e) {
            System.err.println("Error crítico: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (driver != null) {
                System.out.println("Cerrando el navegador y liberando recursos...");
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

    public WebDriver createDriver() {
        // 1. Setup automático del driver compatible
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();

        // 2. Lógica multiplataforma para el binario de Chrome
        String chromeBin = System.getenv("CHROME_BIN");

        if (chromeBin != null && !chromeBin.isEmpty()) {
            // Si existe la variable (como en tu Dockerfile de Render), la usamos
            options.setBinary(chromeBin);
        } else {
            // Si no hay variable y estamos en Linux (Render/Docker), intentamos la ruta estándar
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("linux")) {
                options.setBinary("/usr/bin/google-chrome-stable");
            }
        }

        // 3. Argumentos optimizados para Render y Windows
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--window-size=800,600");
        options.addArguments("--blink-settings=imagesEnabled=false");

        return new ChromeDriver(options);
    }

    public void getAndSavePrecipitacionesRealTime() {
        WebDriver driver = createDriver();

        try {
            driver.get("https://www.chsegura.es/es/cuenca/redes-de-control/saih/informe-horario-de-precipitaciones/");
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id("tablaVisorPrecipitaciones")));

            // Expandir tabla
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            js.executeScript("$('#tablaVisorPrecipitaciones').DataTable().page.len(500).draw();");
            Thread.sleep(3000);

            // CAPTURA RÁPIDA Y CIERRE (Clave para Render)
            String htmlContent = driver.getPageSource();
            driver.quit();
            driver = null;

            // PROCESAMIENTO CON JSOUP MANTENIENDO TUS DTOs
            Document doc = Jsoup.parse(htmlContent);
            Elements filas = doc.select("#tablaVisorPrecipitaciones tbody tr");

            java.sql.Timestamp fechaCaptura = new java.sql.Timestamp(System.currentTimeMillis());

            List<EstacionesDTO> batchDTO = new ArrayList<>();
            int batchSize = 25;

            for (Element fila : filas) {
                Elements celdas = fila.select("td");
                if (celdas.size() >= 8) {
                    String punto = celdas.get(1).text().trim();
                    if (!punto.isEmpty() && !punto.equalsIgnoreCase("No data available in table")) {

                        // Mantenemos tu lógica de DTOs
                        PrecipitacionesDTO pDTO = new PrecipitacionesDTO();
                        pDTO.setPrecipitacion1h(limpiarValor(celdas.get(3).text()));
                        pDTO.setPrecipitacion3h(limpiarValor(celdas.get(4).text()));
                        pDTO.setPrecipitacion6h(limpiarValor(celdas.get(5).text()));
                        pDTO.setPrecipitacion12h(limpiarValor(celdas.get(6).text()));
                        pDTO.setPrecipitacion24h(limpiarValor(celdas.get(7).text()));

                        EstacionesDTO eDTO = new EstacionesDTO();
                        eDTO.setIndicativo(punto);
                        eDTO.setNombre(celdas.get(0).text().trim());
                        eDTO.setFechaActualizacion(fechaCaptura);
                        eDTO.setPrecipitacionesDTO(pDTO);

                        batchDTO.add(eDTO);
                    }
                }

                // Procesar y guardar en lotes para no inflar la RAM
                if (batchDTO.size() >= batchSize) {
                    guardarLote(batchDTO);
                    batchDTO.clear();
                }
            }

            // Guardar lo que quede
            if (!batchDTO.isEmpty()) {
                guardarLote(batchDTO);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            if (driver != null) driver.quit();
        }
    }

    private void guardarLote(List<EstacionesDTO> dtos) {
        List<Precipitaciones> entidades = dtos.stream().map(dto -> {
            Precipitaciones p = new Precipitaciones();

            // 1. CREAR Y ASIGNAR EL ID COMPUESTO (Solución al error Null id)
            com.app.core.model.PrecipitacioneId idCompuesto = new com.app.core.model.PrecipitacioneId();
            idCompuesto.setIndicativo(dto.getIndicativo()); // Usamos el código de la estación
            idCompuesto.setFechaActualizacion(dto.getFechaActualizacion());

            p.setId(idCompuesto);

            // 2. RELLENAR EL RESTO DE DATOS
            p.setNombre(dto.getNombre());
            p.setPrecipitacion1h(dto.getPrecipitacionesDTO().getPrecipitacion1h());
            p.setPrecipitacion3h(dto.getPrecipitacionesDTO().getPrecipitacion3h());
            p.setPrecipitacion6h(dto.getPrecipitacionesDTO().getPrecipitacion6h());
            p.setPrecipitacion12h(dto.getPrecipitacionesDTO().getPrecipitacion12h());
            p.setPrecipitacion24h(dto.getPrecipitacionesDTO().getPrecipitacion24h());

            return p;
        }).collect(Collectors.toList());

        precipitacionesRepository.saveAll(entidades);
        System.out.println("Lote de " + entidades.size() + " registros guardado con éxito.");
    }

}