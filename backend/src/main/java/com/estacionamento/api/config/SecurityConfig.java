package com.estacionamento.api.config;

import com.estacionamento.api.config.properties.JwtProperties;
import com.estacionamento.api.config.properties.SecurityHardeningProperties;
import com.estacionamento.api.web.filter.RedisRateLimitFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        if (properties.secretBase64() == null || properties.secretBase64().isBlank()) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be configured");
        }

        final byte[] secret;
        try {
            secret = Base64.getDecoder().decode(properties.secretBase64());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be valid Base64", exception);
        }

        if (secret.length < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 decoded bytes");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey, JwtProperties properties, com.estacionamento.api.security.PersistentSessionValidator sessions) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), sessions));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(com.estacionamento.api.security.PersistentSessionValidator sessions) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> sessions.current(jwt)
                .map(com.estacionamento.api.security.AuthenticatedUser::from)
                .map(user -> new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>(user.getAuthorities()))
                .orElseThrow(() -> new org.springframework.security.oauth2.server.resource.InvalidBearerTokenException("Session revoked")));
        converter.setPrincipalClaimName("username");
        return converter;
    }

    @Bean
    @org.springframework.core.annotation.Order(1)
    SecurityFilterChain managementSecurity(HttpSecurity http, @org.springframework.beans.factory.annotation.Value("${management.server.port:8081}") int port) throws Exception {
        http.securityMatcher(request -> request.getLocalPort() == port && request.getRequestURI().startsWith("/actuator/"))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/prometheus").permitAll().anyRequest().denyAll());
        return http.build();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            RedisRateLimitFilter rateLimitFilter,
            com.estacionamento.api.web.filter.SensitiveOperationAuditFilter auditFilter,
            SecurityHardeningProperties hardeningProperties
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(hardeningProperties)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(headers -> {
                    headers
                            .contentTypeOptions(Customizer.withDefaults())
                            .frameOptions(frame -> frame.deny())
                            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                            .permissionsPolicyHeader(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()"))
                            .cacheControl(Customizer.withDefaults())
                            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"));
                    if (hardeningProperties.hstsEnabled()) {
                        headers.httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000));
                    }
                })
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/integrations/hikvision/events").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterBefore(rateLimitFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
                .addFilterBefore(auditFilter, RedisRateLimitFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityHardeningProperties hardeningProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOrigins = java.util.Arrays.stream(hardeningProperties.corsAllowedOrigins().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<com.estacionamento.api.web.filter.SensitiveOperationAuditFilter> auditFilterRegistration(com.estacionamento.api.web.filter.SensitiveOperationAuditFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<RedisRateLimitFilter> rateLimitFilterRegistration(RedisRateLimitFilter filter) {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
