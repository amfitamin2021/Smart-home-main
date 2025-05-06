package com.example.auth_service.controller;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.services.ApiGatewayAuthService;
import com.example.auth_service.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserRepository userRepository;
    private final ApiGatewayAuthService apiGatewayAuthService;
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        // Сначала пробуем получить пользователя из заголовка X-Auth-User-ID
        User user = apiGatewayAuthService.getUserFromRequest(request);
        
        // Если не получилось, пробуем получить из текущего контекста безопасности
        if (user == null) {
            log.debug("Не удалось получить пользователя из заголовка, пробуем из контекста безопасности");
            try {
                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();
                
                return userRepository.findById(userDetails.getId())
                        .map(u -> ResponseEntity.ok(u))
                        .orElse(ResponseEntity.notFound().build());
            } catch (Exception e) {
                log.error("Ошибка при получении пользователя из контекста безопасности: {}", e.getMessage());
                return ResponseEntity.status(401).body("Пользователь не аутентифицирован");
            }
        }
        
        log.debug("Успешно получен пользователь из заголовка X-Auth-User-ID: {}", user.getId());
        return ResponseEntity.ok(user);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        log.debug("Запрос на получение всех пользователей");
        return ResponseEntity.ok(userRepository.findAll());
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#id)")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        log.debug("Запрос на получение пользователя с ID: {}", id);
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(user))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        log.debug("Запрос на удаление пользователя с ID: {}", id);
        
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
} 