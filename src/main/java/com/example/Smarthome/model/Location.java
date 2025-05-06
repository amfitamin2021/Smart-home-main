package com.example.Smarthome.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "locations")
@Data
@JsonIgnoreProperties({"rooms.location", "devices.location"})
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String address;
    
    // Геолокация
    private Double latitude;
    private Double longitude;
    
    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference(value = "location-rooms")
    private List<Room> rooms = new ArrayList<>();
    
    @OneToMany(mappedBy = "location")
    @JsonIgnoreProperties("location")
    private List<Device> devices = new ArrayList<>();
} 