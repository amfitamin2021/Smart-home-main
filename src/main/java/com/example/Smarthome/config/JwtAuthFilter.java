package com.example.Smarthome.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Проверяем заголовок X-Auth-User-ID от API Gateway
        String userId = request.getHeader("X-Auth-User-ID");
        if (userId != null && !userId.isEmpty()) {
            log.debug("Найден заголовок X-Auth-User-ID: {}", userId);
            setupAuthenticationFromUserId(userId, request);
            filterChain.doFilter(request, response);
            return;
        }
        
        // Проверяем JWT токен в заголовке Authorization
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUsernameFromJwtToken(jwt);
                log.debug("Успешная валидация JWT для пользователя: {}", username);
                
                setupAuthenticationFromUsername(username, request);
            }
        } catch (Exception e) {
            log.error("Не удалось установить аутентификацию пользователя: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void setupAuthenticationFromUserId(String userId, HttpServletRequest request) {
        UserDetails userDetails = new User(userId, "", new ArrayList<>());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Аутентификация установлена для пользователя через X-Auth-User-ID: {}", userId);
    }
    
    private void setupAuthenticationFromUsername(String username, HttpServletRequest request) {
        UserDetails userDetails = new User(username, "", new ArrayList<>());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Аутентификация установлена для пользователя через JWT: {}", username);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
} 