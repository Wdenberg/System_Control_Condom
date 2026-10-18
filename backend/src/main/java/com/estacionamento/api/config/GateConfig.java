package com.estacionamento.api.config;

import com.estacionamento.api.config.properties.GateProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(GateProperties.class)
public class GateConfig {
}
