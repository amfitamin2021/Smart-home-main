package com.example.Smarthome.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Data
@JsonIgnoreProperties({"location.rooms", "devices.room"})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String floor;
    private Double area;
    
    @ManyToOne
    @JoinColumn(name = "location_id")
    @JsonBackReference(value = "location-rooms")
    private Location location;
    
    @OneToMany(mappedBy = "room")
    @JsonIgnoreProperties("room")
    private List<Device> devices = new ArrayList<>();
} 