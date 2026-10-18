package com.estacionamento.api.application.audit;

public final class AuditRequestContext {

    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private AuditRequestContext() {
    }

    public static void set(Context context) {
        CURRENT.set(context);
    }

    public static Context current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Context(
            String correlationId,
            String method,
            String path,
            String ipAddress,
            String userAgent
    ) {
    }
}
