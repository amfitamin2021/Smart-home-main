<template>
  <div class="bg-white p-6 rounded-xl shadow-sm">
    <div class="flex justify-between items-center mb-4">
      <div>
        <p class="text-sm text-gray-500">ПКУ</p>
        <div class="flex items-center gap-2">
          <i class="text-yellow-500 fas fa-lightbulb"></i>
          <h3 class="text-lg font-semibold">Умная лампа</h3>
        </div>
      </div>
    </div>

    <div class="flex justify-between items-center mb-4">
      <p class="text-gray-500">Состояние</p>
      <label class="relative inline-flex items-center cursor-pointer">
        <input type="checkbox" v-model="isEnabled" class="sr-only peer" @change="toggleState">
        <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
      </label>
    </div>

    <div class="relative mt-5 mb-5">
      <div class="flex justify-center">
        <div class="light-visualization">
          <div class="bulb-container">
            <!-- Основа лампы -->
            <div class="bulb-base"></div>
            
            <!-- Колба лампы -->
            <div 
              class="bulb" 
              :class="{ 'active': isEnabled }"
              :style="bulbStyle"
            >
              <!-- Внутреннее свечение -->
              <div 
                class="bulb-glow"
                :style="glowStyle"
              ></div>
              
              <!-- Отражения -->
              <div class="bulb-reflection"></div>
            </div>
            
            <!-- Эффект свечения вокруг -->
            <div 
              class="light-effect"
              :style="lightEffectStyle"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-6" v-if="isEnabled">
      <div class="mb-4">
        <div class="flex items-center justify-between mb-2">
          <span class="text-gray-500">Яркость</span>
          <span class="text-sm font-medium">{{ brightness }}%</span>
        </div>
        <input 
          type="range" 
          min="10" 
          max="100" 
          step="5" 
          v-model.number="brightness"
          @change="updateBrightness" 
          class="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
        >
      </div>
      
      <div>
        <div class="flex items-center justify-between mb-2">
          <span class="text-gray-500">Цвет</span>
          <div 
            class="w-6 h-6 rounded-full border border-gray-300" 
            :style="{ backgroundColor: color }"
          ></div>
        </div>
        <input 
          type="color" 
          v-model="color"
          @change="updateColor" 
          class="w-full h-8 rounded cursor-pointer"
        >
      </div>
    </div>

    <div class="flex items-center justify-between mt-4">
      <p class="text-sm text-gray-500">Обновлено:</p>
      <p class="flex items-center gap-1 text-sm">
        {{ formatLastUpdated(device?.rawProperties?.tb_last_updated) }}
        <i class="fas fa-sync-alt cursor-pointer hover:text-blue-600" @click="refreshData"></i>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { formatDistanceToNow } from 'date-fns';
import { ru } from 'date-fns/locale';
import { useDeviceStore } from '../../store/deviceStore';

const props = defineProps({
  deviceId: {
    type: String,
    required: true
  }
});

const deviceStore = useDeviceStore();
const device = computed(() => deviceStore.getDeviceById(props.deviceId));
const isEnabled = ref(false);
const brightness = ref(100);
const color = ref('#FFFFFF');
const updateInterval = ref(null);

// Преобразуем HEX в RGBA
const hexToRgba = (hex, alpha = 1) => {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
};

// Яркость в диапазоне 0-1 для стилей
const normalizedBrightness = computed(() => {
  return isEnabled.value ? Math.max(0.1, brightness.value / 100) : 0;
});

// Стили для визуализации лампы
const bulbStyle = computed(() => {
  if (!isEnabled.value) return {};
  
  return {
    backgroundColor: hexToRgba(color.value, normalizedBrightness.value * 0.4),
    boxShadow: `0 0 ${normalizedBrightness.value * 20}px ${normalizedBrightness.value * 10}px ${hexToRgba(color.value, normalizedBrightness.value * 0.3)}`
  };
});

const glowStyle = computed(() => {
  if (!isEnabled.value) return { opacity: 0 };
  
  return {
    backgroundColor: color.value,
    opacity: normalizedBrightness.value
  };
});

const lightEffectStyle = computed(() => {
  if (!isEnabled.value) return { opacity: 0 };
  
  return {
    backgroundColor: hexToRgba(color.value, 0.05),
    boxShadow: `0 0 ${normalizedBrightness.value * 60}px ${normalizedBrightness.value * 30}px ${hexToRgba(color.value, normalizedBrightness.value * 0.2)}`,
    opacity: normalizedBrightness.value
  };
});

// Переключение состояния лампы
const toggleState = async () => {
  try {
    await deviceStore.toggleDevice(props.deviceId, isEnabled.value);
  } catch (error) {
    console.error('Ошибка при переключении устройства:', error);
    isEnabled.value = !isEnabled.value;
  }
};

// Обновление яркости
const updateBrightness = async () => {
  try {
    await deviceStore.updateLightProperties(props.deviceId, {
      brightness: brightness.value
    });
  } catch (error) {
    console.error('Ошибка при обновлении яркости:', error);
  }
};

// Обновление цвета
const updateColor = async () => {
  try {
    const colorValue = color.value.substring(1); // Убрать #
    await deviceStore.updateLightProperties(props.deviceId, {
      color: colorValue
    });
  } catch (error) {
    console.error('Ошибка при обновлении цвета:', error);
  }
};

// Форматирование времени последнего обновления
const formatLastUpdated = (timestamp) => {
  if (!timestamp) return 'Нет данных';
  return formatDistanceToNow(new Date(timestamp), { addSuffix: true, locale: ru });
};

// Обновление данных
const refreshData = () => {
  deviceStore.fetchDevices();
};

// Автоматическое обновление данных
const startAutoUpdate = () => {
  stopAutoUpdate();
  updateInterval.value = setInterval(() => {
    deviceStore.fetchDevices();
  }, 5000);
};

const stopAutoUpdate = () => {
  if (updateInterval.value) {
    clearInterval(updateInterval.value);
    updateInterval.value = null;
  }
};

// Следим за изменениями в устройстве
watch(() => device.value, (newDevice) => {
  if (newDevice) {
    isEnabled.value = newDevice.active;
    brightness.value = newDevice.brightness || 100;
    color.value = newDevice.color || '#FFFFFF';
  }
}, { immediate: true });

// Инициализация при монтировании
onMounted(() => {
  startAutoUpdate();
  if (device.value) {
    isEnabled.value = device.value.active;
    brightness.value = device.value.brightness || 100;
    color.value = device.value.color || '#FFFFFF';
  }
});

// Очистка при размонтировании
onUnmounted(() => {
  stopAutoUpdate();
});
</script>

<style scoped>
.light-visualization {
  position: relative;
  width: 100px;
  height: 150px;
  margin: 0 auto;
}

.bulb-container {
  position: relative;
  width: 80px;
  height: 120px;
  margin: 0 auto;
}

.bulb-base {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 30px;
  height: 20px;
  background: linear-gradient(to bottom, #a1a1a1, #d4d4d4);
  border-radius: 4px 4px 6px 6px;
}

.bulb {
  position: absolute;
  bottom: 15px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 80px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  transition: all 0.3s ease;
}

.bulb.active {
  background-color: rgba(255, 255, 150, 0.4);
  box-shadow: 0 0 20px 10px rgba(255, 255, 150, 0.3);
}

.bulb-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background-color: white;
  border-radius: 50%;
  opacity: 0;
  transition: all 0.3s ease;
}

.bulb-reflection {
  position: absolute;
  top: 15px;
  left: 15px;
  width: 10px;
  height: 10px;
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 50%;
}

.light-effect {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 120px;
  height: 120px;
  background-color: rgba(255, 255, 150, 0.05);
  border-radius: 50%;
  opacity: 0;
  z-index: -1;
  transition: all 0.3s ease;
}
</style> 