package com.estacionamento.api.web.hikvision;

public class HikvisionWebhookUnauthorizedException extends RuntimeException {

    public HikvisionWebhookUnauthorizedException(String message) {
        super(message);
    }
}
