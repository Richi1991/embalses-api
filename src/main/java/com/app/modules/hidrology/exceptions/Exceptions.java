package com.app.modules.hidrology.exceptions;

import com.app.core.constantes.Constants;

public enum Exceptions {

    EMB_E_0001(Constants.EMB0001, "Ocurrió un error al cargar los datos de la web de la confederación hidrográfrica del Segura"),
    EMB_E_0002(Constants.EMB0002, "Ocurrió un error al llamar al job que despierta el servicio de back-end"),
    EMB_E_0003(Constants.EMB0003, "Ocurrió un error al llamar al servidor neon de bd"),
    EMB_E_0004(Constants.EMB0004, "Ocurrió un error al conectar con la base de datos"),
    EMB_E_0005(Constants.EMB0005, "Ocurrió un error al cargar los datos históricos diarios en la base de datos"),
    EMB_E_0006(Constants.EMB0006, "No existe el embalse %s"),
    EMB_E_0007(Constants.EMB0007, "Ha habido un error insertando las estaciones de la Aemet en BD");

    private String codigoError;
    private String descripcionError;

    public String getCodigoError() {
        return codigoError;
    }

    public String getDescripcionError() {
        return descripcionError;
    }

    Exceptions(String codigoError, String descripcionError) {
        this.codigoError = codigoError;
        this.descripcionError = descripcionError;
    }

    public void lanzarExcepcionCausada(Throwable cause) throws FunctionalExceptions {
        throw new FunctionalExceptions(this.codigoError.concat(": ").concat(this.descripcionError), cause);
    }

    public void lanzarExcepcionWithParams(String parametro) throws FunctionalExceptions {
        throw new FunctionalExceptions(this.codigoError.concat(": ").concat(this.descripcionError).concat(parametro));
    }

}
