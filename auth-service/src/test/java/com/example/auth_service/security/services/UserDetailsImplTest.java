package com.example.auth_service.security.services;

import com.example.auth_service.model.ERole;
import com.example.auth_service.model.Role;
import com.example.auth_service.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class UserDetailsImplTest {

    @Test
    public void testBuild() {
        // Создаем тестового пользователя с ролями
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        
        // Добавляем роль пользователя
        Set<Role> roles = new HashSet<>();
        Role userRole = new Role();
        userRole.setId(1);
        userRole.setName(ERole.ROLE_USER);
        roles.add(userRole);
        user.setRoles(roles);
        
        // Создаем UserDetailsImpl из пользователя
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        
        // Проверяем, что все поля корректно скопированы
        assertEquals(1L, userDetails.getId());
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("test@example.com", userDetails.getEmail());
        assertEquals("password123", userDetails.getPassword());
        
        // Проверяем, что роли правильно преобразованы в GrantedAuthority
        assertEquals(1, userDetails.getAuthorities().size());
        boolean hasUserRole = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_USER"));
        assertTrue(hasUserRole);
        
        // Проверяем другие методы
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }
    
    @Test
    public void testEquals() {
        // Создаем два объекта UserDetailsImpl с одинаковым ID
        UserDetailsImpl user1 = new UserDetailsImpl(
                1L, "user1", "user1@example.com", "password", null);
        
        UserDetailsImpl user2 = new UserDetailsImpl(
                1L, "user2", "user2@example.com", "diffpassword", null);
        
        // Проверяем, что equals сравнивает только по ID
        assertEquals(user1, user2);
        
        // Создаем объект с другим ID
        UserDetailsImpl user3 = new UserDetailsImpl(
                2L, "user1", "user1@example.com", "password", null);
        
        // Проверяем, что объекты с разными ID не равны
        assertNotEquals(user1, user3);
    }
} 