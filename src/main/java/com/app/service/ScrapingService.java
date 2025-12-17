package com.app.service;

import com.app.dto.EmbalseDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScrapingService {

 /*   public List<EmbalseDTO> obtenerDatosDeLaWeb() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        List<EmbalseDTO> resultados = new ArrayList<>();
        try {
        // --- INICIO TRUCO DE EMERGENCIA ---
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                }
        };

        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // --- FIN TRUCO DE EMERGENCIA ---

        // Ahora Jsoup conectará sin protestar por el certificado
        Document doc = Jsoup.connect("https://saihweb.chsegura.es/apps/iVisor/inicial.php").get();
        // En lugar de "tr", buscamos los contenedores que tienen el atributo "title"
        // que es donde la web guarda los IDs internos (como "02S01E1")
        Elements bloques = doc.select("div[title^=02]"); // Busca divs cuyo title empieza por 02

        for (Element bloque : bloques) {
            // El nombre está en el texto del propio div
            String textoCompleto = bloque.text();

            if (textoCompleto.contains("(")) {
                // 1. Limpiamos nombre: "Azud Ojós (10,65)" -> "AZUD OJÓS"
                String nombre = textoCompleto.split("\\(")[0].trim().toUpperCase();

                // Los datos de Hm3 y % suelen ser los divs hermanos siguientes
                // O hijos dependiendo de la profundidad. Según tu imagen:
                Element filaContenedora = bloque.parent();
                Elements valores = filaContenedora.select("div");

                if (valores.size() >= 4) {
                    try {
                        // El valor Hm3 suele ser el segundo o tercero
                        double hm3 = Double.parseDouble(valores.get(1).text().replace(",", ".").trim());
                        // El porcentaje suele ser el último (el que tiene el fondo azul)
                        double porc = Double.parseDouble(valores.get(3).text().replace(",", ".").trim());

                        double var = -1.5 + (Math.random() * 3.0);
                        String tendencia = var >= 0 ? "subida" : "bajada";

                        resultados.add(new EmbalseDTO(nombre, hm3, porc, var, tendencia));
                    } catch (Exception e) {
                        // Si no es un número, saltamos este bloque
                        continue;
                    }
                }
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return resultados;
    }*/

    public List<EmbalseDTO> obtenerDatosDeLaWeb() throws IOException, NoSuchAlgorithmException, KeyManagementException {
        List<EmbalseDTO> resultados = new ArrayList<>();
        try {
            // --- INICIO TRUCO DE EMERGENCIA ---
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    }
            };

            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            // --- FIN TRUCO DE EMERGENCIA ---

                Document doc = Jsoup.connect("https://saihweb.chsegura.es/apps/iVisor/inicial.php").get();
                String todoElTexto = doc.body().text();

                // Este patrón busca: "E.Nombre (Cota) H Hm3 %"
                // Ejemplo: E.Fuensanta (67,92) 34,69 29,184 13,9
                Pattern p = Pattern.compile("E\\.([a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+)\\s\\([^\\)]+\\)\\s[0-9,.]+\\s([0-9,.]+)\\s([0-9,.]+)");
                Matcher m = p.matcher(todoElTexto);

                while (m.find()) {
                    String nombre = m.group(1).trim().toUpperCase();
                    double hm3 = Double.parseDouble(m.group(2).replace(",", "."));
                    double porc = Double.parseDouble(m.group(3).replace(",", "."));

                    // Tu toque "Fintech"
                    double var = -1.5 + (Math.random() * 3.0);
                    String tendencia = var >= 0 ? "subida" : "bajada";

                    resultados.add(new EmbalseDTO(nombre, hm3, porc, var, tendencia));
                    System.out.println("Capturado: " + nombre + " - " + porc + "%");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return resultados;
    }
}
