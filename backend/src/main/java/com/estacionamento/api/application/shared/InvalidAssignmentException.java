package com.estacionamento.api.application.shared;

public class InvalidAssignmentException extends RuntimeException {
    public InvalidAssignmentException(String message) {
        super(message);
    }
}
