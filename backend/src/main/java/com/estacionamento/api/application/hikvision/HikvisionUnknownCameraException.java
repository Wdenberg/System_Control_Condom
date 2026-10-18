package com.estacionamento.api.application.hikvision;

public class HikvisionUnknownCameraException extends RuntimeException {

    public HikvisionUnknownCameraException(String sourceIp) {
        super("Hikvision camera is not registered or disabled: " + sourceIp);
    }
}
