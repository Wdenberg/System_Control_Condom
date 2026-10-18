package com.estacionamento.api.application.hikvision;

import com.estacionamento.api.config.properties.HikvisionProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class HikvisionCooldownService {

    private final StringRedisTemplate redis;
    private final HikvisionProperties properties;

    public HikvisionCooldownService(StringRedisTemplate redis, HikvisionProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public boolean acquire(String cameraIp, String plate) {
        String key = "lpr:cooldown:" + cameraIp + ":" + plate;
        Boolean inserted = redis.opsForValue().setIfAbsent(key, "1", properties.cooldown());
        return Boolean.TRUE.equals(inserted);
    }
}
