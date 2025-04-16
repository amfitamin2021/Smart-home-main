package com.example.Smarthome.service;

import com.example.Smarthome.dto.LockHistoryDto;
import com.example.Smarthome.model.LockHistory;
import com.example.Smarthome.repository.LockHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LockHistoryService {
    
    private final LockHistoryRepository lockHistoryRepository;
    
    /**
     * Получить всю историю действий с замками
     * @return список записей истории
     */
    public List<LockHistoryDto> getAllLockHistory() {
        try {
            log.debug("Получение истории всех замков");
            return lockHistoryRepository.findAllByOrderByTimestampDesc()
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении истории всех замков: {}", e.getMessage(), e);
            // Возвращаем пустой список вместо ошибки
            return List.of();
        }
    }
    
    /**
     * Получить историю действий с конкретным замком
     * @param deviceId ID устройства (замка)
     * @return список записей истории
     */
    public List<LockHistoryDto> getLockHistoryByDeviceId(UUID deviceId) {
        try {
            log.debug("Получение истории для замка с ID: {}", deviceId);
            return lockHistoryRepository.findByDeviceIdOrderByTimestampDesc(deviceId)
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении истории замка {}: {}", deviceId, e.getMessage(), e);
            // Возвращаем пустой список вместо ошибки
            return List.of();
        }
    }
    
    /**
     * Добавить новую запись в историю замка
     * @param deviceId ID устройства (замка)
     * @param historyDto данные о действии
     * @return созданная запись
     */
    public LockHistoryDto addLockHistoryEntry(UUID deviceId, LockHistoryDto historyDto) {
        try {
            log.debug("Добавление записи в историю замка с ID: {}, данные: {}", deviceId, historyDto);
            
            LockHistory history = new LockHistory();
            
            // Устанавливаем deviceId
            history.setDeviceId(deviceId);
            
            // Устанавливаем имя устройства из DTO или используем значение по умолчанию
            String deviceName = historyDto.getDeviceName();
            if (deviceName == null || deviceName.isEmpty()) {
                log.warn("Имя устройства не указано в запросе, используем 'Неизвестное устройство'");
                deviceName = "Неизвестное устройство";
            }
            history.setDeviceName(deviceName);
            
            // Проверяем обязательные поля
            String action = historyDto.getAction();
            if (action == null || action.isEmpty()) {
                log.warn("Действие не указано в запросе, используем 'Неизвестное действие'");
                action = "Неизвестное действие";
            }
            history.setAction(action);
            
            // Устанавливаем метод или значение по умолчанию
            String method = historyDto.getMethod();
            if (method == null || method.isEmpty()) {
                method = "Автоматически";
            }
            history.setMethod(method);
            
            history.setTimestamp(LocalDateTime.now());
            
            log.debug("Сохранение записи истории: {}", history);
            LockHistory savedHistory = lockHistoryRepository.save(history);
            log.info("Добавлена новая запись в историю замка: {}", savedHistory);
            
            return convertToDto(savedHistory);
        } catch (Exception e) {
            log.error("Ошибка при добавлении записи в историю замка {}: {}", deviceId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Преобразовать Entity в DTO
     * @param history запись истории замка
     * @return DTO
     */
    private LockHistoryDto convertToDto(LockHistory history) {
        try {
            if (history == null) {
                log.warn("Попытка конвертации null объекта LockHistory в DTO");
                return null;
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            
            String id = history.getId() != null ? history.getId().toString() : null;
            String deviceId = history.getDeviceId() != null ? history.getDeviceId().toString() : null;
            String timestamp = history.getTimestamp() != null ? history.getTimestamp().format(formatter) : null;
            
            return new LockHistoryDto(
                    id,
                    deviceId,
                    history.getDeviceName(),
                    history.getAction(),
                    history.getMethod(),
                    timestamp
            );
        } catch (Exception e) {
            log.error("Ошибка при конвертации LockHistory в DTO: {}", e.getMessage(), e);
            // В случае ошибки возвращаем минимально заполненный DTO
            return new LockHistoryDto(
                    history.getId() != null ? history.getId().toString() : "unknown",
                    history.getDeviceId() != null ? history.getDeviceId().toString() : "unknown",
                    "Ошибка конвертации",
                    "Ошибка",
                    "Неизвестно",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            );
        }
    }
} 