package com.example.Smarthome.service;

import com.example.Smarthome.dto.DeviceEventDto;
import com.example.Smarthome.dto.SensorHistoryDto;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class DeviceEventHandler {

    private final DeviceRepository deviceRepository;
    private final SensorHistoryService sensorHistoryService;
    private final LockHistoryService lockHistoryService;
    private final ThingsBoardIntegrationService thingsBoardService;
    
    // Используем конструктор с @Lazy вместо @RequiredArgsConstructor
    public DeviceEventHandler(
            DeviceRepository deviceRepository,
            SensorHistoryService sensorHistoryService,
            LockHistoryService lockHistoryService,
            @Lazy ThingsBoardIntegrationService thingsBoardService) {
        this.deviceRepository = deviceRepository;
        this.sensorHistoryService = sensorHistoryService;
        this.lockHistoryService = lockHistoryService;
        this.thingsBoardService = thingsBoardService;
    }

    /**
     * Обрабатывает событие от устройства
     * @param event объект события
     */
    public void handleDeviceEvent(DeviceEventDto event) {
        try {
            log.debug("Обработка события устройства: {}", event);
            
            // Получаем устройство по ID
            UUID deviceId = UUID.fromString(event.getDeviceId());
            Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
            
            if (deviceOpt.isEmpty()) {
                log.warn("Устройство с ID {} не найдено при обработке события", deviceId);
                return;
            }
            
            Device device = deviceOpt.get();
            Map<String, String> attributes = event.getAttributes();
            
            log.info("Обработка события для устройства: {} (тип: {}, подтип: {})", 
                    device.getName(), device.getType(), device.getSubType());
            log.info("Атрибуты события: {}", attributes);
            
            // Обрабатываем события в зависимости от типа устройства
            switch (device.getType().toLowerCase()) {
                case "sensor":
                    handleSensorEvent(device, attributes);
                    break;
                case "lock":
                    handleLockEvent(device, attributes);
                    break;
                default:
                    log.debug("События для устройства типа {} не требуют специальной обработки", device.getType());
                    break;
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке события устройства: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Обрабатывает событие от датчика
     * @param device устройство
     * @param attributes атрибуты события
     */
    private void handleSensorEvent(Device device, Map<String, String> attributes) {
        try {
            // Определяем тип датчика
            String sensorType = determineSensorType(device);
            if (sensorType == null) {
                log.warn("Не удалось определить тип датчика для устройства: {}", device.getId());
                return;
            }
            
            log.info("Обработка события для датчика типа: {}", sensorType);
            
            // Проверяем, есть ли событие, которое нужно записать в историю
            boolean shouldAddToHistory = false;
            String value = null;
            String priority = "normal";
            boolean shouldResetState = false;
            
            switch (sensorType) {
                case "motion":
                    if (attributes.containsKey("motion") || attributes.containsKey("tb_motion")) {
                        value = attributes.getOrDefault("motion", attributes.getOrDefault("tb_motion", "false"));
                        shouldAddToHistory = "true".equals(value);
                        priority = shouldAddToHistory ? "medium" : "low";
                        // Если обнаружено движение, нужно после записи сбросить состояние датчика
                        shouldResetState = shouldAddToHistory;
                    }
                    break;
                case "contact":
                    if (attributes.containsKey("contact") || attributes.containsKey("tb_contact")) {
                        value = attributes.getOrDefault("contact", attributes.getOrDefault("tb_contact", "closed"));
                        shouldAddToHistory = "open".equals(value);
                        priority = shouldAddToHistory ? "medium" : "low";
                    }
                    break;
                case "smoke":
                    if (attributes.containsKey("smoke") || attributes.containsKey("tb_smoke")) {
                        value = attributes.getOrDefault("smoke", attributes.getOrDefault("tb_smoke", "false"));
                        shouldAddToHistory = "true".equals(value);
                        priority = shouldAddToHistory ? "critical" : "low";
                    }
                    break;
                case "leak":
                    if (attributes.containsKey("leak") || attributes.containsKey("tb_leak")) {
                        value = attributes.getOrDefault("leak", attributes.getOrDefault("tb_leak", "false"));
                        shouldAddToHistory = "true".equals(value);
                        priority = shouldAddToHistory ? "critical" : "low";
                    }
                    break;
                default:
                    log.debug("Неизвестный тип датчика: {}", sensorType);
                    break;
            }
            
            log.info("Анализ события: value={}, shouldAddToHistory={}, shouldResetState={}", 
                    value, shouldAddToHistory, shouldResetState);
            
            // Если есть событие для записи в историю
            if (shouldAddToHistory && value != null) {
                // Создаем запись для истории
                SensorHistoryDto historyDto = new SensorHistoryDto();
                historyDto.setDeviceId(device.getId().toString());
                historyDto.setDeviceName(device.getName());
                historyDto.setRoom(device.getRoom() != null ? device.getRoom().getName() : "");
                historyDto.setSensorType(sensorType);
                historyDto.setValue(value);
                historyDto.setPriority(priority);
                historyDto.setAcknowledged(false);
                
                // Устанавливаем сообщение в зависимости от типа датчика и значения
                historyDto.setMessage(getSensorMessage(sensorType, value));
                
                // Добавляем запись в историю
                SensorHistoryDto savedEntry = sensorHistoryService.addSensorHistoryEntry(device.getId(), historyDto);
                log.info("Добавлена запись в историю датчика {} ({}): {}", device.getName(), sensorType, historyDto.getMessage());
                
                // Если нужно сбросить состояние датчика (например, для датчиков движения)
                if (shouldResetState) {
                    resetSensorState(device, sensorType);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке события датчика: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Сбрасывает состояние датчика после регистрации события
     * @param device устройство
     * @param sensorType тип датчика
     */
    private void resetSensorState(Device device, String sensorType) {
        try {
            log.info("Сброс состояния датчика {} ({})", device.getName(), sensorType);
            
            // Подготавливаем атрибуты для сброса в зависимости от типа датчика
            Map<String, String> resetAttributes = new HashMap<>();
            
            switch (sensorType) {
                case "motion":
                    resetAttributes.put("tb_motion", "false");
                    break;
                // Можно добавить другие типы датчиков по необходимости
                default:
                    log.debug("Сброс состояния не требуется для датчика типа: {}", sensorType);
                    return;
            }
            
            // Обновляем свойства устройства локально
            Map<String, String> properties = device.getProperties();
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.putAll(resetAttributes);
            device.setProperties(properties);
            
            // Сохраняем обновленное устройство
            deviceRepository.save(device);
            
            // Обновляем атрибуты в ThingsBoard
            thingsBoardService.updateClientAttributes(device, resetAttributes);
            
            log.info("Состояние датчика {} успешно сброшено", device.getName());
        } catch (Exception e) {
            log.error("Ошибка при сбросе состояния датчика {}: {}", device.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Обрабатывает событие от замка
     * @param device устройство
     * @param attributes атрибуты события
     */
    private void handleLockEvent(Device device, Map<String, String> attributes) {
        // Логика обработки событий от замков, если требуется
        // ...
    }
    
    /**
     * Определяет тип датчика на основе его свойств
     * @param device устройство
     * @return тип датчика или null, если не удалось определить
     */
    private String determineSensorType(Device device) {
        // Сначала проверяем subType
        if (device.getSubType() != null) {
            switch (device.getSubType().toUpperCase()) {
                case "MOTION_SENSOR":
                    return "motion";
                case "CONTACT_SENSOR":
                    return "contact";
                case "SMOKE_SENSOR":
                    return "smoke";
                case "LEAK_SENSOR":
                    return "leak";
            }
        }
        
        // Если не удалось определить по subType, проверяем category
        if (device.getCategory() != null && "SECURITY".equals(device.getCategory())) {
            // Если категория безопасность, пробуем определить по свойствам
            Map<String, String> props = device.getProperties();
            if (props != null) {
                // Проверяем свойство tb_sensorType
                if (props.containsKey("tb_sensorType")) {
                    return props.get("tb_sensorType");
                }
                
                // Проверяем наличие специфичных свойств
                if (props.containsKey("tb_motion") || props.containsKey("motion")) {
                    return "motion";
                }
                if (props.containsKey("tb_contact") || props.containsKey("contact")) {
                    return "contact";
                }
                if (props.containsKey("tb_smoke") || props.containsKey("smoke")) {
                    return "smoke";
                }
                if (props.containsKey("tb_leak") || props.containsKey("leak")) {
                    return "leak";
                }
            }
        }
        
        // Если тип устройства sensor, но не смогли определить подтип,
        // проверяем свойства в любом случае
        if ("sensor".equalsIgnoreCase(device.getType())) {
            Map<String, String> props = device.getProperties();
            if (props != null) {
                if (props.containsKey("tb_sensorType")) {
                    return props.get("tb_sensorType");
                }
                if (props.containsKey("tb_motion") || props.containsKey("motion")) {
                    return "motion";
                }
                if (props.containsKey("tb_contact") || props.containsKey("contact")) {
                    return "contact";
                }
                if (props.containsKey("tb_smoke") || props.containsKey("smoke")) {
                    return "smoke";
                }
                if (props.containsKey("tb_leak") || props.containsKey("leak")) {
                    return "leak";
                }
            }
        }
        
        // Если ничего не помогло, логируем подробную информацию
        log.warn("Не удалось определить тип датчика. Устройство: ID={}, name={}, type={}, subType={}, properties={}", 
                device.getId(), device.getName(), device.getType(), device.getSubType(), device.getProperties());
        
        return null;
    }
    
    /**
     * Формирует сообщение в зависимости от типа датчика и значения
     * @param sensorType тип датчика
     * @param value значение
     * @return сообщение для истории
     */
    private String getSensorMessage(String sensorType, String value) {
        switch (sensorType) {
            case "motion":
                return "true".equals(value) ? "Обнаружено движение" : "Движение прекратилось";
            case "contact":
                return "open".equals(value) ? "Дверь/окно открыто" : "Дверь/окно закрыто";
            case "smoke":
                return "true".equals(value) ? "Обнаружен дым!" : "Дым не обнаружен";
            case "leak":
                return "true".equals(value) ? "Обнаружена протечка воды!" : "Протечка не обнаружена";
            default:
                return "Изменение состояния датчика";
        }
    }
} 