<template>
  <div class="dashboard-widget tv-widget">
    <div class="widget-content">
      <!-- Для отладки -->
      <div class="debug-info" v-if="false">
        <p>Power state: {{ device?.active ? 'включен' : 'выключен' }}</p>
        <p>Channel: {{ device?.rawProperties?.tb_channel || '1' }}</p>
        <p>Volume: {{ getVolumePercent }}%</p>
      </div>
      
      <div class="tv-status" :class="{ 'tv-on': device?.active }">
        <div class="tv-screen">
          <div v-if="device?.active" class="tv-channel">
            <span>Канал: {{ device?.rawProperties?.tb_channel || '1' }}</span>
          </div>
          <div v-else class="tv-off-message">
            <span>Выключен</span>
          </div>
        </div>
      </div>
      
      <div class="tv-controls">
        <button 
          class="power-btn" 
          :class="{ 'power-on': device?.active }"
          @click="togglePower"
        >
          <i class="fas fa-power-off"></i>
        </button>
        
        <div class="channel-controls" :class="{ 'disabled': !device?.active }">
          <button @click="changeChannel(1)" :disabled="!device?.active">
            <i class="fas fa-chevron-up"></i>
          </button>
          <span>Канал</span>
          <button @click="changeChannel(-1)" :disabled="!device?.active">
            <i class="fas fa-chevron-down"></i>
          </button>
        </div>
        
        <div class="volume-controls" :class="{ 'disabled': !device?.active }">
          <button @click="changeVolume(-5)" :disabled="!device?.active">
            <i class="fas fa-volume-down"></i>
          </button>
          <div class="volume-bar">
            <div class="volume-level" :style="{ width: getVolumePercent + '%' }"></div>
          </div>
          <button @click="changeVolume(5)" :disabled="!device?.active">
            <i class="fas fa-volume-up"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import api from '../../services/api';
import { useDeviceStore } from '../../store/deviceStore';

// Store для получения устройств
const deviceStore = useDeviceStore();

// Найдем телевизор из списка устройств с нужной категорией и подтипом
const device = computed(() => 
  deviceStore.devices.find(d => 
    (d.category === 'APPLIANCES' && d.subType === 'TV') || d.type === 'TV'
  )
);

// Процент громкости для отображения индикатора
const getVolumePercent = computed(() => {
  return parseInt(device.value?.rawProperties?.tb_volume || 50);
});

// Интервал обновления
let refreshInterval = null;

onMounted(async () => {
  // Загружаем устройства, если они еще не загружены
  if (deviceStore.devices.length === 0) {
    await deviceStore.fetchDevices();
  }
  
  // Запускаем периодическое обновление каждые 5 секунд
  refreshInterval = setInterval(async () => {
    await deviceStore.fetchDevices();
  }, 5000);
});

onUnmounted(() => {
  // Очищаем таймер при размонтировании компонента
  if (refreshInterval) {
    clearInterval(refreshInterval);
  }
});

const togglePower = async () => {
  if (!device.value) return;
  
  try {
    const newState = !device.value.active;
    console.log(`Изменение состояния телевизора: ${device.value.active ? 'выкл' : 'вкл'}`);
    
    // Отправляем команду на сервер (такую же, как и на странице устройств)
    await api.devices.sendCommand(device.value.id, {
      command: 'setState',
      parameters: {
        attr_server_active: newState ? 'true' : 'false',
        tb_power: newState ? 'on' : 'off'
      }
    });
    
    // Локально обновляем значение для более быстрой реакции интерфейса
    if (device.value) {
      device.value.active = newState;
      if (device.value.rawProperties) {
        device.value.rawProperties.tb_power = newState ? 'on' : 'off';
        device.value.rawProperties.attr_server_active = newState ? 'true' : 'false';
      }
    }
    
    // Обновляем устройства через 500 мс
    setTimeout(() => deviceStore.fetchDevices(), 500);
    
    console.log(`Состояние телевизора изменено на: ${newState ? 'вкл' : 'выкл'}`);
  } catch (error) {
    console.error('Ошибка при изменении состояния телевизора:', error);
  }
};

const changeChannel = async (direction) => {
  if (!device.value || !device.value.active) return;
  
  try {
    const currentChannel = parseInt(device.value.rawProperties?.tb_channel || 1);
    let newChannel = currentChannel + direction;
    // Ограничение каналов от 1 до 99
    newChannel = Math.min(Math.max(newChannel, 1), 99);
    
    console.log(`Изменение канала с ${currentChannel} на ${newChannel}`);
    
    // Отправляем команду на сервер
    await api.devices.sendCommand(device.value.id, {
      command: 'setState',
      parameters: {
        tb_channel: String(newChannel),
        attr_server_active: 'true'
      }
    });
    
    // Локально обновляем значение для более быстрой реакции интерфейса
    if (device.value.rawProperties) {
      device.value.rawProperties.tb_channel = String(newChannel);
    }
    
    // Обновляем устройства через 500 мс
    setTimeout(() => deviceStore.fetchDevices(), 500);
    
    console.log(`Канал изменен на ${newChannel}`);
  } catch (error) {
    console.error('Ошибка при изменении канала:', error);
  }
};

const changeVolume = async (change) => {
  if (!device.value || !device.value.active) return;
  
  try {
    const currentVolume = parseInt(device.value.rawProperties?.tb_volume || 50);
    let newVolume = currentVolume + change;
    // Ограничение громкости от 0 до 100
    newVolume = Math.min(Math.max(newVolume, 0), 100);
    
    console.log(`Изменение громкости с ${currentVolume} на ${newVolume}`);
    
    // Отправляем команду на сервер
    await api.devices.sendCommand(device.value.id, {
      command: 'setState',
      parameters: {
        tb_volume: String(newVolume),
        attr_server_active: 'true'
      }
    });
    
    // Локально обновляем значение для более быстрой реакции интерфейса
    if (device.value.rawProperties) {
      device.value.rawProperties.tb_volume = String(newVolume);
    }
    
    // Обновляем устройства через 500 мс
    setTimeout(() => deviceStore.fetchDevices(), 500);
    
    console.log(`Громкость изменена на ${newVolume}`);
  } catch (error) {
    console.error('Ошибка при изменении громкости:', error);
  }
};
</script>

<style scoped>
.tv-widget {
  background-color: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  height: 100%;
}

.widget-content {
  padding: 15px;
  height: 100%;
}

.tv-status {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.tv-screen {
  width: 180px;
  height: 120px;
  background-color: #1a1a1a;
  border-radius: 5px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #333;
  overflow: hidden;
}

.tv-channel {
  color: white;
  font-size: 1rem;
  text-align: center;
}

.tv-off-message {
  color: #666;
  font-size: 0.9rem;
}

.tv-controls {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-top: 10px;
}

.power-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #f0f0f0;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.power-btn:hover {
  background-color: #e0e0e0;
  box-shadow: 0 0 8px rgba(0, 0, 0, 0.15);
  transform: scale(1.05);
}

.power-btn.power-on {
  background-color: #4CAF50;
  color: white;
}

.channel-controls, .volume-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.channel-controls button, .volume-controls button {
  border: none;
  background-color: transparent;
  font-size: 1rem;
  cursor: pointer;
  padding: 5px;
  border-radius: 50%;
  transition: all 0.2s;
}

.channel-controls button:hover, .volume-controls button:hover {
  background-color: #f0f0f0;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.1);
  transform: scale(1.1);
}

.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.volume-bar {
  width: 80px;
  height: 8px;
  background-color: #e0e0e0;
  border-radius: 4px;
  margin: 5px 0;
  overflow: hidden;
}

.volume-level {
  height: 100%;
  background-color: #4a4e69;
  border-radius: 4px;
  transition: width 0.2s;
}

.tv-on .tv-screen {
  background-color: #0a3142;
}
</style> 