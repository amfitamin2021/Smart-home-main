package com.example.Smarthome.controller;

import com.example.Smarthome.dto.DeviceCommandRequest;
import com.example.Smarthome.dto.DeviceDto;
import com.example.Smarthome.dto.DeviceEventDto;
import com.example.Smarthome.dto.DeviceRegistrationRequest;
import com.example.Smarthome.dto.AvailableDeviceDto;
import com.example.Smarthome.dto.LockHistoryDto;
import com.example.Smarthome.dto.SensorHistoryDto;
import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.model.Location;
import com.example.Smarthome.model.Room;
import com.example.Smarthome.service.DeviceEventHandler;
import com.example.Smarthome.service.DeviceService;
import com.example.Smarthome.service.LockHistoryService;
import com.example.Smarthome.service.LocationService;
import com.example.Smarthome.service.ProtocolAdapterService;
import com.example.Smarthome.service.SensorHistoryService;
import com.example.Smarthome.service.ThingsBoardIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.HashMap;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;
    private final ProtocolAdapterService protocolAdapterService;
    private final ThingsBoardIntegrationService thingsBoardService;
    private final LocationService locationService;
    private final LockHistoryService lockHistoryService;
    private final SensorHistoryService sensorHistoryService;
    private final DeviceEventHandler deviceEventHandler;

    /**
     * Получение списка всех устройств
     */
    @GetMapping
    public ResponseEntity<List<DeviceDto>> getAllDevices(
            @RequestHeader(name = "X-Auth-User-ID", required = false) String userId) {
        log.info("Получен запрос на получение всех устройств от пользователя {}", userId);
        List<Device> devices = deviceService.getAllDevices();
        List<DeviceDto> deviceDtos = devices.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deviceDtos);
    }

    /**
     * Получение информации об устройстве по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeviceDto> getDevice(@PathVariable UUID id) {
        Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Устройство с ID " + id + " не найдено"));
        
        // Получаем актуальные свойства
        Map<String, String> currentProperties = protocolAdapterService.getDeviceProperties(device);
        if (!currentProperties.isEmpty()) {
            // Обновляем свойства в устройстве
            for (Map.Entry<String, String> entry : currentProperties.entrySet()) {
                device.getProperties().put(entry.getKey(), entry.getValue());
            }
            deviceService.saveDevice(device);
        }
        
        return ResponseEntity.ok(convertToDto(device));
    }

    /**
     * Регистрация нового устройства
     */
    @PostMapping
    public ResponseEntity<DeviceDto> registerDevice(@RequestBody DeviceRegistrationRequest request) {
        log.info("Получен запрос на регистрацию устройства: {}", request);
        
        try {
            Device device = new Device();
            device.setName(request.getName());
            device.setType(request.getType());
            
            // Установка категории и подтипа устройства
            device.setCategory(request.getCategory());
            device.setSubType(request.getSubType());
            
            // Для обратной совместимости, если subType указан, но type не указан
            if (device.getType() == null && device.getSubType() != null) {
                device.setType(device.getSubType());
            }
            
            device.setProtocol(ConnectionProtocol.valueOf(request.getProtocol()));
            device.setConnectionParams(request.getConnectionParams());
            device.setManufacturer(request.getManufacturer());
            device.setModel(request.getModel());
            device.setFirmwareVersion(request.getFirmwareVersion());
            device.setThingsboardToken(request.getThingsboardToken());
            
            // Устанавливаем ID устройства из ThingsBoard, если он указан
            if (request.getThingsboardId() != null && !request.getThingsboardId().isEmpty()) {
                device.setThingsboardDeviceId(request.getThingsboardId());
                log.info("Установлен идентификатор устройства ThingsBoard: {}", request.getThingsboardId());
            }
            
            device.setStatus(DeviceStatus.OFFLINE);
            
            // Устанавливаем связь с локацией
            if (request.getLocationId() != null) {
                // Получаем локацию из репозитория
                Location location = locationService.getLocationById(request.getLocationId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                                "Локация с ID " + request.getLocationId() + " не найдена"));
                device.setLocation(location);
            }
            
            // Если указан ID комнаты, устанавливаем её
            if (request.getRoomId() != null) {
                Room room = null;
                try {
                    room = deviceService.getRoomById(request.getRoomId());
                } catch (Exception e) {
                    log.warn("Не удалось найти комнату с ID: {}", request.getRoomId());
                }
                device.setRoom(room);
            }
            
            // Если это виртуальное устройство, сразу устанавливаем статус ONLINE
            if (device.getProtocol() == ConnectionProtocol.VIRTUAL) {
                device.setStatus(DeviceStatus.ONLINE);
                device.setLastSeen(LocalDateTime.now());
            }
            
            // Установка начальных свойств в зависимости от категории и типа устройства
            if (request.getInitialProperties() != null && !request.getInitialProperties().isEmpty()) {
                device.getProperties().putAll(request.getInitialProperties());
            } else {
                // Автоматическое добавление базовых свойств в зависимости от категории и типа
                enrichDevicePropertiesByCategoryAndType(device);
            }
            
            // Установка возможностей устройства
            if (request.getCapabilities() != null && !request.getCapabilities().isEmpty()) {
                device.getCapabilities().putAll(request.getCapabilities());
            }
            
            // Сначала сохраняем устройство
            Device savedDevice = deviceService.saveDevice(device);
            
            // Создаем устройство в ThingsBoard, если не указан токен
            if (savedDevice.getThingsboardToken() == null || savedDevice.getThingsboardToken().isEmpty()) {
                boolean tbCreated = thingsBoardService.createDevice(savedDevice);
                
                if (tbCreated) {
                    // Обновляем устройство с новым токеном
                    savedDevice = deviceService.saveDevice(savedDevice);
                    log.info("Устройство {} создано в ThingsBoard", savedDevice.getName());
                } else {
                    log.warn("Не удалось создать устройство {} в ThingsBoard", savedDevice.getName());
                }
            } else {
                // Обновляем атрибуты в ThingsBoard
                thingsBoardService.updateDeviceAttributes(savedDevice);
                
                // Если это датчик влажности, отправляем начальные значения телеметрии
                if ("CLIMATE".equals(savedDevice.getCategory()) && "HUMIDITY_SENSOR".equals(savedDevice.getSubType())) {
                    try {
                        // Создаем данные телеметрии для инициализации
                        Map<String, Object> telemetryData = new HashMap<>();
                        telemetryData.put("humidity", 45); // Начальное значение влажности
                        telemetryData.put("battery", 98);  // Начальное значение батареи
                        
                        // Отправляем телеметрию в ThingsBoard
                        thingsBoardService.sendTelemetry(savedDevice, telemetryData);
                        
                        log.info("Отправлены начальные данные телеметрии для датчика влажности: {}", telemetryData);
                    } catch (Exception e) {
                        log.error("Ошибка при отправке начальной телеметрии для датчика влажности: {}", e.getMessage(), e);
                    }
                }
                // Если это датчик температуры, отправляем начальные значения телеметрии
                else if ("CLIMATE".equals(savedDevice.getCategory()) && "TEMPERATURE_SENSOR".equals(savedDevice.getSubType())) {
                    try {
                        // Создаем данные телеметрии для инициализации
                        Map<String, Object> telemetryData = new HashMap<>();
                        telemetryData.put("temperature", 22); // Начальное значение температуры
                        telemetryData.put("battery", 98);  // Начальное значение батареи
                        
                        // Отправляем телеметрию в ThingsBoard
                        thingsBoardService.sendTelemetry(savedDevice, telemetryData);
                        
                        log.info("Отправлены начальные данные телеметрии для датчика температуры: {}", telemetryData);
                    } catch (Exception e) {
                        log.error("Ошибка при отправке начальной телеметрии для датчика температуры: {}", e.getMessage(), e);
                    }
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedDevice));
        } catch (Exception e) {
            log.error("Ошибка при регистрации устройства: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Ошибка при регистрации устройства: " + e.getMessage());
        }
    }

    /**
     * Обновление информации устройства
     */
    @PutMapping("/{id}")
    public ResponseEntity<DeviceDto> updateDevice(@PathVariable UUID id, 
                                                  @RequestBody DeviceRegistrationRequest request) {
        Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Устройство с ID " + id + " не найдено"));

        device.setName(request.getName());
        device.setType(request.getType());
        
        // Обновление категории и подтипа устройства
        if (request.getCategory() != null) {
            device.setCategory(request.getCategory());
        }
        
        if (request.getSubType() != null) {
            device.setSubType(request.getSubType());
        }
        
        device.setProtocol(ConnectionProtocol.valueOf(request.getProtocol()));
        device.setConnectionParams(request.getConnectionParams());
        device.setManufacturer(request.getManufacturer());
        device.setModel(request.getModel());
        device.setFirmwareVersion(request.getFirmwareVersion());
        
        // Обновляем связь с локацией
        if (request.getLocationId() != null) {
            Location location = locationService.getLocationById(request.getLocationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Локация с ID " + request.getLocationId() + " не найдена"));
            device.setLocation(location);
        }
        
        // Если токен был обновлен, обновляем его
        if (request.getThingsboardToken() != null) {
            device.setThingsboardToken(request.getThingsboardToken());
        }
        
        Device updatedDevice = deviceService.saveDevice(device);
        
        // Обновляем устройство в ThingsBoard
        if (updatedDevice.getThingsboardToken() != null && !updatedDevice.getThingsboardToken().isEmpty()) {
            boolean tbUpdated = thingsBoardService.updateDevice(updatedDevice);
            if (tbUpdated) {
                log.info("Устройство {} обновлено в ThingsBoard", updatedDevice.getName());
            } else {
                log.warn("Не удалось обновить устройство {} в ThingsBoard", updatedDevice.getName());
            }
        }
        
        return ResponseEntity.ok(convertToDto(updatedDevice));
    }

    /**
     * Удаление устройства
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID id) {
        if (!deviceService.getDeviceById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                    "Устройство с ID " + id + " не найдено");
        }
        
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Отправка команды устройству
     */
    @PostMapping("/{id}/command")
    public ResponseEntity<String> sendCommand(
            @PathVariable UUID id, 
            @RequestBody DeviceCommandRequest commandRequest,
            @RequestHeader(name = "X-Auth-User-ID", required = false) String userId) {
        log.info("Получен запрос на отправку команды устройству {} от пользователя {}", id, userId);
        log.debug("Данные команды: {}", commandRequest);
        
        String command = commandRequest.getCommand();
        if (command == null) {
            log.warn("Команда не указана для устройства {}", id);
            return ResponseEntity.badRequest().body("Команда не указана");
        }
        
        try {
            // Проверяем существование устройства
            Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> {
                    log.error("Устройство с ID {} не найдено", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Устройство с ID " + id + " не найдено");
                });
            
            log.info("Найдено устройство: {}, тип: {}, состояние: {}", device.getName(), device.getType(), device.getStatus());
            
            // Создаем Map из DTO для совместимости с существующим методом sendCommandToDevice
            Map<String, String> commandData = new HashMap<>();
            commandData.put("command", command);
            if (commandRequest.getParameters() != null) {
                commandData.putAll(commandRequest.getParameters());
            }
            
            // Отправляем команду на устройство
            boolean success = deviceService.sendCommandToDevice(id, command, commandData);
            
            if (success) {
                log.info("Команда '{}' успешно отправлена устройству {} ({})", command, device.getName(), id);
                return ResponseEntity.ok("Команда отправлена успешно");
            } else {
                log.warn("Не удалось отправить команду '{}' устройству {} ({})", command, device.getName(), id);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Не удалось отправить команду устройству");
            }
        } catch (ResponseStatusException e) {
            log.error("Ошибка в запросе: {}", e.getReason());
            throw e; // Пробрасываем исключение для стандартной обработки
        } catch (Exception e) {
            log.error("Ошибка при отправке команды '{}' устройству {}: {}", 
                command, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ошибка при отправке команды: " + e.getMessage());
        }
    }

    /**
     * Синхронизация устройства с ThingsBoard
     */
    @PostMapping("/{id}/sync-thingsboard")
    public ResponseEntity<Map<String, Object>> syncThingsBoard(@PathVariable UUID id) {
        Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Устройство с ID " + id + " не найдено"));
        
        boolean success;
        
        // Если токен не установлен, пытаемся создать устройство
        if (device.getThingsboardToken() == null || device.getThingsboardToken().isEmpty()) {
            success = thingsBoardService.createDevice(device);
            if (success) {
                // Сохраняем устройство с новым токеном
                deviceService.saveDevice(device);
                log.info("Устройство {} создано в ThingsBoard", device.getName());
            }
        } else {
            // Обновляем существующее устройство
            success = thingsBoardService.updateDevice(device);
            if (success) {
                // И отправляем телеметрию
                thingsBoardService.sendDeviceUpdate(device);
            }
        }
        
        if (!success) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Ошибка при синхронизации с ThingsBoard");
        }
        
        Map<String, Object> response = Map.of(
                "success", true,
                "device_id", id.toString(),
                "message", "Устройство успешно синхронизировано с ThingsBoard"
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Тестовый метод для получения токена устройства из ThingsBoard по имени
     */
    @GetMapping("/test-thingsboard/{deviceName}")
    public ResponseEntity<Map<String, Object>> testThingsBoard(@PathVariable String deviceName) {
        log.info("Тестирование получения токена для устройства: {}", deviceName);
        
        String token = thingsBoardService.getTokenByDeviceName(deviceName);
        
        if (token != null) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "device_name", deviceName,
                "token", token
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "device_name", deviceName,
                "message", "Не удалось получить токен устройства"
            ));
        }
    }

    /**
     * Получение списка доступных устройств из ThingsBoard
     */
    @GetMapping("/available-devices")
    public ResponseEntity<List<AvailableDeviceDto>> getAvailableDevices() {
        List<AvailableDeviceDto> devices = thingsBoardService.getAvailableDevices();
        return ResponseEntity.ok(devices);
    }

    /**
     * Получить историю значений влажности
     * @param id ID устройства
     * @param interval Интервал (hour, day, week, month)
     * @return Список значений влажности с метками времени
     */
    @GetMapping("/{id}/humidity-history")
    public ResponseEntity<List<Map<String, Object>>> getHumidityHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "day") String interval) {
        
        Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Устройство с ID " + id + " не найдено"));
        
        if (!"CLIMATE".equals(device.getCategory()) || !"HUMIDITY_SENSOR".equals(device.getSubType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Устройство не является датчиком влажности");
        }
        
        // Определяем временной интервал
        long endTs = System.currentTimeMillis();
        long startTs = endTs - getTimeInterval(interval);
        
        try {
            // Получаем историю из ThingsBoard
            List<Map<String, Object>> history = thingsBoardService.getDeviceTelemetryHistory(
                device, 
                "humidity",
                startTs,
                endTs
            );
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Ошибка при получении истории влажности: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Не удалось получить историю влажности");
        }
    }
    
    /**
     * Получение истории температуры устройства
     */
    @GetMapping("/{id}/temperature-history")
    public ResponseEntity<List<Map<String, Object>>> getTemperatureHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "day") String interval) {
        
        Device device = deviceService.getDeviceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        "Устройство с ID " + id + " не найдено"));
        
        if (!"CLIMATE".equals(device.getCategory()) || 
            (!"TEMPERATURE_SENSOR".equals(device.getSubType()) && !"HUMIDITY_SENSOR".equals(device.getSubType()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Устройство не является датчиком температуры");
        }
        
        // Определяем временной интервал
        long endTs = System.currentTimeMillis();
        long startTs = endTs - getTimeInterval(interval);
        
        try {
            // Получаем историю из ThingsBoard
            List<Map<String, Object>> history = thingsBoardService.getDeviceTelemetryHistory(
                device, 
                "temperature",
                startTs,
                endTs
            );
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Ошибка при получении истории температуры: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Не удалось получить историю температуры");
        }
    }
    
    /**
     * Преобразует строковый интервал в миллисекунды
     */
    private long getTimeInterval(String interval) {
        switch (interval.toLowerCase()) {
            case "hour":
                return 3600000L;
            case "day":
                return 86400000L;
            case "week":
                return 604800000L;
            case "month":
                return 2592000000L;
            default:
                return 86400000L; // день по умолчанию
        }
    }

    /**
     * Конвертация модели устройства в DTO
     */
    private DeviceDto convertToDto(Device device) {
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setType(device.getType());
        
        // Добавляем категорию и подтип устройства
        dto.setCategory(device.getCategory());
        dto.setSubType(device.getSubType());
        
        dto.setProtocol(device.getProtocol().name());
        dto.setStatus(device.getStatus().name());
        dto.setConnectionParams(device.getConnectionParams());
        dto.setLastSeen(device.getLastSeen());
        dto.setProperties(device.getProperties());
        dto.setCapabilities(device.getCapabilities());
        dto.setManufacturer(device.getManufacturer());
        dto.setModel(device.getModel());
        dto.setFirmwareVersion(device.getFirmwareVersion());
        dto.setThingsboardToken(device.getThingsboardToken());
        dto.setThingsboardId(device.getThingsboardDeviceId());
        
        // Добавляем атрибуты
        dto.setAttributes(device.getAttributes());
        
        if (device.getRoom() != null) {
            dto.setRoomId(device.getRoom().getId());
            dto.setRoomName(device.getRoom().getName());
        }
        
        if (device.getLocation() != null) {
            dto.setLocationId(device.getLocation().getId());
            dto.setLocationName(device.getLocation().getName());
        }
        
        return dto;
    }
    
    /**
     * Метод для автоматического добавления базовых свойств устройству в зависимости от категории и типа
     */
    private void enrichDevicePropertiesByCategoryAndType(Device device) {
        String category = device.getCategory();
        String subType = device.getSubType();
        
        if (category == null || subType == null) {
            return;
        }
        
        // Генерируем уникальный ID для устройства
        String uniqueId = subType.toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);
        device.getProperties().put("device_unique_id", uniqueId);
        
        switch(category) {
            case "LIGHTING":
                if ("SMART_BULB".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_brightness", "80");
                    device.getProperties().put("tb_color", "FFFFFF");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("brightness", "true");
                    device.getCapabilities().put("color", "true");
                } else if ("LED_STRIP".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_brightness", "80");
                    device.getProperties().put("tb_color", "FFFFFF");
                    device.getProperties().put("tb_effect", "none");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("brightness", "true");
                    device.getCapabilities().put("color", "true");
                    device.getCapabilities().put("effects", "true");
                }
                break;
                
            case "CLIMATE":
                if ("THERMOSTAT".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_temperature", "22");
                    device.getProperties().put("tb_target_temperature", "22");
                    device.getProperties().put("tb_mode", "auto");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("temperature", "true");
                    device.getCapabilities().put("target_temperature", "true");
                    device.getCapabilities().put("mode", "true");
                } else if ("TEMPERATURE_SENSOR".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_temperature", "22");
                    device.getProperties().put("tb_battery", "98");
                    device.getProperties().put("tb_last_updated", LocalDateTime.now().toString());
                    
                    device.getCapabilities().put("temperature", "true");
                    device.getCapabilities().put("battery", "true");
                } else if ("HUMIDITY_SENSOR".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_humidity", "45");
                    device.getProperties().put("tb_temperature", "22");
                    device.getProperties().put("tb_battery", "98");
                    device.getProperties().put("tb_last_updated", LocalDateTime.now().toString());
                    
                    device.getCapabilities().put("humidity", "true");
                    device.getCapabilities().put("temperature", "true");
                    device.getCapabilities().put("battery", "true");
                }
                break;
                
            case "SECURITY":
                if ("CAMERA".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_recording", "off");
                    device.getProperties().put("tb_motion_detection", "on");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("recording", "true");
                    device.getCapabilities().put("motion_detection", "true");
                } else if ("SMART_LOCK".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_locked", "true");
                    device.getProperties().put("tb_battery", "100");
                    
                    device.getCapabilities().put("lock", "true");
                    device.getCapabilities().put("battery", "true");
                }
                break;
                
            case "APPLIANCES":
                if ("TV".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_volume", "50");
                    device.getProperties().put("tb_channel", "1");
                    device.getProperties().put("tb_input_source", "tv");
                    device.getProperties().put("tb_favorite_channels", "1,2,4,8");
                    device.getProperties().put("tb_last_updated", LocalDateTime.now().toString());
                    device.getProperties().put("tb_screen_mode", "normal");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("volume", "true");
                    device.getCapabilities().put("channel", "true");
                    device.getCapabilities().put("input_source", "true");
                    device.getCapabilities().put("screen_mode", "true");
                } else if ("VACUUM".equals(subType)) {
                    device.getProperties().put("attr_server_active", "true");
                    device.getProperties().put("tb_power", "off");
                    device.getProperties().put("tb_mode", "auto");
                    device.getProperties().put("tb_battery", "100");
                    
                    device.getCapabilities().put("toggle", "true");
                    device.getCapabilities().put("mode", "true");
                    device.getCapabilities().put("battery", "true");
                }
                break;
                
            default:
                // Для всех остальных типов устройств устанавливаем базовые свойства
                device.getProperties().put("attr_server_active", "true");
                device.getProperties().put("tb_power", "off");
        }
    }

    /**
     * Получение истории действий с замком
     */
    @GetMapping("/{id}/lock-history")
    public ResponseEntity<List<LockHistoryDto>> getLockHistory(
            @PathVariable UUID id) {
        log.info("Запрос истории замка с ID: {}", id);
        
        try {
            // Проверяем, существует ли устройство
            Device device = deviceService.getDeviceById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Устройство с ID " + id + " не найдено"));
            
            // Проверяем, является ли устройство замком (опционально)
            if (!(device.getType().equalsIgnoreCase("lock") || 
                    ("SECURITY".equals(device.getCategory()) && "SMART_LOCK".equals(device.getSubType())))) {
                log.warn("Запрос истории для устройства, которое не является замком: {}", device.getName());
            }
            
            // Получаем историю замка
            List<LockHistoryDto> history = lockHistoryService.getLockHistoryByDeviceId(id);
            log.info("Получена история замка: {} записей", history.size());
            
            return ResponseEntity.ok(history);
        } catch (ResponseStatusException e) {
            log.error("Ошибка при получении истории замка: {}", e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("Внутренняя ошибка при получении истории замка: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Внутренняя ошибка при получении истории замка: " + e.getMessage());
        }
    }
    
    /**
     * Получение истории действий со всеми замками
     */
    @GetMapping("/lock-history")
    public ResponseEntity<List<LockHistoryDto>> getAllLockHistory() {
        log.info("Запрос истории всех замков");
        
        try {
            // Получаем историю всех замков
            List<LockHistoryDto> history = lockHistoryService.getAllLockHistory();
            log.info("Получена история всех замков: {} записей", history.size());
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Внутренняя ошибка при получении истории всех замков: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Внутренняя ошибка при получении истории всех замков: " + e.getMessage());
        }
    }
    
    /**
     * Добавление записи в историю замка
     */
    @PostMapping("/{id}/lock-history")
    public ResponseEntity<LockHistoryDto> addLockHistoryEntry(
            @PathVariable UUID id,
            @RequestBody LockHistoryDto historyDto) {
        log.info("Добавление записи в историю замка с ID: {}, данные: {}", id, historyDto);
        
        try {
            // Проверяем, существует ли устройство
            Device device = deviceService.getDeviceById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Устройство с ID " + id + " не найдено"));
            
            // Проверяем, является ли устройство замком
            if (!(device.getType().equalsIgnoreCase("lock") || 
                    ("SECURITY".equals(device.getCategory()) && "SMART_LOCK".equals(device.getSubType())))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                        "Устройство с ID " + id + " не является замком");
            }
            
            // Если имя устройства не указано, устанавливаем его из базы данных
            if (historyDto.getDeviceName() == null || historyDto.getDeviceName().isEmpty()) {
                historyDto.setDeviceName(device.getName());
            }
            
            // Добавляем запись в историю
            LockHistoryDto createdEntry = lockHistoryService.addLockHistoryEntry(id, historyDto);
            log.info("Успешно добавлена запись в историю замка: {}", createdEntry);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdEntry);
        } catch (ResponseStatusException e) {
            // Пробрасываем ошибки статуса напрямую
            log.error("Ошибка при добавлении истории замка: {}", e.getReason());
            throw e;
        } catch (Exception e) {
            // Логируем и преобразуем другие ошибки в HTTP 500
            log.error("Внутренняя ошибка при добавлении истории замка: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Внутренняя ошибка при добавлении истории замка: " + e.getMessage());
        }
    }

    /**
     * Получение истории срабатывания датчиков
     */
    @GetMapping("/sensor-history")
    public ResponseEntity<List<SensorHistoryDto>> getAllSensorHistory() {
        log.info("Запрос истории всех датчиков");
        
        try {
            // Получаем историю всех датчиков
            List<SensorHistoryDto> history = sensorHistoryService.getAllSensorHistory();
            log.info("Получена история всех датчиков: {} записей", history.size());
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Внутренняя ошибка при получении истории всех датчиков: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Внутренняя ошибка при получении истории всех датчиков: " + e.getMessage());
        }
    }
    
    /**
     * Получение истории срабатывания датчика по ID
     */
    @GetMapping("/{id}/sensor-history")
    public ResponseEntity<List<SensorHistoryDto>> getSensorHistory(
            @PathVariable UUID id) {
        log.info("Запрос истории датчика с ID: {}", id);
        
        try {
            // Проверяем, существует ли устройство
            Device device = deviceService.getDeviceById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Устройство с ID " + id + " не найдено"));
            
            // Получаем историю датчика
            List<SensorHistoryDto> history = sensorHistoryService.getSensorHistoryByDeviceId(id);
            log.info("Получена история датчика: {} записей", history.size());
            
            return ResponseEntity.ok(history);
        } catch (ResponseStatusException e) {
            log.error("Ошибка при получении истории датчика: {}", e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("Внутренняя ошибка при получении истории датчика: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Внутренняя ошибка при получении истории датчика: " + e.getMessage());
        }
    }
    
    /**
     * Добавление записи в историю датчика
     */
    @PostMapping("/{id}/sensor-history")
    public ResponseEntity<SensorHistoryDto> addSensorHistoryEntry(
            @PathVariable UUID id, 
            @RequestBody SensorHistoryDto historyDto) {
        log.info("Запрос на добавление записи в историю датчика с ID: {}", id);
        
        try {
            // Проверяем, существует ли устройство
            Device device = deviceService.getDeviceById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Устройство с ID " + id + " не найдено"));
            
            // Добавляем запись в историю
            SensorHistoryDto savedEntry = sensorHistoryService.addSensorHistoryEntry(id, historyDto);
            log.info("Добавлена запись в историю датчика с ID: {}", id);
            
            return ResponseEntity.ok(savedEntry);
        } catch (ResponseStatusException e) {
            log.error("Ошибка при добавлении записи в историю датчика: {}", e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("Внутренняя ошибка при добавлении записи в историю датчика: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Внутренняя ошибка при добавлении записи в историю датчика: " + e.getMessage());
        }
    }
    
    /**
     * Подтверждение записи в истории датчика
     */
    @PutMapping("/sensor-history/{entryId}/acknowledge")
    public ResponseEntity<SensorHistoryDto> acknowledgeSensorHistoryEntry(
            @PathVariable UUID entryId) {
        log.info("Запрос на подтверждение записи в истории с ID: {}", entryId);
        
        try {
            // Обновляем статус подтверждения
            SensorHistoryDto updatedEntry = sensorHistoryService.updateAcknowledgedStatus(entryId, true);
            log.info("Подтверждена запись в истории с ID: {}", entryId);
            
            return ResponseEntity.ok(updatedEntry);
        } catch (IllegalArgumentException e) {
            log.error("Запись не найдена: {}", entryId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена: " + e.getMessage());
        } catch (Exception e) {
            log.error("Внутренняя ошибка при подтверждении записи: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Внутренняя ошибка при подтверждении записи: " + e.getMessage());
        }
    }
    
    /**
     * Подтверждение всех записей в истории датчиков
     */
    @PostMapping("/sensor-history/acknowledge-all")
    public ResponseEntity<Map<String, Object>> acknowledgeAllSensorHistory() {
        log.info("Запрос на подтверждение всех записей в истории датчиков");
        
        try {
            // Подтверждаем все записи
            int count = sensorHistoryService.acknowledgeAllEvents();
            log.info("Подтверждено записей: {}", count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("acknowledgedCount", count);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Внутренняя ошибка при подтверждении всех записей: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Внутренняя ошибка при подтверждении всех записей: " + e.getMessage());
        }
    }
    
    /**
     * Подтверждение всех записей в истории конкретного датчика
     */
    @PostMapping("/{id}/sensor-history/acknowledge-all")
    public ResponseEntity<Map<String, Object>> acknowledgeAllSensorHistoryForDevice(
            @PathVariable UUID id) {
        log.info("Запрос на подтверждение всех записей в истории датчика с ID: {}", id);
        
        try {
            // Проверяем, существует ли устройство
            deviceService.getDeviceById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                            "Устройство с ID " + id + " не найдено"));
            
            // Подтверждаем все записи для устройства
            int count = sensorHistoryService.acknowledgeAllEventsForDevice(id);
            log.info("Подтверждено записей для датчика {}: {}", id, count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("acknowledgedCount", count);
            
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            log.error("Ошибка при подтверждении записей: {}", e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("Внутренняя ошибка при подтверждении записей: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Внутренняя ошибка при подтверждении записей: " + e.getMessage());
        }
    }

    /**
     * Удаление всех устройств из системы
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, String>> deleteAllDevices() {
        log.info("Получен запрос на удаление всех устройств");
        
        // Получаем список всех устройств
        List<Device> devices = deviceService.getAllDevices();
        int count = devices.size();
        
        // Удаляем все устройства из ThingsBoard, если необходимо
        for (Device device : devices) {
            if (device.getThingsboardDeviceId() != null && !device.getThingsboardDeviceId().isEmpty()) {
                try {
                    thingsBoardService.deleteDevice(device);
                    log.info("Устройство {} удалено из ThingsBoard", device.getName());
                } catch (Exception e) {
                    log.error("Ошибка при удалении устройства {} из ThingsBoard: {}", 
                            device.getName(), e.getMessage());
                }
            }
        }
        
        // Удаляем все устройства из базы данных
        deviceService.deleteAllDevices();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Удалено " + count + " устройств");
        
        return ResponseEntity.ok(response);
    }
} 