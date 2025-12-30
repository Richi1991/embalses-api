package com.app.modules.weather.dto;

import lombok.ToString;

import java.util.Date;
@ToString
public class TemperaturasDTO {

    private Double tmed;
    private Double tmin;
    private Double tmax;
    private Date horaTmin;
    private Date horaTmax;
    private Date fechaRegistro;

    public TemperaturasDTO(){
    }

    public Double getTmed() {
        return tmed;
    }

    public void setTmed(Double tmed) {
        this.tmed = tmed;
    }

    public Double getTmin() {
        return tmin;
    }

    public void setTmin(Double tmin) {
        this.tmin = tmin;
    }

    public Double getTmax() {
        return tmax;
    }

    public void setTmax(Double tmax) {
        this.tmax = tmax;
    }

    public Date getHoraTmin() {
        return horaTmin;
    }

    public void setHoraTmin(Date horaTmin) {
        this.horaTmin = horaTmin;
    }

    public Date getHoraTmax() {
        return horaTmax;
    }

    public void setHoraTmax(Date horaTmax) {
        this.horaTmax = horaTmax;
    }

    public Date getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(Date fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
