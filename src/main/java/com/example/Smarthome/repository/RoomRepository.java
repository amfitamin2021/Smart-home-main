package com.example.Smarthome.repository;

import com.example.Smarthome.model.Room;
import com.example.Smarthome.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    List<Room> findByLocation(Location location);
} 