package com.example.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        log.debug("Запрос на авторизацию: {}", path);
        
        // Проверяем наличие заголовка Authorization
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            log.debug("Заголовок Authorization отсутствует для пути: {}", path);
            return Mono.empty();
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("Некорректный формат заголовка Authorization: {}", authHeader);
            return Mono.empty();
        }

        // Извлекаем JWT токен
        String token = authHeader.substring(7);
        try {
            // Проверяем и парсим JWT токен
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
                log.warn("В токене не найдено ID пользователя");
                // Устанавливаем заглушку, чтобы позволить продолжить авторизацию
                // Вы можете настроить это поведение в соответствии с вашими требованиями
                userId = "anonymous";
            }
            
            log.debug("Извлечен ID пользователя из токена: {}", userId);
            
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            
            // Извлечение ролей из JWT токена
            try {
                List<String> roles = (List<String>) claims.get("roles");
                if (roles != null) {
                    authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.debug("Ошибка при получении ролей из токена: {}", e.getMessage());
            }

            log.debug("Успешная аутентификация пользователя с ID: {}, роли: {}", userId, authorities);

            // Добавляем информацию из токена в заголовок запроса
            ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                    .header("X-Auth-User-ID", userId)
                    .build();
            
            // Создаем новый обмен с модифицированным запросом
            ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
            
            // Заменяем оригинальный обмен модифицированным
            exchange = modifiedExchange;

            // Создаем объект аутентификации
            UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            return Mono.just(auth);
        } catch (Exception e) {
            log.error("Ошибка при обработке JWT токена: {} для пути: {}", e.getMessage(), path);
            return Mono.empty();
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
} 