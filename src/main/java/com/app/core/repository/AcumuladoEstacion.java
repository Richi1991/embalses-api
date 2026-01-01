package com.app.core.repository;

public interface AcumuladoEstacion {
    String getIndicativo();
    String getNombre();
    Double getValor_acumulado(); // Debe coincidir con el alias del SQL
    Double getLng();
    Double getLat();
}