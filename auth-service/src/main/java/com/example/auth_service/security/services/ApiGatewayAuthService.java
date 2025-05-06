package com.example.auth_service.security.services;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiGatewayAuthService {

    private final UserRepository userRepository;
    
    /**
     * Получает ID пользователя из заголовка X-Auth-User-ID, добавленного API Gateway
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-Auth-User-ID");
        log.debug("Получен заголовок X-Auth-User-ID: {}", userIdHeader);
        
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            return null;
        }
        
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            log.error("Ошибка при преобразовании ID пользователя из заголовка: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Получает пользователя из БД по ID из заголовка
     */
    public User getUserFromRequest(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return null;
        }
        
        return userRepository.findById(userId).orElse(null);
    }
} 