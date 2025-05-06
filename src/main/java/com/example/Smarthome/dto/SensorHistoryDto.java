package com.example.Smarthome.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorHistoryDto {
    private String id;
    private String deviceId;
    private String deviceName;
    private String room;
    private String sensorType;
    private String value;
    private String message;
    private String priority;
    private Boolean acknowledged;
    private String timestamp;
} 