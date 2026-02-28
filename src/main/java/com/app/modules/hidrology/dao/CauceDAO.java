package com.app.modules.hidrology.dao;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CauceDAO {

    @Autowired
    private DSLContext dsl;

    public List<CauceDAO> getCaudalesRealTime() {
        return null;
    }
}
