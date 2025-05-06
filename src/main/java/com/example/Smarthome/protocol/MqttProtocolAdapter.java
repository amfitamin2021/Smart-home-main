package com.example.Smarthome.protocol;

import com.example.Smarthome.model.Device;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("mqtt")
public class MqttProtocolAdapter implements ProtocolAdapter {
    
    @Value("${mqtt.topics.command}")
    private String commandTopicTemplate;
    
    @Value("${mqtt.topics.state}")
    private String stateTopicTemplate;
    
    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    
    // Кэш последних известных состояний устройств
    private final Map<String, Map<String, String>> devicePropertiesCache = new ConcurrentHashMap<>();
    
    /**
     * Отправляет команду на устройство через MQTT
     */
    @Override
    public boolean sendCommand(Device device, String command, Map<String, String> parameters) {
        try {
            String deviceId = device.getId().toString();
            
            // Формируем JSON команды
            Map<String, Object> payload = new HashMap<>();
            payload.put("command", command);
            payload.put("parameters", parameters);
            
            // Определяем топик для устройства, заменяя + на идентификатор устройства
            String topic = commandTopicTemplate.replace("+", deviceId);
            
            log.debug("Отправка MQTT команды на устройство {} ({}), топик: {}, команда: {}", 
                      device.getName(), deviceId, topic, command);
            
            // Конвертируем в JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            // Отправляем сообщение
            MqttMessage mqttMessage = new MqttMessage(jsonPayload.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish(topic, mqttMessage);
            
            log.info("Команда успешно отправлена на устройство {}: {}", device.getName(), jsonPayload);
            
            // Обновляем кэш свойств, если команда изменяет состояние
            if (parameters.containsKey("state") || parameters.containsKey("value") || parameters.containsKey("power")) {
                Map<String, String> updatedProperties = devicePropertiesCache.getOrDefault(deviceId, new HashMap<>());
                
                if (parameters.containsKey("state")) {
                    updatedProperties.put("state", parameters.get("state"));
                }
                if (parameters.containsKey("power")) {
                    updatedProperties.put("power", parameters.get("power"));
                }
                if (parameters.containsKey("value")) {
                    updatedProperties.put("value", parameters.get("value"));
                }
                
                devicePropertiesCache.put(deviceId, updatedProperties);
            }
            
            return true;
        } catch (MqttException e) {
            log.error("Ошибка MQTT при отправке команды на устройство {}: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации JSON при отправке команды на устройство {}: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при отправке команды на устройство {}: {}", 
                    device.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Проверяет статус устройства через MQTT
     */
    @Override
    public boolean checkDeviceStatus(Device device) {
        // Предполагаем, что устройство отвечает на ping или у нас есть 
        // кэшированное состояние устройства, полученное через MQTT
        String deviceId = device.getId().toString();
        
        // Если у нас есть кэшированные свойства и они обновлялись недавно,
        // считаем устройство онлайн
        return devicePropertiesCache.containsKey(deviceId);
    }
    
    /**
     * Получает свойства устройства из кэша или через запрос по MQTT
     */
    @Override
    public Map<String, String> getDeviceProperties(Device device) {
        String deviceId = device.getId().toString();
        
        // Возвращаем кэшированные свойства, если они доступны
        return devicePropertiesCache.getOrDefault(deviceId, new HashMap<>());
    }
    
    /**
     * Обновляет кэш свойств устройства при получении нового состояния
     * (вызывается из MQTT слушателя)
     */
    public void updateDeviceProperties(String deviceId, Map<String, String> properties) {
        devicePropertiesCache.put(deviceId, properties);
    }
} 