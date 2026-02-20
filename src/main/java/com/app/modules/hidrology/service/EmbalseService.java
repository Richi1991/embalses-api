package com.app.modules.hidrology.service;

import com.app.core.constantes.Constants;
import com.app.modules.hidrology.dto.*;
import com.app.modules.hidrology.dao.EmbalseDAO;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.impl.DSL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jooq.impl.DSL.*;
import static com.app.core.jooq.generated.Tables.LECTURAS_EMBALSES;

@Service
public class EmbalseService {

    public EmbalseService() throws NoSuchAlgorithmException, KeyManagementException {
        this.configureSSL();
    }

    private static final Logger logger = LoggerFactory.getLogger(EmbalseService.class);

    @Autowired
    private EmbalseDAO embalseDAO;

    @Autowired
    private DSLContext dsl;

    public record LecturaProcesada(int id, double hm3, double porc, double variacion, String tendencia) {}

    public void getEmbalsesDataAndInsertInLecturasEmbalses() throws FunctionalExceptions {
        try {
            // 1. Scraping
            Document doc = Jsoup.connect("https://saihweb.chsegura.es/apps/iVisor/inicial.php").timeout(10000).get();
            String todoElTexto = doc.body().text();
            Pattern p = Pattern.compile("E\\.([a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+)\\s\\([^\\)]+\\)\\s[0-9,.]+\\s([0-9,.]+)\\s([0-9,.]+)");
            Matcher m = p.matcher(todoElTexto);

            // 2. Traer últimos Hm3 (Metodología Revolut: Una sola consulta)
            // Obtenemos el Hm3 actual agrupando por ID y usando una subconsulta para la fecha máxima
            Map<Integer, BigDecimal> ultimosHm3Map = dsl
                    .select(LECTURAS_EMBALSES.EMBALSE_ID, LECTURAS_EMBALSES.HM3_ACTUAL)
                    .from(LECTURAS_EMBALSES)
                    .where(LECTURAS_EMBALSES.FECHA_REGISTRO.in(
                            dsl.select(max(LECTURAS_EMBALSES.FECHA_REGISTRO))
                                    .from(LECTURAS_EMBALSES)
                                    .groupBy(LECTURAS_EMBALSES.EMBALSE_ID)
                    ))
                    .fetchMap(LECTURAS_EMBALSES.EMBALSE_ID, LECTURAS_EMBALSES.HM3_ACTUAL);

            List<LecturaProcesada> lecturasNuevas = new ArrayList<>();

            while (m.find()) {
                String nombreWeb = m.group(1).trim().toUpperCase();
                int idEmbalse = EmbalseEnum.resolverId(nombreWeb);
                if (idEmbalse == 0) continue;

                double hm3Actual = Double.parseDouble(m.group(2).replace(",", "."));
                double porc = Double.parseDouble(m.group(3).replace(",", "."));

                // Lógica de variación (Usando el mapa de la DB)
                double hm3Anterior = ultimosHm3Map.getOrDefault(idEmbalse, BigDecimal.valueOf(hm3Actual)).doubleValue();
                double variacion = hm3Actual - hm3Anterior;
                String tendencia = calcularTendenciaString(variacion);

                lecturasNuevas.add(new LecturaProcesada(idEmbalse, hm3Actual, porc, variacion, tendencia));
            }

            // 3. Batch Insert (Sin errores de tipos)
            List<Query> inserts = new ArrayList<>();
            for (LecturaProcesada l : lecturasNuevas) {
                inserts.add(
                        dsl.insertInto(LECTURAS_EMBALSES)
                                .set(LECTURAS_EMBALSES.EMBALSE_ID, l.id())
                                .set(LECTURAS_EMBALSES.HM3_ACTUAL, BigDecimal.valueOf(l.hm3()))
                                .set(LECTURAS_EMBALSES.PORCENTAJE, BigDecimal.valueOf(l.porc()))
                                .set(LECTURAS_EMBALSES.VARIACION, BigDecimal.valueOf(l.variacion()))
                                .set(LECTURAS_EMBALSES.TENDENCIA, l.tendencia())
                                .set(LECTURAS_EMBALSES.FECHA_REGISTRO, DSL.currentLocalDateTime()) // O DSL.now()
                );
            }

            // Ejecución atómica
            if (!inserts.isEmpty()) {
                dsl.batch(inserts).execute();
            }

        } catch (Exception e) {
            Exceptions.EMB_E_0001.lanzarExcepcionCausada(e);
        }
    }

    private String calcularTendenciaString(double variacion) {
        // Usamos un umbral pequeño (precision) para evitar que cambios de 0.000001
        // se marquen como subida/bajada si son despreciables
        if (Math.abs(variacion) < 0.001) {
            return "ESTABLE";
        }
        return variacion > 0 ? "SUBIDA" : "BAJADA";
    }

    public void getAndInsertHistoricoCuencaSegura() throws FunctionalExceptions {
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

            embalseDAO.insertarValoresEnHistoricoCuencaSegura(volumenActualCuenca, porcentajeTotalCuenca, Constants.TABLA_HISTORICO_CUENCA_SEGURA);
        } catch (Exception e) {
            Exceptions.EMB_E_0001.lanzarExcepcionCausada(e);
        }
    }

    public List<EmbalseDTO> obtenerUltimasLecturasConVariacionPorIntervalo(String intervalo) throws FunctionalExceptions {

        List<EmbalseDTO> lista = new ArrayList<>();
        try {
            lista = embalseDAO.obtenerUltimasLecturasConVariacionPorIntervalo(intervalo);
        } catch (Exception e) {
            Exceptions.EMB_E_0003.lanzarExcepcionCausada(e);
        }
        return lista;
    }



    public void checkDatabaseNeonConnection() throws FunctionalExceptions {
        embalseDAO.checkDatabaseConnection();
    }



    public List<HistoricoCuencaDTO> getHistoricoCuencaSegura() throws FunctionalExceptions {
        return embalseDAO.getHistoricoCuencaSeguraList(Constants.TABLA_HISTORICO_CUENCA_SEGURA);
    }



    public List<EmbalseDTO> obtenerHistoricoEmbalsePorIdEmbalse(int idEmbalse) throws FunctionalExceptions {
        return embalseDAO.obtenerHistoricoEmbalsePorIdEmbalse(idEmbalse);
    }

    public List<HistoricoCuencaDTO> getHistoricoCuencaSeguraUltimoDia() throws FunctionalExceptions {
        return embalseDAO.getHistoricoCuencaSeguraList(Constants.TABLA_HISTORICO_CUENCA_SEGURA_DIARIO);
    }

    public void getAndInsertHistoricoCuencaSeguraHorario() throws IOException, SQLException {
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

        embalseDAO.insertarValoresEnHistoricoCuencaSegura(volumenActualCuenca, porcentajeTotalCuenca, Constants.TABLA_HISTORICO_CUENCA_SEGURA_DIARIO);
    }

    public void configureSSL() throws NoSuchAlgorithmException, KeyManagementException {
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
