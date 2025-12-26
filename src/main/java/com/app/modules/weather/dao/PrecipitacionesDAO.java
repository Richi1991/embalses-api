package com.app.modules.weather.dao;

import com.app.core.config.DatabaseConfig;
import com.app.core.constantes.Constants;
import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.weather.dto.EstacionesDTO;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

@Component
public class PrecipitacionesDAO {

    public void insertarEstacionesChs(List<EstacionesDTO> estacionesDTOListChs) throws FunctionalExceptions {
        String sql = "INSERT INTO estaciones_meteorologicas(indicativo, nombre, red_origen) VALUES(?,?,?)";

        // 2. Abrir la conexión UNA SOLA VEZ fuera del bucle
        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false); // Para enviar todo en un bloque (eficiente)

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (EstacionesDTO dto : estacionesDTOListChs) {
                    // 3. Índices CORRECTOS: 1, 2 y 3
                    ps.setString(1, dto.getIndicativo());
                    ps.setString(2, dto.getNombre());
                    ps.setString(3, Constants.CHS);

                    ps.addBatch(); // Acumular
                }

                ps.executeBatch(); // Ejecutar todo de una vez
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
