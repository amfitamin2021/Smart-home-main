package com.example.apigateway.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        log.info("Инициализация API Gateway маршрутов");
        
        return builder.routes()
                .route("device-commands", r -> r
                        .path("/api/devices/*/command")
                        .filters(f -> f
                                .removeRequestHeader("Cookie")
                                .preserveHostHeader()
                                .addRequestHeader("X-Forwarded-Prefix", "/api")
                                .rewriteResponseHeader("X-Auth-User-ID", ".*", "${headers.X-Auth-User-ID}"))
                        .uri("http://localhost:8086"))
                
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .removeRequestHeader("Cookie")
                                .preserveHostHeader())
                        .uri("http://localhost:8087"))
                
                // Маршрут для пользовательских эндпоинтов сервиса авторизации
                .route("auth-service-users", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .removeRequestHeader("Cookie")
                                .addRequestHeader("X-Forwarded-Prefix", "/api"))
                        .uri("http://localhost:8087"))
                
                // Маршрут для основного сервиса (аутентификация выполняется через JWT фильтр в SecurityConfig)
                .route("smarthome-service", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .removeRequestHeader("Cookie")
                                .preserveHostHeader()
                                .addRequestHeader("X-Forwarded-Prefix", "/api"))
                        .uri("http://localhost:8086"))
                .build();
    }
} 