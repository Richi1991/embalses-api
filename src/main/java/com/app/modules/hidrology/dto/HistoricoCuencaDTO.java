package com.app.modules.hidrology.dto;

import java.sql.Timestamp;

public record HistoricoCuencaDTO(

    double volumenTotal,
    double porcentajeTotal,
    Timestamp fechaRegistro

){}
