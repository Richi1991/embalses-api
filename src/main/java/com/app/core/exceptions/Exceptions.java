package com.app.core.exceptions;

import com.app.core.constantes.Constants;

public enum Exceptions {

    EMB_E_0001(Constants.EMB0001, "Ocurrió un error al cargar los datos de la web de la confederación hidrográfrica del Segura"),
    EMB_E_0002(Constants.EMB0002, "Ocurrió un error al llamar al job que despierta el servicio de back-end"),
    EMB_E_0003(Constants.EMB0003, "Ocurrió un error al llamar al servidor neon de bd"),
    EMB_E_0004(Constants.EMB0004, "Ocurrió un error al conectar con la base de datos"),
    EMB_E_0005(Constants.EMB0005, "Ocurrió un error al cargar los datos históricos diarios en la base de datos"),
    EMB_E_0006(Constants.EMB0006, "No existe el embalse %s"),
    EMB_E_0007(Constants.EMB0007, "Ha habido un error insertando las estaciones de la Aemet en BD"),
    EMB_E_0008(Constants.EMB0008, "Ha habido un error obteniendo las estaciones meteorologicas"),
    EMB_E_0009(Constants.EMB0009, "Ha habido un error obteniendo las estaciones meteorologicas y sus precipitaciones"),
    EMB_E_0010(Constants.EMB0010, "Error en la request %s"),
    EMB_E_0011(Constants.EMB0011, "Error en la response %s"),
    EMB_E_0012(Constants.EMB0012, "Ha habido un error obteniendo el top movimientos de los embalses %s"),
    EMB_E_0013(Constants.EMB0013, "Ha habido un error obteniendo el historico de la cuenca del segura de la tabla: %s"),
    EMB_E_0014(Constants.EMB0014, "Ha habido un error obteniendo el historico de embalse por id de embalse: %s"),
    EMB_E_0015(Constants.EMB0015, "Ha habido un error obteniendo el último valor de volumen de los embalses de la cuenca del segura y sus coordenadas: %s"),

    CAU_E_0001(Constants.CAU0001, "Ha habido un error obteniendo el último valor de caudales"),
    CAU_E_0002(Constants.CAU0002, "Ha habido un error obteniendo el último valor de caudales medios horarios: %s");

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
