package com.example.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final List<String> excludedUrls = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/signin",
            "/api/auth/signup"
    );

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            
            log.debug("Обработка запроса: {}", path);

            // Проверяем, не является ли URL исключенным из проверки
            final Predicate<ServerHttpRequest> isSecured = r -> excludedUrls.stream()
                    .noneMatch(uri -> r.getURI().getPath().contains(uri));

            if (!isSecured.test(request)) {
                log.debug("URL исключен из проверки JWT: {}", path);
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey("Authorization")) {
                log.error("Отсутствует заголовок Authorization для URL: {}", path);
                return onError(exchange, "Отсутствует заголовок Authorization", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getOrEmpty("Authorization").get(0);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.error("Неверный формат токена для URL: {}", path);
                return onError(exchange, "Неверный формат токена", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // Проверка JWT
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(getSigningKey())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Получаем ID пользователя из токена, проверяя разные поля
                String userId = null;
                if (claims.get("id") != null) {
                    userId = String.valueOf(claims.get("id"));
                } else if (claims.getSubject() != null) {
                    userId = claims.getSubject();
                } else {
                    log.warn("В токене не найдено ID пользователя для URL: {}", path);
                    userId = "anonymous";
                }
                
                log.debug("ID пользователя из токена: {}, для URL: {}", userId, path);

                // Создаем новый запрос с добавленным заголовком
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-Auth-User-ID", userId)
                        .build();
                
                // Создаем новый обмен с модифицированным запросом
                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(modifiedRequest)
                        .build();

                log.debug("JWT аутентификация успешна для URL: {}", path);
                return chain.filter(modifiedExchange);
            } catch (Exception e) {
                log.error("Ошибка проверки JWT для URL {}: {}", path, e.getMessage());
                return onError(exchange, "Недействительный токен", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public static class Config {
        // Пустой класс конфигурации, требуется для AbstractGatewayFilterFactory
    }
} 