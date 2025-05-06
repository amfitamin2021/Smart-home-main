package com.example.Smarthome.service;

import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Простой тест для проверки работы ThingsBoardIntegrationService
 */
@ExtendWith(MockitoExtension.class)
public class ThingsBoardIntegrationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private DeviceRepository deviceRepository;
    
    @Mock
    private DeviceService deviceService;

    @Spy
    @InjectMocks
    private ThingsBoardIntegrationService thingsBoardService;

    private Device testDevice;

    @BeforeEach
    public void setup() {
        // Устанавливаем значения для полей с аннотацией @Value
        ReflectionTestUtils.setField(thingsBoardService, "thingsBoardUrl", "http://localhost:9090");
        ReflectionTestUtils.setField(thingsBoardService, "thingsBoardUsername", "tenant@thingsboard.org");
        ReflectionTestUtils.setField(thingsBoardService, "thingsBoardPassword", "tenant");
        ReflectionTestUtils.setField(thingsBoardService, "accessToken", "test-access-token");

        // Создаем тестовое устройство
        testDevice = new Device();
        testDevice.setId(UUID.randomUUID());
        testDevice.setName("Тестовое устройство");
        testDevice.setType("SENSOR");
        testDevice.setStatus(DeviceStatus.ONLINE);
        testDevice.setProtocol(ConnectionProtocol.MQTT);
        testDevice.setLastSeen(LocalDateTime.now());
        testDevice.setProperties(new HashMap<>());
        testDevice.getProperties().put("temperature", "22");
        testDevice.getProperties().put("humidity", "45");
        testDevice.setThingsboardToken("test-token");
        testDevice.setThingsboardDeviceId("test-device-id");
    }

    @Test
    @DisplayName("Тест получения токена устройства по его имени")
    public void testGetTokenByDeviceName() {
        // Создаем мок ответа от ThingsBoard API
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("data", Collections.singletonList(createDeviceMap()));
        
        // Мокируем вызов ensureAuthenticated
        doReturn(true).when(thingsBoardService).ensureAuthenticated();
        
        // Мокируем вызов REST API
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseMap, HttpStatus.OK);
        when(restTemplate.exchange(
                contains("/api/tenant/devices"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);
        
        // Мокируем дополнительные вызовы, необходимые для метода
        doReturn("test-token").when(thingsBoardService).getTokenByDeviceId(anyString());
        
        // Вызываем тестируемый метод
        String result = thingsBoardService.getTokenByDeviceName("Тестовое устройство");
        
        // Проверяем результат
        assertEquals("test-token", result);
    }
    
    /**
     * Вспомогательный метод для создания моковых данных устройства из ThingsBoard
     */
    private Map<String, Object> createDeviceMap() {
        Map<String, Object> deviceMap = new HashMap<>();
        
        // Создаем ID объект
        Map<String, Object> idMap = new HashMap<>();
        idMap.put("id", "test-device-id");
        idMap.put("entityType", "DEVICE");
        
        deviceMap.put("id", idMap);
        deviceMap.put("name", "Тестовое устройство");
        deviceMap.put("type", "SENSOR");
        
        return deviceMap;
    }
} 