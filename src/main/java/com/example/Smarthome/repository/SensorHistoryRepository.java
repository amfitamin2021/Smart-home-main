package com.example.Smarthome.repository;

import com.example.Smarthome.model.SensorHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SensorHistoryRepository extends JpaRepository<SensorHistory, UUID> {
    
    /**
     * Находит историю срабатываний датчика по его ID
     * @param deviceId ID устройства (датчика)
     * @return список записей истории
     */
    List<SensorHistory> findByDeviceIdOrderByTimestampDesc(UUID deviceId);
    
    /**
     * Находит всю историю срабатываний датчиков, отсортированную по времени (сначала новые)
     * @return список записей истории
     */
    List<SensorHistory> findAllByOrderByTimestampDesc();
    
    /**
     * Находит историю срабатываний по типу датчика
     * @param sensorType тип датчика
     * @return список записей истории
     */
    List<SensorHistory> findBySensorTypeOrderByTimestampDesc(String sensorType);
    
    /**
     * Находит неподтвержденные срабатывания
     * @return список записей истории
     */
    List<SensorHistory> findByAcknowledgedFalseOrderByTimestampDesc();
    
    /**
     * Находит неподтвержденные срабатывания для конкретного датчика
     * @param deviceId ID устройства (датчика)
     * @return список записей истории
     */
    List<SensorHistory> findByDeviceIdAndAcknowledgedFalseOrderByTimestampDesc(UUID deviceId);
} 