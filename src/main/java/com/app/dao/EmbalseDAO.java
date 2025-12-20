package com.app.dao;

import com.app.constantes.Tendencia;
import com.app.dto.EmbalseDTO;
import com.app.dto.HistoricoCuencaDTO;
import com.app.exceptions.Exceptions;
import com.app.exceptions.FunctionalExceptions;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    public void guardarLectura(String nombre, double hm3, double porc, Double variacion, Tendencia tendencia) throws SQLException {
        // Usamos esta técnica de UPDATE para garantizar que RETURNING id siempre devuelva el valor, exista o no
        String sqlUpsertEmbalse = "INSERT INTO embalses (nombre) VALUES (?) " +
                "ON CONFLICT (nombre) DO UPDATE SET nombre = EXCLUDED.nombre " +
                "RETURNING id";

        String sqlInsertaLectura = "INSERT INTO lecturas_embalses (embalse_id, hm3_actual, porcentaje, variacion, tendencia) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            int embalseId = -1;

            // 1. Obtener o Crear el embalse en un solo paso
            try (PreparedStatement psIns = conn.prepareStatement(sqlUpsertEmbalse)) {
                psIns.setString(1, nombre);
                try (ResultSet rs = psIns.executeQuery()) {
                    if (rs.next()) {
                        embalseId = rs.getInt("id");
                    }
                }
            }

            // 2. Insertar la lectura asociada al ID obtenido
            if (embalseId != -1) {
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
            } else {
                System.err.println("Error: No se pudo obtener el ID para el embalse: " + nombre);
            }
        }
    }

    public List<EmbalseDTO> obtenerUltimasLecturasConVariacion() throws SQLException {
        List<EmbalseDTO> lista = new ArrayList<>();

        // DISTINCT ON nos da la fila más reciente (fecha_registro DESC) para cada embalse
        String sql = "SELECT DISTINCT ON (e.id) " +
                "l.embalse_id, e.nombre, l.hm3_actual, l.porcentaje, l.variacion, l.tendencia, l.fecha_registro " +
                "FROM embalses e " +
                "JOIN lecturas_embalses l ON e.id = l.embalse_id " +
                "ORDER BY e.id, l.fecha_registro DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Convertimos el String de la BD al Enum de Java
                String tendenciaBD = rs.getString("tendencia");
                Tendencia tendenciaEnum = (tendenciaBD != null)
                        ? Tendencia.valueOf(tendenciaBD.toUpperCase())
                        : Tendencia.ESTABLE;

                lista.add(new EmbalseDTO(
                        rs.getInt("embalse_id"),
                        rs.getString("nombre"),
                        rs.getDouble("hm3_actual"),
                        rs.getDouble("porcentaje"),
                        rs.getDouble("variacion"),
                        tendenciaEnum,
                        rs.getTimestamp("fecha_registro")
                ));
            }
        }
        return lista;
    }

    public void insertarValoresEnHistoricoCuencaSegura(double volumenActualCuenca, double porc) throws SQLException {

        String sqlInsertaLectura = "INSERT INTO historico_cuenca_segura (volumen_total, porcentaje_total, fecha_registro) " +
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

    public List<HistoricoCuencaDTO> getHistoricoCuencaSeguraList() throws FunctionalExceptions {

        int intentos = 0;
        boolean exito = false;

        String sqlSelect = "SELECT volumen_total, porcentaje_total, fecha_registro FROM historico_cuenca_segura ORDER BY fecha_registro ASC";

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

}
