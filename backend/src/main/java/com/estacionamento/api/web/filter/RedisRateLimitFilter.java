package com.estacionamento.api.web.filter;

import com.estacionamento.api.config.properties.SecurityHardeningProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
public class RedisRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String HIKVISION_PATH = "/api/v1/integrations/hikvision/events";

    private static final DefaultRedisScript<Long> INCREMENT_AND_EXPIRE = new DefaultRedisScript<>(
            "local current = redis.call('INCR', KEYS[1]); " +
                    "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
                    "return current;",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final SecurityHardeningProperties properties;
    private final ClientIpResolver clientIpResolver;

    public RedisRateLimitFilter(StringRedisTemplate redisTemplate,
                                SecurityHardeningProperties properties,
                                ClientIpResolver clientIpResolver) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.rateLimitEnabled()) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(LOGIN_PATH.equals(path) || HIKVISION_PATH.equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        int limit = LOGIN_PATH.equals(path)
                ? properties.loginRequestsPerMinute()
                : properties.hikvisionRequestsPerMinute();
        String bucket = LOGIN_PATH.equals(path) ? "login" : "hikvision";
        String clientIp = clientIpResolver.resolve(request);
        String key = "garage:ratelimit:" + bucket + ":" + clientIp + ":" + (Instant.now().getEpochSecond() / 60);

        try {
            Long count = redisTemplate.execute(
                    INCREMENT_AND_EXPIRE,
                    List.of(key),
                    "61"
            );
            long current = count == null ? 0 : count;
            long remaining = Math.max(0, limit - current);
            response.setHeader("X-RateLimit-Limit", Integer.toString(limit));
            response.setHeader("X-RateLimit-Remaining", Long.toString(remaining));
            if (current > limit) {
                writeTooManyRequests(response);
                return;
            }
        } catch (RuntimeException exception) {
            if (!properties.rateLimitFailOpen()) {
                writeServiceUnavailable(response);
                return;
            }
            logger.warn("Redis rate limiter unavailable; request allowed because fail-open is enabled", exception);
        }

        filterChain.doFilter(request, response);
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many requests. Please try again later.\"}");
    }

    private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"Security rate limiter is unavailable.\"}");
    }
}
