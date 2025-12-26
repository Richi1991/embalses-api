package com.app.modules.hidrology.service;

import com.app.core.constantes.Constants;
import com.app.modules.weather.dao.PrecipitacionesDAO;
import com.app.modules.weather.dto.EstacionesDTO;
import io.github.bonigarcia.wdm.WebDriverManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PrecipitacionesService {

    @Autowired
    private PrecipitacionesDAO precipitacionesDAO;

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

            List<EstacionesDTO> estacionesDTOList = new ArrayList<>();

            for (WebElement fila : filas) {
                List<WebElement> celdas = fila.findElements(By.tagName("td"));

                // Verificamos que la fila tenga al menos 8 columnas para evitar errores
                if (celdas.size() >= 8) {
                    String denominacion = celdas.get(0).getText().trim();
                    String punto = celdas.get(1).getText().trim();

                    if (!punto.isEmpty() && !punto.equalsIgnoreCase("No data available in table")) {

                        // Extraemos los textos de cada intervalo
//                        String lluvia1hStr = celdas.get(3).getText().trim();
//                        String lluvia3hStr = celdas.get(4).getText().trim();
//                        String lluvia6hStr = celdas.get(5).getText().trim();
//                        String lluvia12hStr = celdas.get(6).getText().trim();
//                        String lluvia24hStr = celdas.get(7).getText().trim();

                        EstacionesDTO estacionesDTO = new EstacionesDTO();
                        estacionesDTO.setNombre(denominacion);
                        estacionesDTO.setIndicativo(punto);
                        estacionesDTO.setRedOrigen(Constants.CHS);

                        // Convertimos a Double usando una función auxiliar para limpiar la coma
//                        double h1 = limpiarValor(lluvia1hStr);
//                        double h3 = limpiarValor(lluvia3hStr);
//                        double h6 = limpiarValor(lluvia6hStr);
//                        double h12 = limpiarValor(lluvia12hStr);
//                        double h24 = limpiarValor(lluvia24hStr);

//                        Map<String, double[]> mapEstacionesCoordenadas = obtenerCoordenadas(punto);

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

    public Map<String, double[]> obtenerCoordenadas(String codigoPunto) throws IOException {

        Map<String, double[]> mapaPosiciones = new HashMap<>();

        // Traemos TODOS los registros con coordenadas en formato GPS (4326)
        String urlTodas = "https://www.chsegura.es/server/rest/services/DashboardServices/DashDatosBase/MapServer/23/query"
                + "?where=1%3D1&outFields=COD_CHS&outSR=4326&returnGeometry=true&f=json";
        // 1. Configurar OkHttpClient para que ignore errores de certificado SSL
        OkHttpClient client = getUnsafeOkHttpClient();

        // 2. Añadir User-Agent para evitar que el servidor nos bloquee
        Request request = new Request.Builder()
                .url(urlTodas)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode root = new ObjectMapper().readTree(response.body().string());
                JsonNode features = root.path("features");

                for (JsonNode feature : features) {
                    String cod = feature.path("attributes").path("COD_CHS").asText().trim();
                    double lat = feature.path("geometry").path("y").asDouble();
                    double lon = feature.path("geometry").path("x").asDouble();

                    if (!cod.isEmpty() && lat != 0) {
                        mapaPosiciones.put(cod, new double[]{lat, lon});
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al consultar coordenadas para " + codigoPunto + ": " + e.getMessage());
        }

        return mapaPosiciones;
    }

    // Método auxiliar para saltar validación SSL (Necesario para webs gubernamentales a veces)
    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                    }
            };
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    // AUMENTO DE TIEMPOS DE ESPERA
                    .connectTimeout(30, TimeUnit.SECONDS) // Tiempo para establecer conexión
                    .readTimeout(30, TimeUnit.SECONDS)    // Tiempo para recibir los datos
                    .writeTimeout(30, TimeUnit.SECONDS)   // Tiempo para enviar la petición
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
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