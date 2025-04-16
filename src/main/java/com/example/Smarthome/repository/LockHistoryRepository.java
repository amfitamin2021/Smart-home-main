package com.example.Smarthome.repository;

import com.example.Smarthome.model.LockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LockHistoryRepository extends JpaRepository<LockHistory, UUID> {
    
    /**
     * Находит историю действий с замком по его ID
     * @param deviceId ID устройства (замка)
     * @return список записей истории
     */
    List<LockHistory> findByDeviceIdOrderByTimestampDesc(UUID deviceId);
    
    /**
     * Находит всю историю действий с замками, отсортированную по времени (сначала новые)
     * @return список записей истории
     */
    List<LockHistory> findAllByOrderByTimestampDesc();
} 