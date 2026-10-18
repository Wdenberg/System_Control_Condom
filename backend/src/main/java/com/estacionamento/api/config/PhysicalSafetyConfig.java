package com.estacionamento.api.config;
import com.estacionamento.api.config.properties.GateProperties;
import com.estacionamento.api.config.properties.WebhookCredentials;
import com.estacionamento.api.domain.camera.CameraRepository;
import org.springframework.context.annotation.*;
import org.springframework.boot.ApplicationRunner;
@Configuration
public class PhysicalSafetyConfig {
    @Bean ApplicationRunner physicalSafety(GateProperties gate, WebhookCredentials credentials, CameraRepository cameras,
            @org.springframework.beans.factory.annotation.Value("${GATE_HARDWARE_APPROVED:false}") boolean approved,
            @org.springframework.beans.factory.annotation.Value("${APP_ENVIRONMENT:local}") String environment,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.password}") String databasePassword) {
        return args -> {
            if(gate.enabled() && !"SIMULATED".equalsIgnoreCase(gate.driver()) && !approved)
                throw new IllegalStateException("Physical gate automation requires explicit operational commissioning approval");
            if("production".equalsIgnoreCase(environment)) {
                if(databasePassword==null || databasePassword.isBlank() || databasePassword.equals("change-me") || databasePassword.startsWith("CHANGE_ME")) throw new IllegalStateException("Production database credentials must be provisioned");
                for(var camera:cameras.findAllByOrderByNameAsc()) {
                    String secret=credentials.tokens().get(camera.getName().toUpperCase(java.util.Locale.ROOT));
                    if(camera.isEnabled() && (secret==null || secret.isBlank())) throw new IllegalStateException("Every enabled camera requires a provisioned webhook credential");
                }
            }
        };
    }
}
