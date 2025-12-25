package com.app.modules.weather.dao;

import com.app.core.config.DatabaseConfig;
import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.weather.dto.EstacionesAemetDTO;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class WeatherDAO {

    public void insertarEstacionesAemetFilterByProvincia(String provincia, List<EstacionesAemetDTO> estacionesAemetDTOListFilterByProvincia) throws FunctionalExceptions {

        try (Connection conn = DatabaseConfig.getConnection()) {
            for (EstacionesAemetDTO estacionesAemetDTO : estacionesAemetDTOListFilterByProvincia) {
                String sqlInsertarEstacionesFilteredByProvincia = "INSERT INTO estaciones_aemet(latitud, provincia, altitud, indicativo, nombre, indsinop, longitud )" +
                        "VALUES(?,?,?,?,?,?,?)";

                try (PreparedStatement psLectura = conn.prepareStatement(sqlInsertarEstacionesFilteredByProvincia)) {
                    psLectura.setString(1, estacionesAemetDTO.getLatitud());
                    psLectura.setString(2, estacionesAemetDTO.getProvincia());
                    psLectura.setLong(3, estacionesAemetDTO.getAltitud());
                    psLectura.setString(4, estacionesAemetDTO.getIndicativo());
                    psLectura.setString(5, estacionesAemetDTO.getNombre());
                    psLectura.setString(6, estacionesAemetDTO.getIndsinop());
                    psLectura.setString(7, estacionesAemetDTO.getLongitud());
                    psLectura.executeUpdate();
                }
            }
        } catch (SQLException e) {
            Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
        }
    }
}
