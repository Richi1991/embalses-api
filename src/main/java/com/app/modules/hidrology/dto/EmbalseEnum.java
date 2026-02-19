package com.app.modules.hidrology.dto;

import java.util.Arrays;

public enum EmbalseEnum {

    FUENSANTA(1, "FUENSANTA"),
    CENAJO(2, "CENAJO"),
    TALAVE(3, "TALAVE"),
    CAMARILLAS(4, "CAMARILLAS"),
    BOQUERON(5, "BOQUERÓN"),
    CHARCOS(6, "CHARCOS"),
    BAYCO(7, "BAYCO"),
    LA_RISCA(8, "LA RISCA"),
    MORATALLA(9, "MORATALLA"),
    ARGOS(10, "ARGOS"),
    ALFONSO_XIII(11, "ALFONSO XIII"),
    JUDIO(12, "JUDÍO"),
    MORO(13, "MORO"),
    CARCABO(14, "CÁRCABO"),
    LA_CIERVA(15, "LA CIERVA"),
    PLIEGO(16, "PLIEGO"),
    DONA_ANA(17, "DOÑA ANA"),
    LOS_RODEOS(18, "LOS RODEOS"),
    MAYES(19, "MAYÉS"),
    SANTOMERA(20, "SANTOMERA"),
    VALDEINFIERNO(21, "VALDEINFIERNO"),
    PUENTES(22, "PUENTES"),
    ALGECIRAS(23, "ALGECIRAS"),
    JOSE_BAUTISTA(24, "JOSÉ BAUTISTA"),
    LA_PEDRERA(25, "LA PEDRERA"),
    CREVILLENTE(26, "CREVILLENTE");

    private final Integer codigoEmbalse;
    private final String nombreEmbalse;

    EmbalseEnum(Integer codigoEmbalse, String nombreEmbalse) {
        this.codigoEmbalse = codigoEmbalse;
        this.nombreEmbalse = nombreEmbalse;
    }

    public static int resolverId(String nombreWeb) {
        String nombreLimpio = nombreWeb.toUpperCase().trim();

        return Arrays.stream(EmbalseEnum.values())
                .filter(e -> nombreLimpio.contains(e.nombreEmbalse.toUpperCase())
                        || e.nombreEmbalse.toUpperCase().contains(nombreLimpio))
                .map(e -> e.codigoEmbalse)
                .findFirst()
                .orElse(0); // Devolvemos 0 si no hay match para que el 'continue' del service funcione
    }

    public Integer getCodigoEmbalse() {
        return codigoEmbalse;
    }

    public String getNombreEmbalse() {
        return nombreEmbalse;
    }
}
