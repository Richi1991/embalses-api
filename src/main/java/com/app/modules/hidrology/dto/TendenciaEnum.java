package com.app.modules.hidrology.dto;

// En tu Enum
public enum TendenciaEnum {
    SUBIDA("subida"),
    BAJADA("bajada"),
    ESTABLE("estable");

    private final String valor;
    TendenciaEnum(String valor) { this.valor = valor; }
    public String getValor() { return valor; }
}
