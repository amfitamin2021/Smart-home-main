package com.example.Smarthome.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEventDto {
    private String deviceId;
    private String eventType;
    private Map<String, String> attributes;
} 