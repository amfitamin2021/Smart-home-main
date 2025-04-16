package com.example.Smarthome.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockHistoryDto {
    private String id;
    private String deviceId;
    private String deviceName;
    private String action;
    private String method;
    private String timestamp;
} 