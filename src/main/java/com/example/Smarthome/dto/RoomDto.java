package com.example.Smarthome.dto;

import com.example.Smarthome.model.Location;
import com.example.Smarthome.model.Room;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;

import java.util.UUID;

/**
 * DTO для передачи информации о комнате через API
 */
@Data
public class RoomDTO {
    private UUID id;
    private String name;
    private String floor;
    private Double area;
    private UUID locationId;
    private String locationName;
    private long deviceCount;
    
    @JsonBackReference
    private Location location;
    
    public static RoomDTO fromEntity(Room room) {
        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setFloor(room.getFloor());
        dto.setArea(room.getArea());
        dto.setLocation(room.getLocation());
        return dto;
    }
} 