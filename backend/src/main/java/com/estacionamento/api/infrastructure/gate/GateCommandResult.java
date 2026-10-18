package com.estacionamento.api.infrastructure.gate;
public record GateCommandResult(boolean success, String message, boolean uncertain) {
    public GateCommandResult(boolean success, String message) { this(success, message, false); }
    public static GateCommandResult success(String message) { return new GateCommandResult(true, message, false); }
    public static GateCommandResult failure(String message) { return new GateCommandResult(false, message, false); }
    public static GateCommandResult unknown(String message) { return new GateCommandResult(false, message, true); }
}
