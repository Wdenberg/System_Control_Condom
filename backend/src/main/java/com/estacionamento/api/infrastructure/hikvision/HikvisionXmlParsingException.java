package com.estacionamento.api.infrastructure.hikvision;

public class HikvisionXmlParsingException extends RuntimeException {

    public HikvisionXmlParsingException(String message) {
        super(message);
    }

    public HikvisionXmlParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
