package com.app.modules.weather.service;

import com.app.core.exceptions.FunctionalExceptions;
import com.app.core.jooq.generated.tables.EstacionesMeteorologicas;
import com.app.core.model.Precipitaciones;
import com.app.core.repository.PrecipitacionesRepository;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import io.github.bonigarcia.wdm.WebDriverManager;
import okhttp3.OkHttpClient;
import org.jooq.DSLContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.app.core.jooq.generated.Tables.ESTACIONES_METEOROLOGICAS;
import static com.app.core.jooq.generated.Tables.PRECIPITACIONES;

@Service
public class PrecipitacionesService {

    @Autowired
    private PrecipitacionesRepository precipitacionesRepository;

    @Autowired
    private EstacionesService estacionesService;

    @Autowired
    private DSLContext dsl;

    private double limpiarValor(String valor) {
        try {
            if (valor == null || valor.equals("-") || valor.isEmpty()) return 0.0;
            return Double.parseDouble(valor.replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }

    public List<EstacionesDTO> obtenerMapaRapido() {
        return dsl.select(
                        // Seleccionamos los campos de la tabla generada por jOOQ
                        ESTACIONES_METEOROLOGICAS.NOMBRE,
                        ESTACIONES_METEOROLOGICAS.INDICATIVO,
                        ESTACIONES_METEOROLOGICAS.LATITUD,
                        ESTACIONES_METEOROLOGICAS.LONGITUD,
                        PRECIPITACIONES.PRECIPITACION_24H,
                        PRECIPITACIONES.FECHA_ACTUALIZACION
                )
                .distinctOn(ESTACIONES_METEOROLOGICAS.INDICATIVO) // Si falla, usa dsl.selectDistinctOn(...)
                .from(ESTACIONES_METEOROLOGICAS)
                .leftJoin(PRECIPITACIONES)
                .on(ESTACIONES_METEOROLOGICAS.INDICATIVO.eq(PRECIPITACIONES.INDICATIVO))
                .orderBy(ESTACIONES_METEOROLOGICAS.INDICATIVO, PRECIPITACIONES.FECHA_ACTUALIZACION.desc())
                .fetchInto(EstacionesDTO.class);
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

    public void getValuesOfTodayAemetByProvincia(String provincia, String apiKeyAemet) throws FunctionalExceptions {

        List<EstacionesDTO> estacionesAemetDTOListFilterByProvincia = estacionesService.obtenerEstacionesAemetPorProvincia(provincia, apiKeyAemet);

        OkHttpClient client = new OkHttpClient();


    }
}