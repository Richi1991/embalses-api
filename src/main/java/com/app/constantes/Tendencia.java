package com.app.constantes;

// En tu Enum
public enum Tendencia {
    SUBIDA("subida"),
    BAJADA("bajada"),
    ESTABLE("estable");

    private final String valor;
    Tendencia(String valor) { this.valor = valor; }
    public String getValor() { return valor; }
}
