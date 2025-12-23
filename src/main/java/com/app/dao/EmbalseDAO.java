package com.app.dao;

import com.app.constantes.Constants;
import com.app.constantes.Tendencia;
import com.app.dto.EmbalseDTO;
import com.app.dto.HistoricoCuencaDTO;
import com.app.exceptions.Exceptions;
import com.app.exceptions.FunctionalExceptions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Repository
public class EmbalseDAO {

    public double obtenerHm3AnteriorYActualizar(String nombre, double hm3Actual) throws SQLException {
        double hm3Anterior = hm3Actual; // Por defecto 0 variación si es nuevo

        // 1. Consultar el último valor guardado
        String sqlSelect = "SELECT hm3_anterior FROM historico_embalses WHERE nombre = ?";

        // 2. Insertar o actualizar (Upsert)
        String sqlUpsert = "INSERT INTO historico_embalses (nombre, hm3_anterior, fecha_actualizacion) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (nombre) DO UPDATE SET " +
                "hm3_anterior = EXCLUDED.hm3_anterior, " +
                "fecha_actualizacion = CURRENT_TIMESTAMP";

        try (Connection conn = DatabaseConfig.getConnection()) {
            // Buscamos el valor previo
            try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                ps.setString(1, nombre);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        hm3Anterior = rs.getDouble("hm3_anterior");
                    }
                }
            }

            // Actualizamos con el nuevo valor para la próxima consulta
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpsert)) {
                psUpdate.setString(1, nombre);
                psUpdate.setDouble(2, hm3Actual);
                psUpdate.executeUpdate();
            }
        }
        return hm3Anterior;
    }

    public void guardarLectura(Integer embalseId, double hm3, double porc, Double variacion, Tendencia tendencia) throws SQLException {

        String sqlInsertaLectura = "INSERT INTO lecturas_embalses (embalse_id, hm3_actual, porcentaje, variacion, tendencia) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            //  Insertar la lectura asociada al ID obtenido
            try (PreparedStatement psLectura = conn.prepareStatement(sqlInsertaLectura)) {
                psLectura.setInt(1, embalseId);
                psLectura.setDouble(2, hm3);
                psLectura.setDouble(3, porc);

                // Manejo de nulos para la variación (en caso de que sea la primera lectura)
                if (variacion != null) {
                    psLectura.setDouble(4, variacion);
                } else {
                    psLectura.setNull(4, java.sql.Types.DOUBLE);
                }

                psLectura.setString(5, tendencia.getValor());
                psLectura.executeUpdate();
            }
        }
    }

    public List<EmbalseDTO> obtenerUltimasLecturasConVariacionPorIntervalo(String intervalo) throws SQLException {
        List<EmbalseDTO> lista = new ArrayList<>();

        // Usamos el intervalo como parámetro en la consulta SQL
        String sql = "WITH LecturaActual AS (" +
                "    SELECT DISTINCT ON (embalse_id) * FROM lecturas_embalses ORDER BY embalse_id, fecha_registro DESC" +
                "), LecturaPasada AS (" +
                "    SELECT DISTINCT ON (embalse_id) * FROM lecturas_embalses " +
                "    WHERE fecha_registro <= NOW() - CAST(? AS INTERVAL) " + // <--- Parámetro dinámico
                "    ORDER BY embalse_id, fecha_registro DESC" +
                ") " +
                "SELECT e.id, e.nombre, e.capacidad_maxima, curr.hm3_actual, curr.porcentaje, curr.fecha_registro, " +
                "(curr.hm3_actual - COALESCE(prev.hm3_actual, curr.hm3_actual)) AS variacion_calculada " +
                "FROM embalses e " +
                "JOIN LecturaActual curr ON e.id = curr.embalse_id " +
                "LEFT JOIN LecturaPasada prev ON e.id = prev.embalse_id";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, intervalo); // Ejemplo: "1 day" o "7 days"

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new EmbalseDTO(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getDouble("hm3_actual"),
                            rs.getDouble("porcentaje"),
                            rs.getDouble("capacidad_maxima"),
                            rs.getDouble("variacion_calculada"),
                            Tendencia.ESTABLE, // La tendencia la calcularemos en el Front según la variación
                            rs.getTimestamp("fecha_registro")
                    ));
                }
            }
        }
        return lista;
    }

    public void insertarValoresEnHistoricoCuencaSegura(double volumenActualCuenca, double porc, String nombreTabla) throws SQLException {

        String sqlInsertaLectura = "INSERT INTO " + nombreTabla + " (volumen_total, porcentaje_total, fecha_registro) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement psLectura = conn.prepareStatement(sqlInsertaLectura)) {

                psLectura.setDouble(1, volumenActualCuenca);
                psLectura.setDouble(2, porc);
                psLectura.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace(); // Es vital para ver el detalle en los logs de Render
                throw new RuntimeException("Error en la BD Neon: " + e.getMessage());
            }
        }
    }

    public List<HistoricoCuencaDTO> getHistoricoCuencaSeguraList(String nombreTabla) throws FunctionalExceptions {

        int intentos = 0;
        boolean exito = false;

        String sqlSelect = "SELECT volumen_total, porcentaje_total, fecha_registro FROM " + nombreTabla + " ORDER BY fecha_registro ASC";

        List<HistoricoCuencaDTO> historicoCuencaDTOList = new ArrayList<>();

        while (intentos < 3 && !exito) {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlSelect);
                 ResultSet rs = ps.executeQuery()) {

                exito = true;

                while (rs.next()) {

                    historicoCuencaDTOList.add(new HistoricoCuencaDTO(
                            rs.getDouble("volumen_total"),
                            rs.getDouble("porcentaje_total"),
                            rs.getTimestamp("fecha_registro")
                    ));
                }

            } catch (Exception e) {
                intentos++;
                if (intentos >= 3) {
                    Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
                }
                manejarEspera(3000L);
            }
        }
        return historicoCuencaDTOList;
    }

    public List<HistoricoCuencaDTO> getHistoricoCuencaSeguraUltimoDia() throws FunctionalExceptions {

        List<HistoricoCuencaDTO> historicoCuencaDTOList = new ArrayList<>();
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

            this.insertarValoresEnHistoricoCuencaSegura(volumenActualCuenca, porcentajeTotalCuenca, Constants.TABLA_HISTORICO_CUENCA_SEGURA_DIARIO);

            historicoCuencaDTOList = this.getHistoricoCuencaSeguraList(Constants.TABLA_HISTORICO_CUENCA_SEGURA_DIARIO);
        } catch (Exception e) {
            Exceptions.EMB_E_0001.lanzarExcepcionCausada(e);
        }
        return historicoCuencaDTOList;
    }

    public void checkDatabaseConnection() throws FunctionalExceptions {
        int intentos = 0;
        boolean conectado = false;

        while (intentos < 3 && !conectado) {
            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.executeQuery("SELECT 1");
                conectado = true; // Si llega aquí, todo ok

            } catch (Exception e) {
                intentos++;
                if (intentos >= 3) {
                    Exceptions.EMB_E_0003.lanzarExcepcionCausada(e);
                }
                manejarEspera(8000L);
            }
        }
    }

    public void manejarEspera(Long time) {
        try {
            Thread.sleep(time); // Espera X segundos antes de reintentar
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public List<EmbalseDTO> obtenerHistoricoEmbalsePorIdEmbalse(int idEmbalse) throws FunctionalExceptions {
        List<EmbalseDTO> embalseDTOList = new ArrayList<>();

        String sqlHistoricoEmbalse = "SELECT lecturas.*, emb.nombre AS nombre_embalse, emb.capacidad_maxima FROM lecturas_embalses lecturas " +
                "JOIN embalses emb ON lecturas.embalse_id = emb.id WHERE lecturas.embalse_id = ? ORDER BY lecturas.fecha_registro ASC";

        int intentos = 0;
        boolean conectado = false;

        while (intentos < 3 && !conectado) {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlHistoricoEmbalse)) {

                ps.setInt(1, idEmbalse);

                try (ResultSet rs = ps.executeQuery()) {
                    conectado = true;
                    while (rs.next()) {

                        String tendenciaBD = rs.getString("tendencia");
                        Tendencia tendencia = (tendenciaBD != null)
                                ? Tendencia.valueOf(tendenciaBD.toUpperCase())
                                : Tendencia.ESTABLE;

                        embalseDTOList.add(new EmbalseDTO(
                                rs.getInt("embalse_id"),
                                rs.getString("nombre_embalse"),
                                rs.getDouble("hm3_actual"),
                                rs.getDouble("porcentaje"),
                                rs.getDouble("capacidad_maxima"),
                                rs.getDouble("variacion"),
                                tendencia,
                                rs.getTimestamp("fecha_registro")
                        ));
                    }
                }
            } catch (SQLException e) {
                intentos++;
                if (intentos >= 3) {
                    Exceptions.EMB_E_0003.lanzarExcepcionCausada(e);
                }
                manejarEspera(3000L);
            }
        }
        return embalseDTOList;
    }

}
