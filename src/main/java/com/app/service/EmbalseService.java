package com.app.service;

import com.app.constantes.Constants;
import com.app.constantes.Tendencia;
import com.app.dao.EmbalseDAO;
import com.app.dto.EmbalseDTO;
import com.app.dto.HistoricoCuencaDTO;
import com.app.exceptions.Exceptions;
import com.app.exceptions.FunctionalExceptions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmbalseService {

    public EmbalseService() throws NoSuchAlgorithmException, KeyManagementException {
        configureSSL();
    }

    private static final Logger logger = LoggerFactory.getLogger(EmbalseService.class);

    @Autowired
    private EmbalseDAO embalseDAO;

    public void obtenerAndActualizarDatosDeLaWeb() throws FunctionalExceptions {
        try {
            Document doc = Jsoup.connect("https://saihweb.chsegura.es/apps/iVisor/inicial.php").get();
            String todoElTexto = doc.body().text();

            // Este patrón busca: "E.Nombre (Cota) H Hm3 %"
            // Ejemplo: E.Fuensanta (67,92) 34,69 29,184 13,9
            Pattern p = Pattern.compile("E\\.([a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+)\\s\\([^\\)]+\\)\\s[0-9,.]+\\s([0-9,.]+)\\s([0-9,.]+)");
            Matcher m = p.matcher(todoElTexto);

                while (m.find()) {
                    String nombre = m.group(1).trim().toUpperCase();
                    double hm3Actual = Double.parseDouble(m.group(2).replace(",", "."));
                    double porc = Double.parseDouble(m.group(3).replace(",", "."));
                    double hm3Anterior = embalseDAO.obtenerHm3AnteriorYActualizar(nombre, hm3Actual);
                    double variacion = hm3Actual - hm3Anterior;

                    Tendencia tendencia = switch (Double.compare(variacion, 0)) {
                        case 1  -> Tendencia.SUBIDA;
                        case -1 -> Tendencia.BAJADA;
                        default -> Tendencia.ESTABLE;
                    };

                    embalseDAO.guardarLectura(nombre, hm3Actual, porc, variacion, tendencia);

                }
            } catch (Exception e) {
                Exceptions.EMB_E_0001.lanzarExcepcionCausada(e);
            }
    }


    public void obtenerDatosWebAndUpdateEach3hours() throws FunctionalExceptions {
        try {
            Document doc = Jsoup.connect("https://saihweb.chsegura.es/apps/iVisor/inicial.php").get();
            String todoElTexto = doc.body().text();

            // Este patrón busca: "E.Nombre (Cota) H Hm3 %"
            // Ejemplo: E.Fuensanta (67,92) 34,69 29,184 13,9
            Pattern p = Pattern.compile("E\\.([a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+)\\s\\([^\\)]+\\)\\s[0-9,.]+\\s([0-9,.]+)\\s([0-9,.]+)");
            Matcher m = p.matcher(todoElTexto);

            double volumenActualCuenca = 0.0;
            double porcentajeTotalCuenca = 0.0;
            while (m.find()) {

                double volumenActualEmbalse = Double.parseDouble(m.group(2).replace(",", "."));
                volumenActualCuenca = volumenActualCuenca + volumenActualEmbalse;

            }

            porcentajeTotalCuenca = (volumenActualCuenca * 100) / Constants.VOLUMEN_MAXIMO_CUENCA_SEGURA;

            embalseDAO.insertarValoresEnHistoricoCuencaSegura(volumenActualCuenca, porcentajeTotalCuenca);
        } catch (Exception e) {
            Exceptions.EMB_E_0001.lanzarExcepcionCausada(e);
        }
    }

    public List<EmbalseDTO> obtenerListadoParaFront() throws FunctionalExceptions {

        List<EmbalseDTO> lista = new ArrayList<>();
        try {
            lista = embalseDAO.obtenerUltimasLecturasConVariacion();
        } catch (Exception e) {
            Exceptions.EMB_E_0003.lanzarExcepcionCausada(e);
        }
        return lista;
    }



    public void checkDatabaseNeonConnection() throws FunctionalExceptions {
        embalseDAO.checkDatabaseConnection();
    }



    public List<HistoricoCuencaDTO> getHistoricoCuenca() throws FunctionalExceptions {
        return embalseDAO.getHistoricoCuencaSeguraList();
    }

    private void configureSSL() throws NoSuchAlgorithmException, KeyManagementException {
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
    }

}
