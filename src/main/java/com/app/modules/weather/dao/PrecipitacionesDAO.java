package com.app.modules.weather.dao;

import com.app.core.config.DatabaseConfig;
import com.app.core.constantes.Constants;
import com.app.core.exceptions.Exceptions;
import com.app.core.exceptions.FunctionalExceptions;
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

}
