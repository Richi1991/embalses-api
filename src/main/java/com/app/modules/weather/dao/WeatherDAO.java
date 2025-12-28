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
import java.sql.SQLException;
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
                    exito = true;
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

    public void manejarEspera(Long time) {
        try {
            Thread.sleep(time); // Espera X segundos antes de reintentar
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public List<EstacionesDTO> obtenerEstacionesMeteorologicas() throws FunctionalExceptions {
        int intentos = 0;
        boolean exito = false;
        List<EstacionesDTO> estacionesDTOList = new ArrayList<>();

        // 1. Usar un alias en SQL si los nombres de columna en Java difieren (opcional)
        String sqlSelect = "SELECT latitud, provincia, altitud, indicativo, nombre, indsinop, longitud, red_origen, geom FROM estaciones_meteorologicas";

        while (intentos < 3 && !exito) {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sqlSelect)) {

                // 2. Optimización de lectura para PostgreSQL
                ps.setFetchSize(100);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        estacionesDTOList.add(mapResultSetToDTO(rs));
                    }
                    exito = true; // Marcamos éxito para salir del bucle while
                }

            } catch (SQLException e) {
                intentos++;
                // Si es un error de autenticación o red, reintentamos
                if (intentos >= 3) {
                    Exceptions.EMB_E_0008.lanzarExcepcionCausada(e);
                }
                System.err.println("Intento " + intentos + " fallido. Reintentando...");
                manejarEspera(2000L); // Reducimos el tiempo de espera a 2s para mayor agilidad
            } catch (Exception e) {
                // Errores no relacionados con la DB (como NullPointer) no deberían reintentarse
                Exceptions.EMB_E_0008.lanzarExcepcionCausada(e);
                break;
            }
        }
        return estacionesDTOList;
    }

    private EstacionesDTO mapResultSetToDTO(ResultSet rs) throws SQLException {
        EstacionesDTO dto = new EstacionesDTO();
        dto.setLatitud(rs.getString("latitud"));
        dto.setProvincia(rs.getString("provincia"));
        dto.setAltitud(rs.getLong("altitud"));
        dto.setIndicativo(rs.getString("indicativo"));
        dto.setNombre(rs.getString("nombre"));
        dto.setIndsinop(rs.getString("indsinop"));
        dto.setLongitud(rs.getString("longitud"));
        dto.setRedOrigen(rs.getString("red_origen"));
        return dto;
    }
}
