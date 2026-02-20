package com.app.modules.hidrology.dao;

import com.app.core.config.DatabaseConfig;
import com.app.core.constantes.Constants;
import com.app.modules.hidrology.dto.TendenciaEnum;
import com.app.modules.hidrology.dto.EmbalseDTO;
import com.app.modules.hidrology.dto.HistoricoCuencaDTO;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.app.core.jooq.generated.Tables.EMBALSES;
import static com.app.core.jooq.generated.Tables.LECTURAS_EMBALSES;
import static org.jooq.impl.DSL.*;

@Repository
public class EmbalseDAO {

    @Autowired
    private DSLContext dsl;

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
                            TendenciaEnum.ESTABLE, // La tendencia la calcularemos en el Front según la variación
                            rs.getTimestamp("fecha_registro")
                    ));
                }
            }
        }
        return lista;
    }

    public List<EmbalseDTO> obtenerUltimasLecturasConVariacionPorIntervaloWrong(String intervalo) {

        // 1. Lectura Actual
        var lecturaActual = select(
                LECTURAS_EMBALSES.EMBALSE_ID,
                LECTURAS_EMBALSES.HM3_ACTUAL,
                LECTURAS_EMBALSES.PORCENTAJE,
                LECTURAS_EMBALSES.FECHA_REGISTRO,
                rowNumber().over(partitionBy(LECTURAS_EMBALSES.EMBALSE_ID).orderBy(LECTURAS_EMBALSES.FECHA_REGISTRO.desc())).as("rn")
        )
                .from(LECTURAS_EMBALSES)
                .asTable("curr");

        // 2. Lectura Pasada
        var lecturaPasada = select(
                LECTURAS_EMBALSES.EMBALSE_ID,
                LECTURAS_EMBALSES.HM3_ACTUAL,
                rowNumber().over(partitionBy(LECTURAS_EMBALSES.EMBALSE_ID).orderBy(LECTURAS_EMBALSES.FECHA_REGISTRO.desc())).as("rn")
        )
                .from(LECTURAS_EMBALSES)
                .where(LECTURAS_EMBALSES.FECHA_REGISTRO.le(field("NOW() - CAST({0} AS INTERVAL)", LocalDateTime.class, intervalo)))
                .asTable("prev");

        // 3. Consulta Final con ALIAS COINCIDENTES con el Record
        return dsl.select(
                        EMBALSES.ID.as("idEmbalse"), // Coincide con Record
                        EMBALSES.NOMBRE.as("nombre"), // Coincide con Record
                        lecturaActual.field(LECTURAS_EMBALSES.HM3_ACTUAL).coerce(Double.class).as("hm3"), // Coincide con Record
                        lecturaActual.field(LECTURAS_EMBALSES.PORCENTAJE).coerce(Double.class).as("porcentaje"), // Coincide con Record
                        EMBALSES.CAPACIDAD_MAXIMA.as("capacidadMaximaEmbalse"), // Coincide con Record

                        // Calculamos la variación y la renombramos
                        lecturaActual.field(LECTURAS_EMBALSES.HM3_ACTUAL)
                                .minus(coalesce(lecturaPasada.field(LECTURAS_EMBALSES.HM3_ACTUAL), lecturaActual.field(LECTURAS_EMBALSES.HM3_ACTUAL)))
                                .coerce(Double.class)
                                .as("variacion"), // Coincide con Record

                        // Valor por defecto para Tendencia (se puede calcular en el Record o Front)
                        val(TendenciaEnum.ESTABLE.name()).as("tendencia"),

                        lecturaActual.field(LECTURAS_EMBALSES.FECHA_REGISTRO).as("fechaRegistro") // Coincide con Record
                )
                .from(EMBALSES)
                .join(lecturaActual).on(EMBALSES.ID.eq(lecturaActual.field(LECTURAS_EMBALSES.EMBALSE_ID)))
                .leftJoin(lecturaPasada).on(EMBALSES.ID.eq(lecturaPasada.field(LECTURAS_EMBALSES.EMBALSE_ID)))
                .where(lecturaActual.field("rn", Integer.class).eq(1))
                .fetchInto(EmbalseDTO.class);
    }

    public void insertarValoresEnHistoricoCuencaSegura(double volumenActualCuenca, double porc, String nombreTabla) {
        dsl.insertInto(DSL.table(DSL.name(nombreTabla)))
                .columns(
                        DSL.field("volumen_total"),
                        DSL.field("porcentaje_total"),
                        DSL.field("fecha_registro")
                )
                .values(
                        volumenActualCuenca,
                        porc,
                        DSL.currentTimestamp() // Usa la función nativa de la BD
                )
                .execute();
    }

    public List<HistoricoCuencaDTO> getHistoricoCuencaSeguraList(String nombreTabla) {
        return dsl.select(
                        DSL.field("volumen_total", Double.class),
                        DSL.field("porcentaje_total", Double.class),
                        DSL.field("fecha_registro", Timestamp.class)
                )
                .from(DSL.table(DSL.name(nombreTabla))) // Uso de name() para evitar SQL Injection
                .orderBy(DSL.field("fecha_registro").asc())
                .fetchInto(HistoricoCuencaDTO.class); // Mapeo automático ultra rápido
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

    public List<EmbalseDTO> obtenerHistoricoEmbalsePorIdEmbalseOld(int idEmbalse) throws FunctionalExceptions {
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
                        TendenciaEnum tendencia = (tendenciaBD != null)
                                ? TendenciaEnum.valueOf(tendenciaBD.toUpperCase())
                                : TendenciaEnum.ESTABLE;

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

    public List<EmbalseDTO> obtenerHistoricoEmbalsePorIdEmbalse(int idEmbalse) {
        return dsl.select(
                        LECTURAS_EMBALSES.EMBALSE_ID,
                        EMBALSES.NOMBRE.as("nombre_embalse"),
                        LECTURAS_EMBALSES.HM3_ACTUAL,
                        LECTURAS_EMBALSES.PORCENTAJE,
                        EMBALSES.CAPACIDAD_MAXIMA,
                        LECTURAS_EMBALSES.VARIACION,
                        LECTURAS_EMBALSES.TENDENCIA,
                        LECTURAS_EMBALSES.FECHA_REGISTRO
                )
                .from(LECTURAS_EMBALSES)
                .join(EMBALSES).on(LECTURAS_EMBALSES.EMBALSE_ID.eq(EMBALSES.ID))
                .where(LECTURAS_EMBALSES.EMBALSE_ID.eq(idEmbalse))
                .orderBy(LECTURAS_EMBALSES.FECHA_REGISTRO.asc())
                .fetch(record -> {
                    // Mapeo personalizado para manejar el Enum de tendencia
                    String tendenciaStr = record.get(LECTURAS_EMBALSES.TENDENCIA);
                    TendenciaEnum tendencia = (tendenciaStr != null)
                            ? TendenciaEnum.valueOf(tendenciaStr.toUpperCase())
                            : TendenciaEnum.ESTABLE;

                    return new EmbalseDTO(
                            record.get(LECTURAS_EMBALSES.EMBALSE_ID),
                            record.get(EMBALSES.NOMBRE.as("nombre_embalse")),
                            record.get(LECTURAS_EMBALSES.HM3_ACTUAL, Double.class), // jOOQ lo convierte automáticamente
                            record.get(LECTURAS_EMBALSES.PORCENTAJE, Double.class),
                            record.get(EMBALSES.CAPACIDAD_MAXIMA, Double.class),
                            record.get(LECTURAS_EMBALSES.VARIACION, Double.class),
                            tendencia,
                            Timestamp.valueOf(record.get(LECTURAS_EMBALSES.FECHA_REGISTRO))
                    );
                });
    }

}
