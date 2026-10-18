package com.estacionamento.api.config;

import com.estacionamento.api.config.properties.DashboardProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DashboardProperties.class)
public class DashboardConfig {
}
