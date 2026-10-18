package com.estacionamento.api.config;

import com.estacionamento.api.config.properties.HikvisionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HikvisionProperties.class)
public class HikvisionConfig {
}
