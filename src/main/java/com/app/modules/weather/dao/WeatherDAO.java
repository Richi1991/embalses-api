package com.app.modules.weather.dao;

import com.app.core.config.DatabaseConfig;
import com.app.core.constantes.Constants;
import com.app.modules.hidrology.exceptions.Exceptions;
import com.app.modules.hidrology.exceptions.FunctionalExceptions;
import com.app.modules.weather.dto.EstacionesDTO;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Repository
public class WeatherDAO {

    public void insertarEstacionesAemetFilterByProvincia(List<EstacionesDTO> estacionesAemetDTOListFilterByProvincia) throws FunctionalExceptions {

        int intentos = 0;
        boolean exito = false;

        String sqlInsertarEstacionesFilteredByProvincia = "INSERT INTO estaciones_meteorologicas(latitud, provincia, altitud, indicativo, nombre, indsinop, longitud, red_origen)" +
                "VALUES(?,?,?,?,?,?,?)";

        while (intentos < 3 && !exito) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                try (PreparedStatement psLectura = conn.prepareStatement(sqlInsertarEstacionesFilteredByProvincia)) {
                    for (EstacionesDTO estacionesAemetDTO : estacionesAemetDTOListFilterByProvincia) {
                        psLectura.setString(1, estacionesAemetDTO.getLatitud());
                        psLectura.setString(2, estacionesAemetDTO.getProvincia());
                        psLectura.setLong(3, estacionesAemetDTO.getAltitud());
                        psLectura.setString(4, estacionesAemetDTO.getIndicativo());
                        psLectura.setString(5, estacionesAemetDTO.getNombre());
                        psLectura.setString(6, estacionesAemetDTO.getIndsinop());
                        psLectura.setString(7, estacionesAemetDTO.getLongitud());
                        psLectura.setString(8, Constants.AEMET);
                        psLectura.executeUpdate();
                    }
                }
            } catch (Exception e) {
                intentos++;
                if (intentos >= 3) {
                    Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
                }
                manejarEspera(3000L);
            }
        }
    }

    public List<EstacionesDTO> buscarEstacionesPorProvincia(String provincia) throws FunctionalExceptions {

        int intentos = 0;
        boolean exito = false;

        List<EstacionesDTO> estacionesAemetDTOList = new ArrayList<>();

        String sqlSelectEstacionesByProvincia = "SELECT * FROM estaciones_aemet WHERE provincia =" + provincia;

        while (intentos < 3 && !exito) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                try (PreparedStatement psLectura = conn.prepareStatement(sqlSelectEstacionesByProvincia)) {
                    psLectura.setString(2, provincia);
                    try (ResultSet rs = psLectura.executeQuery()) {
                        while (rs.next()) {
                            estacionesAemetDTOList.add(new EstacionesDTO(
                                    rs.getString("latitud"),
                                    rs.getString("provincia"),
                                    rs.getLong("altitud"),
                                    rs.getString("indicativo"),
                                    rs.getString("nombre"),
                                    rs.getString("indsinop"),
                                    rs.getString("longitud")
                            ));
                        }
                    }
                }
            } catch (Exception e) {
                intentos++;
                if (intentos >= 3) {
                    Exceptions.EMB_E_0004.lanzarExcepcionCausada(e);
                }
                manejarEspera(4000L);
            }
        }
        return estacionesAemetDTOList;
    }

    public void manejarEspera(Long time) {
        try {
            Thread.sleep(time); // Espera X segundos antes de reintentar
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
