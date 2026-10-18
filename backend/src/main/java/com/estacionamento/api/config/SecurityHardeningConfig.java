package com.estacionamento.api.config;

import com.estacionamento.api.config.properties.SecurityHardeningProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SecurityHardeningProperties.class)
public class SecurityHardeningConfig {
}
