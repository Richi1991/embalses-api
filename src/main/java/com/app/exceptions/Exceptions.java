package com.app.exceptions;

import com.app.constantes.Constants;

public enum Exceptions {

    EMB_E_0001(Constants.EMB0001, "Ocurrió un error al cargar los datos de la web de la confederación hidrográfrica del Segura"),
    EMB_E_0002(Constants.EMB0002, "Ocurrió un error al llamar al job que despierta el servicio de back-end"),
    EMB_E_0003(Constants.EMB0003, "Ocurrió un error al llamar al servidor neon de bd"),
    EMB_E_0004(Constants.EMB0004, "Ocurrió un error al conectar con la base de datos");

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
}
