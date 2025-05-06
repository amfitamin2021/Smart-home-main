package com.example.auth_service.security.services;

import com.example.auth_service.model.User;
import com.example.auth_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApiGatewayAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ApiGatewayAuthService apiGatewayAuthService;

    @Test
    @DisplayName("Извлечение ID пользователя из заголовка")
    public void testGetUserIdFromRequest() {
        // Arrange
        when(request.getHeader("X-Auth-User-ID")).thenReturn("123");

        // Act
        Long userId = apiGatewayAuthService.getUserIdFromRequest(request);

        // Assert
        assertEquals(123L, userId);
        verify(request, times(1)).getHeader("X-Auth-User-ID");
    }

    @Test
    @DisplayName("Возврат null при отсутствии заголовка")
    public void testGetUserIdFromRequestWithNoHeader() {
        // Arrange
        when(request.getHeader("X-Auth-User-ID")).thenReturn(null);

        // Act
        Long userId = apiGatewayAuthService.getUserIdFromRequest(request);

        // Assert
        assertNull(userId);
        verify(request, times(1)).getHeader("X-Auth-User-ID");
    }

    @Test
    @DisplayName("Возврат null при некорректном формате ID")
    public void testGetUserIdFromRequestWithInvalidFormat() {
        // Arrange
        when(request.getHeader("X-Auth-User-ID")).thenReturn("invalid-id");

        // Act
        Long userId = apiGatewayAuthService.getUserIdFromRequest(request);

        // Assert
        assertNull(userId);
        verify(request, times(1)).getHeader("X-Auth-User-ID");
    }

    @Test
    @DisplayName("Получение пользователя из репозитория по ID")
    public void testGetUserFromRequest() {
        // Arrange
        when(request.getHeader("X-Auth-User-ID")).thenReturn("123");
        
        User user = new User();
        user.setId(123L);
        user.setUsername("testuser");
        
        when(userRepository.findById(123L)).thenReturn(Optional.of(user));

        // Act
        User result = apiGatewayAuthService.getUserFromRequest(request);

        // Assert
        assertNotNull(result);
        assertEquals(123L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(request, times(1)).getHeader("X-Auth-User-ID");
        verify(userRepository, times(1)).findById(123L);
    }

    @Test
    @DisplayName("Возврат null при отсутствии пользователя в БД")
    public void testGetUserFromRequestWithNonExistentUser() {
        // Arrange
        when(request.getHeader("X-Auth-User-ID")).thenReturn("123");
        when(userRepository.findById(123L)).thenReturn(Optional.empty());

        // Act
        User result = apiGatewayAuthService.getUserFromRequest(request);

        // Assert
        assertNull(result);
        verify(request, times(1)).getHeader("X-Auth-User-ID");
        verify(userRepository, times(1)).findById(123L);
    }
} 