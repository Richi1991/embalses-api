package com.app.dto;

import java.sql.Timestamp;

public record HistoricoCuencaDTO(

    double volumenTotal,
    double porcentajeTotal,
    Timestamp fechaRegistro

){}
