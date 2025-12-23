package com.app.exceptions;

import javax.lang.model.type.ErrorType;
import java.util.Map;

public class FunctionalExceptions extends Exception {

    private static final long serialVersionUID = 1L;

    public FunctionalExceptions(String descripcionError, Throwable cause) {
        super(descripcionError, cause);
    }


    public FunctionalExceptions(String descripcionError) {
        super(descripcionError);
    }


}
