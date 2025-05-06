package com.example.Smarthome.service;

import com.example.Smarthome.dto.SensorHistoryDto;
import com.example.Smarthome.model.SensorHistory;
import com.example.Smarthome.repository.SensorHistoryRepository;
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
public class SensorHistoryService {
    
    private final SensorHistoryRepository sensorHistoryRepository;
    
    /**
     * Получить всю историю срабатываний датчиков
     * @return список записей истории
     */
    public List<SensorHistoryDto> getAllSensorHistory() {
        try {
            log.debug("Получение истории всех датчиков");
            return sensorHistoryRepository.findAllByOrderByTimestampDesc()
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении истории всех датчиков: {}", e.getMessage(), e);
            // Возвращаем пустой список вместо ошибки
            return List.of();
        }
    }
    
    /**
     * Получить историю срабатываний конкретного датчика
     * @param deviceId ID устройства (датчика)
     * @return список записей истории
     */
    public List<SensorHistoryDto> getSensorHistoryByDeviceId(UUID deviceId) {
        try {
            log.debug("Получение истории для датчика с ID: {}", deviceId);
            return sensorHistoryRepository.findByDeviceIdOrderByTimestampDesc(deviceId)
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении истории датчика {}: {}", deviceId, e.getMessage(), e);
            // Возвращаем пустой список вместо ошибки
            return List.of();
        }
    }
    
    /**
     * Получить историю срабатываний по типу датчика
     * @param sensorType тип датчика
     * @return список записей истории
     */
    public List<SensorHistoryDto> getSensorHistoryByType(String sensorType) {
        try {
            log.debug("Получение истории для датчиков типа: {}", sensorType);
            return sensorHistoryRepository.findBySensorTypeOrderByTimestampDesc(sensorType)
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении истории датчиков типа {}: {}", sensorType, e.getMessage(), e);
            // Возвращаем пустой список вместо ошибки
            return List.of();
        }
    }
    
    /**
     * Получить неподтвержденные срабатывания
     * @return список записей истории
     */
    public List<SensorHistoryDto> getUnacknowledgedEvents() {
        try {
            log.debug("Получение неподтвержденных срабатываний");
            return sensorHistoryRepository.findByAcknowledgedFalseOrderByTimestampDesc()
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Ошибка при получении неподтвержденных срабатываний: {}", e.getMessage(), e);
            // Возвращаем пустой список вместо ошибки
            return List.of();
        }
    }
    
    /**
     * Добавить новую запись в историю срабатываний датчика
     * @param deviceId ID устройства (датчика)
     * @param historyDto данные о срабатывании
     * @return созданная запись
     */
    public SensorHistoryDto addSensorHistoryEntry(UUID deviceId, SensorHistoryDto historyDto) {
        try {
            log.debug("Добавление записи в историю датчика: {} - {}", deviceId, historyDto);
            
            // Создаем новую запись
            SensorHistory entry = new SensorHistory();
            entry.setDeviceId(deviceId);
            entry.setDeviceName(historyDto.getDeviceName());
            entry.setRoom(historyDto.getRoom());
            entry.setSensorType(historyDto.getSensorType());
            entry.setValue(historyDto.getValue());
            entry.setMessage(historyDto.getMessage());
            entry.setPriority(historyDto.getPriority());
            entry.setAcknowledged(historyDto.getAcknowledged() != null ? historyDto.getAcknowledged() : false);
            entry.setTimestamp(LocalDateTime.now());
            
            // Сохраняем запись в репозитории
            SensorHistory savedEntry = sensorHistoryRepository.save(entry);
            log.debug("Запись в историю датчика добавлена: {}", savedEntry.getId());
            
            // Возвращаем DTO
            return convertToDto(savedEntry);
        } catch (Exception e) {
            log.error("Ошибка при добавлении записи в историю датчика {}: {}", deviceId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Обновить статус подтверждения записи
     * @param entryId ID записи
     * @param acknowledged статус подтверждения
     * @return обновленная запись
     */
    public SensorHistoryDto updateAcknowledgedStatus(UUID entryId, Boolean acknowledged) {
        try {
            log.debug("Обновление статуса подтверждения записи: {} -> {}", entryId, acknowledged);
            
            // Находим запись
            SensorHistory entry = sensorHistoryRepository.findById(entryId)
                    .orElseThrow(() -> new IllegalArgumentException("Запись не найдена"));
            
            // Обновляем статус
            entry.setAcknowledged(acknowledged);
            
            // Сохраняем обновленную запись
            SensorHistory updatedEntry = sensorHistoryRepository.save(entry);
            log.debug("Статус подтверждения обновлен для записи: {}", entryId);
            
            // Возвращаем DTO
            return convertToDto(updatedEntry);
        } catch (Exception e) {
            log.error("Ошибка при обновлении статуса подтверждения: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Подтвердить все неподтвержденные записи
     * @return количество обновленных записей
     */
    public int acknowledgeAllEvents() {
        try {
            log.debug("Подтверждение всех неподтвержденных записей");
            
            // Получаем все неподтвержденные записи
            List<SensorHistory> unacknowledgedEntries = sensorHistoryRepository.findByAcknowledgedFalseOrderByTimestampDesc();
            
            // Обновляем статус для каждой записи
            int count = 0;
            for (SensorHistory entry : unacknowledgedEntries) {
                entry.setAcknowledged(true);
                sensorHistoryRepository.save(entry);
                count++;
            }
            
            log.debug("Подтверждено записей: {}", count);
            return count;
        } catch (Exception e) {
            log.error("Ошибка при подтверждении всех записей: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Подтвердить все неподтвержденные записи для конкретного датчика
     * @param deviceId ID устройства (датчика)
     * @return количество обновленных записей
     */
    public int acknowledgeAllEventsForDevice(UUID deviceId) {
        try {
            log.debug("Подтверждение всех неподтвержденных записей для датчика: {}", deviceId);
            
            // Получаем все неподтвержденные записи для датчика
            List<SensorHistory> unacknowledgedEntries = sensorHistoryRepository.findByDeviceIdAndAcknowledgedFalseOrderByTimestampDesc(deviceId);
            
            // Обновляем статус для каждой записи
            int count = 0;
            for (SensorHistory entry : unacknowledgedEntries) {
                entry.setAcknowledged(true);
                sensorHistoryRepository.save(entry);
                count++;
            }
            
            log.debug("Подтверждено записей для датчика {}: {}", deviceId, count);
            return count;
        } catch (Exception e) {
            log.error("Ошибка при подтверждении записей для датчика {}: {}", deviceId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Преобразование модели в DTO
     * @param entity модель
     * @return DTO
     */
    private SensorHistoryDto convertToDto(SensorHistory entity) {
        return new SensorHistoryDto(
                entity.getId().toString(),
                entity.getDeviceId().toString(),
                entity.getDeviceName(),
                entity.getRoom(),
                entity.getSensorType(),
                entity.getValue(),
                entity.getMessage(),
                entity.getPriority(),
                entity.getAcknowledged(),
                entity.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
} 