package com.example.Smarthome.service;

import com.example.Smarthome.model.ConnectionProtocol;
import com.example.Smarthome.model.Device;
import com.example.Smarthome.model.DeviceStatus;
import com.example.Smarthome.repository.DeviceRepository;
import com.example.Smarthome.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private ProtocolAdapterService protocolAdapterService;

    @Mock
    private ThingsBoardIntegrationService thingsBoardService;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private DeviceService deviceService;

    private Device testDevice;
    private UUID deviceId;

    @BeforeEach
    public void setup() {
        // Создаем тестовое устройство
        deviceId = UUID.randomUUID();
        testDevice = new Device();
        testDevice.setId(deviceId);
        testDevice.setName("Тестовое устройство");
        testDevice.setStatus(DeviceStatus.ONLINE);
        testDevice.setProtocol(ConnectionProtocol.MQTT);
        testDevice.setLastSeen(LocalDateTime.now());
        testDevice.setProperties(new HashMap<>());
        testDevice.getProperties().put("power", "on");
        testDevice.setConnectionParams("{\"ip\":\"192.168.1.100\",\"port\":\"1883\"}");
    }

    @Test
    @DisplayName("Тест получения устройства по ID")
    public void testGetDeviceById() {
        // Arrange
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));

        // Act
        Optional<Device> result = deviceService.getDeviceById(deviceId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testDevice.getName(), result.get().getName());
        assertEquals(testDevice.getStatus(), result.get().getStatus());
        verify(deviceRepository, times(1)).findById(deviceId);
    }

    @Test
    @DisplayName("Тест получения всех устройств")
    public void testGetAllDevices() {
        // Arrange
        List<Device> devices = new ArrayList<>();
        devices.add(testDevice);
        
        Device device2 = new Device();
        device2.setId(UUID.randomUUID());
        device2.setName("Устройство 2");
        device2.setStatus(DeviceStatus.OFFLINE);
        devices.add(device2);
        
        when(deviceRepository.findAll()).thenReturn(devices);

        // Act
        List<Device> result = deviceService.getAllDevices();

        // Assert
        assertEquals(2, result.size());
        verify(deviceRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Тест обновления статуса устройства")
    public void testUpdateDeviceStatus() {
        // Arrange
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        Device updatedDevice = deviceService.updateDeviceStatus(deviceId, DeviceStatus.OFFLINE);

        // Assert
        assertEquals(DeviceStatus.OFFLINE, updatedDevice.getStatus());
        verify(deviceRepository, times(1)).findById(deviceId);
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    @DisplayName("Тест обновления свойства устройства")
    public void testUpdateDeviceProperty() {
        // Arrange
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));
        when(deviceRepository.save(any(Device.class))).thenReturn(testDevice);

        // Act
        boolean result = deviceService.updateDeviceProperty(deviceId, "brightness", "80");

        // Assert
        assertTrue(result);
        assertEquals("80", testDevice.getProperties().get("brightness"));
        verify(deviceRepository, times(1)).findById(deviceId);
        verify(deviceRepository, times(1)).save(testDevice);
    }

    @Test
    @DisplayName("Тест отправки команды устройству")
    public void testSendCommandToDevice() {
        // Arrange
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(testDevice));
        when(protocolAdapterService.sendCommand(eq(testDevice), eq("setPower"), any())).thenReturn(true);

        Map<String, String> params = new HashMap<>();
        params.put("power", "off");

        // Act
        boolean result = deviceService.sendCommandToDevice(deviceId, "setPower", params);

        // Assert
        assertTrue(result);
        verify(deviceRepository, times(1)).findById(deviceId);
        verify(protocolAdapterService, times(1)).sendCommand(eq(testDevice), eq("setPower"), any());
    }

    @Test
    @DisplayName("Тест удаления устройства")
    public void testDeleteDevice() {
        // Act
        deviceService.deleteDevice(deviceId);

        // Assert
        verify(deviceRepository, times(1)).deleteById(deviceId);
    }

    @Test
    @DisplayName("Тест сохранения устройства")
    public void testSaveDevice() {
        // Arrange
        when(deviceRepository.save(testDevice)).thenReturn(testDevice);

        // Act
        Device savedDevice = deviceService.saveDevice(testDevice);

        // Assert
        assertNotNull(savedDevice);
        assertEquals(testDevice.getId(), savedDevice.getId());
        verify(deviceRepository, times(1)).save(testDevice);
    }
} 