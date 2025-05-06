package com.example.Smarthome.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sensor_history")
@Data
public class SensorHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;
    
    @Column(name = "device_name")
    private String deviceName;
    
    @Column(name = "room")
    private String room;
    
    @Column(name = "sensor_type", nullable = false)
    private String sensorType;
    
    @Column(name = "value", nullable = false)
    private String value;
    
    @Column(name = "message")
    private String message;
    
    @Column(name = "priority", nullable = false)
    private String priority;
    
    @Column(name = "acknowledged")
    private Boolean acknowledged;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
} 