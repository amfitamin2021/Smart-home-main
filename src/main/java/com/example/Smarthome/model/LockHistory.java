package com.example.Smarthome.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lock_history")
@Data
public class LockHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;
    
    @Column(name = "device_name")
    private String deviceName;
    
    @Column(nullable = false)
    private String action;
    
    @Column(nullable = false)
    private String method;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
} 