package com.example.auth_service.config;

import com.example.auth_service.model.ERole;
import com.example.auth_service.model.Role;
import com.example.auth_service.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    
    @Override
    public void run(String... args) {
        // Проверяем, есть ли уже роли в базе данных
        if (roleRepository.count() > 0) {
            log.info("Роли уже созданы в базе данных, пропускаем инициализацию");
            return;
        }
        
        log.info("Инициализация ролей...");
        
        // Создаем базовые роли
        for (ERole role : ERole.values()) {
            Role newRole = new Role(role);
            roleRepository.save(newRole);
            log.info("Создана роль: {}", role);
        }
        
        log.info("Инициализация ролей завершена");
    }
} 