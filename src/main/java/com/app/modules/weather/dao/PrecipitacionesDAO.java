package com.app.modules.weather.dao;

import com.app.core.config.DatabaseConfig;
import com.app.core.constantes.Constants;
import com.app.modules.hidrology.dto.EmbalseDTO;
import com.app.modules.hidrology.dto.TendenciaEnum;
import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.weather.dto.EstacionesDTO;
import com.app.modules.weather.dto.PrecipitacionesDTO;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PrecipitacionesDAO {

    public void insertarEstacionesChs(List<EstacionesDTO> estacionesDTOListChs) throws FunctionalExceptions {
        String sql = "INSERT INTO estaciones_meteorologicas(indicativo, nombre, red_origen) VALUES(?,?,?)";

        // 2. Abrir la conexión UNA SOLA VEZ fuera del bucle
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // Para enviar all en un bloque (eficiente)

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (EstacionesDTO dto : estacionesDTOListChs) {
                    // 3. Índices CORRECTOS: 1, 2 y 3
                    ps.setString(1, dto.getIndicativo());
                    ps.setString(2, dto.getNombre());
                    ps.setString(3, Constants.CHS);

                    ps.addBatch(); // Acumular
                }

                ps.executeBatch(); // Ejecutar all de una vez
                conn.commit();     // Guardar cambios
                System.out.println("Inserción terminada con éxito.");

            } catch (Exception e) {
                conn.rollback(); // Si falla una, no se guarda ninguna (mantiene la DB limpia)
                throw e;
            }
        } catch (Exception e) {
            System.err.println("Error de base de datos: " + e.getMessage());
            Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
        }
    }

    public void guardarValoresPrecipitaciones(List<EstacionesDTO> estacionesDTOListChs) throws FunctionalExceptions {
        String sql = "INSERT INTO precipitaciones(indicativo, nombre, precipitacion_1h, precipitacion_3h, precipitacion_6h, precipitacion_12h, precipitacion_24h, fecha_actualizacion) " +
                "VALUES(?,?,?,?,?,?,?, CURRENT_TIMESTAMP)";

        // 2. Abrir la conexión UNA SOLA VEZ fuera del bucle
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // Para enviar all en un bloque (eficiente)

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (EstacionesDTO dto : estacionesDTOListChs) {
                    // 3. Índices CORRECTOS: 1, 2 y 3
                    ps.setString(1, dto.getIndicativo());
                    ps.setString(2, dto.getNombre());
                    ps.setDouble(3, dto.getPrecipitacionesDTO().getPrecipitacion1h());
                    ps.setDouble(4, dto.getPrecipitacionesDTO().getPrecipitacion3h());
                    ps.setDouble(5, dto.getPrecipitacionesDTO().getPrecipitacion6h());
                    ps.setDouble(6, dto.getPrecipitacionesDTO().getPrecipitacion12h());
                    ps.setDouble(7, dto.getPrecipitacionesDTO().getPrecipitacion24h());

                    ps.addBatch(); // Acumular
                }

                ps.executeBatch(); // Ejecutar all de una vez
                conn.commit();     // Guardar cambios
                System.out.println("Inserción terminada con éxito.");

            } catch (Exception e) {
                conn.rollback(); // Si falla una, no se guarda ninguna (mantiene la DB limpia)
                throw e;
            }
        } catch (Exception e) {
            System.err.println("Error de base de datos: " + e.getMessage());
            Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
        }
    }

    public List<EstacionesDTO> getPrecipitacionesRealTime() {

        List<EstacionesDTO> estacionesAndPrecipitacionesDtoList = new ArrayList<>();

        String sqlGetPrecipitaciones = " SELECT DISTINCT ON (pre.indicativo) \n " +
                "  pre.indicativo, \n " +
                "  pre.nombre,\n " +
                "  pre.fecha_actualizacion, \n " +
                "  pre.precipitacion_1h, \n " +
                "  pre.precipitacion_3h, \n " +
                "  pre.precipitacion_6h,\n " +
                "  pre.precipitacion_12h,\n " +
                "  pre.precipitacion_24h,\n " +
                "  est.latitud,\n " +
                "  est.longitud,\n " +
                "  est.geom\n " +
                " FROM precipitaciones pre, estaciones_meteorologicas est\n " +
                " ORDER BY indicativo, fecha_actualizacion DESC; " ;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlGetPrecipitaciones)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    EstacionesDTO estacionesDTO = new EstacionesDTO();

                    PrecipitacionesDTO precipitacionesDTO = new PrecipitacionesDTO();

                    estacionesDTO.setIndicativo(rs.getString("indicativo"));
                    estacionesDTO.setNombre(rs.getString("nombre"));
                    estacionesDTO.setFechaActualizacion(rs.getTimestamp("fecha_actualizacion"));
                    estacionesDTO.setLatitud(rs.getString("latitud"));
                    estacionesDTO.setLongitud(rs.getString("longitud"));
                    estacionesDTO.setGeom(rs.getString("geom"));

                    precipitacionesDTO.setPrecipitacion1h(rs.getDouble("precipitacion_1h"));
                    precipitacionesDTO.setPrecipitacion3h(rs.getDouble("precipitacion_3h"));
                    precipitacionesDTO.setPrecipitacion6h(rs.getDouble("precipitacion_6h"));
                    precipitacionesDTO.setPrecipitacion12h(rs.getDouble("precipitacion_12h"));
                    precipitacionesDTO.setPrecipitacion24h(rs.getDouble("precipitacion_24h"));

                    estacionesDTO.setPrecipitacionesDTO(precipitacionesDTO);

                    estacionesAndPrecipitacionesDtoList.add(estacionesDTO);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return estacionesAndPrecipitacionesDtoList;
    }
}
