package com.example.Smarthome.service;

import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.model.Room;
import com.example.Smarthome.repository.DeviceRepository;
import com.example.Smarthome.repository.RoomRepository;
import com.example.Smarthome.service.ThingsBoardIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    
    private final DeviceRepository deviceRepository;
    private final ProtocolAdapterService protocolAdapterService;
    private final ThingsBoardIntegrationService thingsBoardService;
    private final RoomRepository roomRepository;
    
    // Карта для хранения времени последней отправки команды для каждого устройства
    private final ConcurrentHashMap<UUID, LocalDateTime> lastCommandTime = new ConcurrentHashMap<>();
    
    // Время (в секундах), в течение которого не нужно синхронизировать устройство после отправки команды
    private static final int SYNC_BLOCK_DURATION_SECONDS = 90;
    
    /**
     * Проверяем, нужно ли блокировать синхронизацию для устройства
     * @param deviceId ID устройства
     * @return true, если синхронизацию нужно заблокировать
     */
    public boolean shouldBlockSync(UUID deviceId) {
        LocalDateTime lastTime = lastCommandTime.get(deviceId);
        if (lastTime == null) {
            return false;
        }
        
        // Если с момента последней команды прошло меньше SYNC_BLOCK_DURATION_SECONDS секунд, блокируем синхронизацию
        return lastTime.plusSeconds(SYNC_BLOCK_DURATION_SECONDS).isAfter(LocalDateTime.now());
    }
    
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }
    
    public Optional<Device> getDeviceById(UUID id) {
        return deviceRepository.findById(id);
    }
    
    public Device saveDevice(Device device) {
        return deviceRepository.save(device);
    }
    
    public void deleteDevice(UUID id) {
        deviceRepository.deleteById(id);
    }
    
    public List<Device> getDevicesByStatus(DeviceStatus status) {
        return deviceRepository.findByStatus(status);
    }
    
    public List<Device> getDevicesByProtocol(ConnectionProtocol protocol) {
        return deviceRepository.findByProtocol(protocol);
    }
    
    public Device updateDeviceStatus(UUID deviceId, DeviceStatus status) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            device.setStatus(status);
            if (status == DeviceStatus.ONLINE) {
                device.setLastSeen(LocalDateTime.now());
            }
            return deviceRepository.save(device);
        }
        throw new IllegalArgumentException("Устройство с ID " + deviceId + " не найдено");
    }
    
    /**
     * Обновляет свойство устройства
     * @param deviceId ID устройства
     * @param propertyName Имя свойства
     * @param propertyValue Значение свойства
     * @return true если свойство успешно обновлено
     */
    public boolean updateDeviceProperty(UUID deviceId, String propertyName, String propertyValue) {
        Optional<Device> deviceOptional = deviceRepository.findById(deviceId);
        if (deviceOptional.isPresent()) {
            Device device = deviceOptional.get();
            device.getProperties().put(propertyName, propertyValue);
            deviceRepository.save(device);
            
            // Синхронизируем с ThingsBoard, если имеется токен
            if (device.getThingsboardToken() != null && !device.getThingsboardToken().isEmpty()) {
                thingsBoardService.sendDeviceUpdate(device);
            }
            
            return true;
        }
        return false;
    }
    
    public boolean sendCommandToDevice(UUID deviceId, String command, Map<String, String> parameters) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            log.debug("Отправка команды '{}' устройству {} ({}), параметры: {}", command, device.getName(), deviceId, parameters);
            
            try {
                boolean success = protocolAdapterService.sendCommand(device, command, parameters);
                
                if (success) {
                    log.info("Команда '{}' успешно отправлена устройству {} ({})", command, device.getName(), deviceId);
                    
                    boolean devicePropertiesChanged = false;
                    
                    // Обрабатываем все параметры, кроме самой команды
                    for (Map.Entry<String, String> entry : parameters.entrySet()) {
                        String paramKey = entry.getKey();
                        String paramValue = entry.getValue();
                        
                        // Пропускаем параметр 'command', так как это имя самой команды
                        if ("command".equals(paramKey)) {
                            continue;
                        }
                        
                        // Проверяем, есть ли уже префикс tb_ в ключе параметра
                        String propertyKey = paramKey.startsWith("tb_") ? paramKey : "tb_" + paramKey;
                        
                        // Сохраняем значение параметра в свойства устройства
                        if (paramValue != null && !paramValue.isEmpty()) {
                            // Проверяем, изменилось ли значение
                            String currentValue = device.getProperties().get(propertyKey);
                            if (currentValue == null || !currentValue.equals(paramValue)) {
                                device.getProperties().put(propertyKey, paramValue);
                                devicePropertiesChanged = true;
                                log.debug("Обновлено свойство {} устройства на {}", propertyKey, paramValue);
                            }
                        }
                    }
                    
                    // Обновляем время последней отправки команды для ЛЮБОЙ успешной команды
                    lastCommandTime.put(deviceId, LocalDateTime.now());
                    log.debug("Блокировка синхронизации устройства {} на {} секунд", device.getName(), SYNC_BLOCK_DURATION_SECONDS);
                    
                    // Если свойства изменились, сохраняем устройство
                    if (devicePropertiesChanged) {
                        deviceRepository.save(device);
                        
                        // Немедленно отправляем обновление в ThingsBoard, чтобы синхронизировать данные
                        if (device.getThingsboardToken() != null && !device.getThingsboardToken().isEmpty()) {
                            boolean tbUpdated = thingsBoardService.sendDeviceUpdate(device);
                            if (tbUpdated) {
                                log.info("Обновление устройства {} успешно отправлено в ThingsBoard", device.getName());
                            } else {
                                log.warn("Не удалось отправить обновление устройства {} в ThingsBoard", device.getName());
                            }
                        }
                    }
                } else {
                    log.warn("Не удалось отправить команду '{}' устройству {} ({})", command, device.getName(), deviceId);
                }
                
                return success;
            } catch (Exception e) {
                log.error("Ошибка при отправке команды на устройство {} ({}): {}", device.getName(), deviceId, e.getMessage());
                return false;
            }
        }
        log.error("Устройство с ID {} не найдено", deviceId);
        throw new IllegalArgumentException("Устройство с ID " + deviceId + " не найдено");
    }
    
    // Планировщик для проверки состояния устройств
    // @Scheduled(fixedRate = 60000) // каждую минуту - временно отключено
    public void checkDevicesStatus() {
        log.debug("Выполняется проверка состояния устройств");
        List<Device> onlineDevices = deviceRepository.findByStatus(DeviceStatus.ONLINE);
        
        for (Device device : onlineDevices) {
            // Проверяем, не превышен ли таймаут
            if (device.getLastSeen() != null &&
                    device.getLastSeen().plusMinutes(30).isBefore(LocalDateTime.now())) {
                log.info("Устройство {} не в сети (таймаут)", device.getName());
                device.setStatus(DeviceStatus.OFFLINE);
                deviceRepository.save(device);
            } else {
                // Запрашиваем статус через адаптер
                boolean isOnline = protocolAdapterService.checkDeviceStatus(device);
                if (!isOnline && device.getStatus() == DeviceStatus.ONLINE) {
                    log.info("Устройство {} перешло в статус OFFLINE", device.getName());
                    device.setStatus(DeviceStatus.OFFLINE);
                    deviceRepository.save(device);
                } else if (isOnline && device.getStatus() == DeviceStatus.OFFLINE) {
                    log.info("Устройство {} перешло в статус ONLINE", device.getName());
                    device.setStatus(DeviceStatus.ONLINE);
                    device.setLastSeen(LocalDateTime.now());
                    deviceRepository.save(device);
                }
            }
        }
    }
    
    /**
     * Находит устройство по его ID в ThingsBoard
     * @param thingsBoardId ID устройства в ThingsBoard
     * @return Optional с устройством, если найдено
     */
    public Optional<Device> findByThingsBoardId(String thingsBoardId) {
        log.debug("Поиск устройства по ThingsBoard ID: {}", thingsBoardId);
        
        // Получаем токен устройства по его ID в ThingsBoard
        String deviceToken = thingsBoardService.getTokenByDeviceId(thingsBoardId);
        
        if (deviceToken == null) {
            log.warn("Не удалось получить токен для устройства с ID {} в ThingsBoard", thingsBoardId);
            return Optional.empty();
        }
        
        // Ищем устройство по токену
        return deviceRepository.findByThingsboardToken(deviceToken);
    }
    
    /**
     * Находит устройство по токену ThingsBoard
     * @param token токен устройства в ThingsBoard
     * @return устройство или null, если не найдено
     */
    public Device findByThingsboardToken(String token) {
        if (token == null || token.isEmpty()) {
            log.debug("Невозможно найти устройство по пустому токену");
            return null;
        }
        
        return deviceRepository.findByThingsboardToken(token).orElse(null);
    }
    
    /**
     * Получает комнату по её ID
     * @param roomId ID комнаты
     * @return комната или null, если не найдена
     */
    public Room getRoomById(UUID roomId) {
        if (roomId == null) {
            return null;
        }
        return roomRepository.findById(roomId).orElse(null);
    }
    
    /**
     * Удаляет все устройства из системы
     */
    public void deleteAllDevices() {
        log.info("Удаление всех устройств из базы данных");
        deviceRepository.deleteAll();
    }
} 