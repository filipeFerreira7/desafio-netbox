package com.policiafederal.descobre_ip.infra.exception;

public class ValidacaoException extends RuntimeException {
    private String fieldName;

    public ValidacaoException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
