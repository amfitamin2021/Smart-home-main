<template>
  <div 
    class="device-card bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden transition-all duration-200"
    :class="{ 
      'border-blue-500 shadow-md': isActive,
      'opacity-80': !device?.online
    }"
  >
    <div class="p-3 border-b border-gray-100 flex items-center">
      <div class="flex items-center justify-center w-8 h-8 rounded-full bg-blue-100 text-blue-600 mr-2">
        <i :class="['fas', getDeviceIcon]"></i>
      </div>
      <div class="flex-1 min-w-0">
        <h3 class="text-sm font-medium text-gray-900 truncate">{{ device?.name || 'Устройство' }}</h3>
        <p class="text-xs text-gray-500 truncate" v-if="device?.room">{{ device.room }}</p>
      </div>
      <div class="flex items-center">
        <span 
          class="inline-block w-2 h-2 rounded-full mr-1"
          :class="{ 
            'bg-green-500': device?.online && device?.active,
            'bg-blue-500': device?.online && !device?.active,
            'bg-red-500': !device?.online
          }"
        ></span>
        <span class="text-xs text-gray-500">{{ device?.online ? 'Онлайн' : 'Не в сети' }}</span>
      </div>
    </div>
    
    <div class="p-3" @click="toggleActive">
      <!-- Ошибка загрузки -->
      <div v-if="error" class="p-4 text-center">
        <div class="inline-flex items-center justify-center w-12 h-12 rounded-full bg-red-100 text-red-500 mb-2">
          <i class="fas fa-exclamation-circle text-lg"></i>
        </div>
        <p class="text-sm font-medium text-gray-900">Ошибка загрузки</p>
        <p class="text-xs text-gray-500 mb-3">{{ error }}</p>
        <button @click.stop="loadDevice" class="px-3 py-1 text-xs bg-blue-600 text-white rounded-md hover:bg-blue-700">
          Повторить
        </button>
      </div>
      
      <!-- Загрузка -->
      <div v-else-if="loading" class="flex items-center justify-center p-8">
        <div class="w-8 h-8 border-t-2 border-b-2 border-blue-500 rounded-full animate-spin"></div>
      </div>
      
      <!-- Контент виджета в зависимости от типа устройства -->
      <div v-else-if="device" class="space-y-3">
        <!-- Освещение -->
        <template v-if="device.category === 'LIGHTING'">
          <div class="flex items-center justify-between">
            <div class="flex items-center">
              <div class="w-10 h-10 flex items-center justify-center">
                <i class="fas fa-lightbulb text-xl" :class="device.active ? 'text-yellow-400' : 'text-gray-300'"></i>
              </div>
              <div>
                <div class="text-sm">Состояние</div>
                <div class="text-xs font-medium">{{ device.active ? 'Включено' : 'Выключено' }}</div>
              </div>
            </div>
            
            <label class="relative inline-flex items-center cursor-pointer">
              <input 
                type="checkbox" 
                class="sr-only peer" 
                :checked="device.active" 
                @change="toggleDevice"
                :disabled="!device.online"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
        </template>
        
        <!-- Климат: датчик температуры -->
        <template v-else-if="device.category === 'CLIMATE' && (device.subType === 'TEMPERATURE_SENSOR' || device.rawProperties?.tb_temperature)">
          <div class="flex items-start">
            <div class="relative w-16 h-16 flex-shrink-0">
              <svg class="w-full h-full" viewBox="0 0 120 120">
                <circle
                  class="text-gray-100"
                  stroke-width="12"
                  stroke="currentColor"
                  fill="transparent"
                  r="54"
                  cx="60"
                  cy="60"
                />
                <circle
                  :class="getTemperatureColorClass"
                  stroke-width="12"
                  :stroke-dasharray="2 * Math.PI * 54"
                  :stroke-dashoffset="getTemperatureDashOffset"
                  stroke-linecap="round"
                  stroke="currentColor"
                  fill="transparent"
                  r="54"
                  cx="60"
                  cy="60"
                  transform="rotate(-90, 60, 60)"
                />
                <circle cx="60" cy="60" r="40" fill="white" />
                <text x="60" y="50" dominant-baseline="middle" text-anchor="middle" style="font-size: 16px">🌡️</text>
                <text x="60" y="72" dominant-baseline="middle" text-anchor="middle" :class="getTemperatureColorClass" style="font-size: 13px; font-weight: bold">
                  {{ device.rawProperties?.tb_temperature || '--' }}°C
                </text>
              </svg>
            </div>
            <div class="ml-3 flex-1">
              <div class="text-sm font-medium">{{ getTemperatureLevelText }}</div>
              <div class="text-xs text-gray-500 mt-1">Обновлено: {{ formatDate(device.rawProperties?.tb_last_updated) }}</div>
              
              <div v-if="device.rawProperties?.tb_battery" class="flex items-center mt-2">
                <span 
                  class="text-xs font-medium py-1 px-2 rounded-full flex items-center"
                  :class="getBatteryClass"
                >
                  <i class="fas fa-battery-three-quarters text-xs mr-1"></i>
                  {{ device.rawProperties.tb_battery }}%
                </span>
              </div>
            </div>
          </div>
        </template>
        
        <!-- Климат: датчик влажности -->
        <template v-else-if="device.category === 'CLIMATE' && (device.subType === 'HUMIDITY_SENSOR' || device.rawProperties?.tb_humidity !== undefined)">
          <div class="flex items-start">
            <div class="relative w-16 h-16 flex-shrink-0">
              <svg class="w-full h-full" viewBox="0 0 120 120">
                <circle
                  class="text-gray-100"
                  stroke-width="12"
                  stroke="currentColor"
                  fill="transparent"
                  r="54"
                  cx="60"
                  cy="60"
                />
                <circle
                  :class="getHumidityColorClass"
                  stroke-width="12"
                  :stroke-dasharray="2 * Math.PI * 54"
                  :stroke-dashoffset="getHumidityDashOffset"
                  stroke-linecap="round"
                  stroke="currentColor"
                  fill="transparent"
                  r="54"
                  cx="60"
                  cy="60"
                  transform="rotate(-90, 60, 60)"
                />
                <circle cx="60" cy="60" r="40" fill="white" />
                <text x="60" y="50" dominant-baseline="middle" text-anchor="middle" style="font-size: 16px">💧</text>
                <text x="60" y="72" dominant-baseline="middle" text-anchor="middle" :class="getHumidityColorClass" style="font-size: 13px; font-weight: bold">
                  {{ device.rawProperties?.tb_humidity || '--' }}%
                </text>
              </svg>
            </div>
            <div class="ml-3 flex-1">
              <div class="text-sm font-medium">{{ getHumidityLevelText }}</div>
              <div class="text-xs text-gray-500 mt-1">Обновлено: {{ formatDate(device.rawProperties?.tb_last_updated) }}</div>
              
              <div v-if="device.rawProperties?.tb_battery" class="flex items-center mt-2">
                <span 
                  class="text-xs font-medium py-1 px-2 rounded-full flex items-center"
                  :class="getBatteryClass"
                >
                  <i class="fas fa-battery-three-quarters text-xs mr-1"></i>
                  {{ device.rawProperties.tb_battery }}%
                </span>
              </div>
            </div>
          </div>
        </template>
        
        <!-- TV -->
        <template v-else-if="device.type === 'tv' || (device.category === 'APPLIANCES' && device.subType === 'TV')">
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center">
                <div class="w-10 h-10 flex items-center justify-center">
                  <i class="fas fa-tv text-xl" :class="device.active ? 'text-blue-500' : 'text-gray-300'"></i>
                </div>
                <div>
                  <div class="text-sm">Состояние</div>
                  <div class="text-xs font-medium">{{ device.active ? 'Включен' : 'Выключен' }}</div>
                </div>
              </div>
              
              <label class="relative inline-flex items-center cursor-pointer">
                <input 
                  type="checkbox" 
                  class="sr-only peer" 
                  :checked="device.active" 
                  @change="toggleDevice"
                  :disabled="!device.online"
                >
                <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>
            
            <div v-if="device.active" class="grid grid-cols-2 gap-2 mt-2">
              <div class="bg-gray-50 p-2 rounded">
                <div class="text-xs text-gray-500">Канал</div>
                <div class="text-sm font-medium">{{ device.rawProperties?.tb_channel || '1' }}</div>
              </div>
              <div class="bg-gray-50 p-2 rounded">
                <div class="text-xs text-gray-500">Источник</div>
                <div class="text-sm font-medium">{{ getSourceLabel }}</div>
              </div>
            </div>
          </div>
        </template>
        
        <!-- Стандартный виджет для других типов устройств -->
        <template v-else>
          <div class="flex items-center justify-between">
            <div class="flex items-center">
              <div class="w-10 h-10 flex items-center justify-center">
                <i class="fas fa-microchip text-xl" :class="device.active ? 'text-blue-500' : 'text-gray-300'"></i>
              </div>
              <div>
                <div class="text-sm">Состояние</div>
                <div class="text-xs font-medium">{{ device.active ? 'Включено' : 'Выключено' }}</div>
              </div>
            </div>
            
            <label class="relative inline-flex items-center cursor-pointer">
              <input 
                type="checkbox" 
                class="sr-only peer" 
                :checked="device.active" 
                @change="toggleDevice"
                :disabled="!device.online"
              >
              <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>
        </template>
      </div>
    </div>
    
    <div class="border-t border-gray-100 p-2 flex justify-end gap-2">
      <button @click.stop="refreshDevice" class="p-1 text-gray-500 hover:text-blue-600 transition-colors" title="Обновить данные">
        <i class="fas fa-sync-alt text-sm" :class="{ 'animate-spin': refreshing }"></i>
      </button>
      <button @click.stop="removeWidget" class="p-1 text-gray-500 hover:text-red-600 transition-colors" title="Удалить виджет">
        <i class="fas fa-times text-sm"></i>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useDeviceStore } from '../../store/deviceStore';
import { useDashboardStore } from '../../store/dashboardStore';

const deviceStore = useDeviceStore();
const dashboardStore = useDashboardStore();

// Пропсы
const props = defineProps({
  widgetId: {
    type: String,
    required: true
  },
  deviceId: {
    type: String,
    required: true
  }
});

// Состояние
const loading = ref(true);
const error = ref(null);
const device = ref(null);
const isActive = ref(false);
const refreshing = ref(false);

// Иконки для разных типов устройств
const getDeviceIcon = computed(() => {
  if (!device.value) return 'fa-microchip';
  
  const type = device.value.type?.toLowerCase() || '';
  const category = device.value.category || '';
  const subType = device.value.subType || '';
  
  switch(type) {
    case 'light': return 'fa-lightbulb';
    case 'thermostat': return 'fa-thermometer-half';
    case 'lock': return 'fa-lock';
    case 'camera': return 'fa-video';
    case 'tv': return 'fa-tv';
    case 'vacuum': return 'fa-broom';
    default:
      // Если тип не распознан, пробуем определить по категории
      switch(category) {
        case 'LIGHTING': return 'fa-lightbulb';
        case 'CLIMATE':
          if (subType === 'HUMIDITY_SENSOR' || device.value.rawProperties?.tb_humidity) 
            return 'fa-tint';
          if (subType === 'TEMPERATURE_SENSOR' || device.value.rawProperties?.tb_temperature) 
            return 'fa-thermometer-half';
          return 'fa-thermometer-half';
        case 'SECURITY': return 'fa-shield-alt';
        case 'APPLIANCES':
          if (subType === 'TV') return 'fa-tv';
          return 'fa-plug';
        default: return 'fa-microchip';
      }
  }
});

// Данные температуры
const getTemperatureDashOffset = computed(() => {
  if (!device.value?.rawProperties?.tb_temperature) return 2 * Math.PI * 54;
  const temperature = parseFloat(device.value.rawProperties.tb_temperature);
  const percentage = Math.min((temperature / 40) * 100, 100);
  return 2 * Math.PI * 54 - (percentage / 100) * 2 * Math.PI * 54;
});

const getTemperatureColorClass = computed(() => {
  if (!device.value?.rawProperties?.tb_temperature) return 'temp-normal';
  const temperature = parseFloat(device.value.rawProperties.tb_temperature);
  
  if (temperature < 18) return 'temp-cold';
  if (temperature <= 20) return 'temp-cool';
  if (temperature <= 24) return 'temp-normal';
  if (temperature <= 28) return 'temp-warm';
  return 'temp-hot';
});

const getTemperatureLevelText = computed(() => {
  if (!device.value?.rawProperties?.tb_temperature) return 'Нет данных';
  const temperature = parseFloat(device.value.rawProperties.tb_temperature);
  
  if (temperature < 18) return 'Холодно';
  if (temperature <= 20) return 'Прохладно';
  if (temperature <= 24) return 'Нормально';
  if (temperature <= 28) return 'Тепло';
  return 'Жарко';
});

// Данные влажности
const getHumidityDashOffset = computed(() => {
  if (!device.value?.rawProperties?.tb_humidity) return 2 * Math.PI * 54;
  
  // Явно преобразуем к числу и убеждаемся, что значение корректно
  let humidity;
  try {
    humidity = parseFloat(device.value.rawProperties.tb_humidity);
    if (isNaN(humidity)) return 2 * Math.PI * 54;
  } catch (e) {
    console.error('Ошибка при обработке значения влажности:', e);
    return 2 * Math.PI * 54;
  }
  
  const percentage = Math.min(humidity, 100);
  return 2 * Math.PI * 54 - (percentage / 100) * 2 * Math.PI * 54;
});

const getHumidityColorClass = computed(() => {
  if (!device.value?.rawProperties?.tb_humidity) return 'humidity-normal';
  
  // Явно преобразуем к числу и убеждаемся, что значение корректно
  let humidity;
  try {
    humidity = parseFloat(device.value.rawProperties.tb_humidity);
    if (isNaN(humidity)) return 'humidity-normal';
  } catch (e) {
    console.error('Ошибка при обработке значения влажности:', e);
    return 'humidity-normal';
  }
  
  if (humidity < 30) return 'humidity-dry';
  if (humidity < 40) return 'humidity-low';
  if (humidity <= 60) return 'humidity-normal';
  if (humidity <= 70) return 'humidity-high';
  return 'humidity-very-high';
});

const getHumidityLevelText = computed(() => {
  if (!device.value?.rawProperties?.tb_humidity) return 'Нет данных';
  
  // Явно преобразуем к числу и убеждаемся, что значение корректно
  let humidity;
  try {
    humidity = parseFloat(device.value.rawProperties.tb_humidity);
    if (isNaN(humidity)) return 'Нет данных';
  } catch (e) {
    console.error('Ошибка при обработке значения влажности:', e);
    return 'Нет данных';
  }
  
  if (humidity < 30) return 'Очень сухо';
  if (humidity < 40) return 'Сухо';
  if (humidity <= 60) return 'Оптимально';
  if (humidity <= 70) return 'Влажно';
  return 'Очень влажно';
});

// Данные телевизора
const getSourceLabel = computed(() => {
  if (!device.value?.rawProperties?.tb_input_source) return 'ТВ';
  const source = device.value.rawProperties.tb_input_source;
  
  switch(source) {
    case 'tv': return 'ТВ';
    case 'hdmi1': return 'HDMI 1';
    case 'hdmi2': return 'HDMI 2';
    case 'av': return 'AV';
    case 'usb': return 'USB';
    case 'smarttv': return 'Smart TV';
    default: return source;
  }
});

// Индикатор батареи
const getBatteryClass = computed(() => {
  if (!device.value?.rawProperties?.tb_battery) return 'battery-unknown';
  const battery = parseFloat(device.value.rawProperties.tb_battery);
  
  if (battery < 20) return 'battery-low';
  if (battery < 50) return 'battery-medium';
  return 'battery-high';
});

// Методы
function formatDate(dateString) {
  if (!dateString) return 'Недавно';
  
  try {
    const date = new Date(dateString);
    // Проверяем валидность даты
    if (isNaN(date.getTime())) return 'Недавно';
    
    return date.toLocaleString('ru-RU', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit'
    });
  } catch (e) {
    console.error('Ошибка при форматировании даты:', e);
    return 'Недавно';
  }
}

async function loadDevice() {
  loading.value = true;
  error.value = null;
  
  try {
    // Получаем устройство напрямую из хранилища
    const deviceData = deviceStore.getDeviceById(props.deviceId);
    
    if (!deviceData) {
      console.error(`Устройство с ID ${props.deviceId} не найдено в хранилище`);
      throw new Error('Устройство не найдено');
    }
    
    device.value = deviceData;
  } catch (err) {
    console.error('Ошибка при загрузке устройства:', err);
    error.value = err.message || 'Ошибка при загрузке устройства';
  } finally {
    loading.value = false;
  }
}

function toggleActive() {
  isActive.value = !isActive.value;
}

async function toggleDevice() {
  if (!device.value || !device.value.online) return;
  
  try {
    // Используем метод из deviceStore для переключения устройства
    await deviceStore.toggleDevice(device.value.id, !device.value.active);
    
    // Переключение успешно, синхронизируем локальный статус
    const updatedDevice = deviceStore.getDeviceById(props.deviceId);
    if (updatedDevice) {
      device.value = updatedDevice;
    }
  } catch (error) {
    console.error('Ошибка при переключении устройства:', error);
  }
}

async function handleBrightnessChange(event) {
  if (!device.value || !device.value.online || !device.value.active) return;
  
  try {
    const newBrightness = parseInt(event.target.value);
    
    // Используем метод из deviceStore для изменения яркости
    await deviceStore.setBrightness(device.value.id, newBrightness);
    
    // Обновляем локальные данные
    const updatedDevice = deviceStore.getDeviceById(props.deviceId);
    if (updatedDevice) {
      device.value = updatedDevice;
    }
  } catch (error) {
    console.error('Ошибка при изменении яркости:', error);
  }
}

async function refreshDevice() {
  if (refreshing.value) return;
  
  refreshing.value = true;
  
  try {
    await deviceStore.fetchDevices();
    
    // Обновляем локальные данные
    const updatedDevice = deviceStore.getDeviceById(props.deviceId);
    if (updatedDevice) {
      device.value = updatedDevice;
    }
  } catch (error) {
    console.error('Ошибка при обновлении устройства:', error);
  } finally {
    setTimeout(() => {
      refreshing.value = false;
    }, 500);
  }
}

function removeWidget() {
  dashboardStore.removeWidget(props.widgetId);
}

// Жизненный цикл компонента
let updateInterval = null;

onMounted(async () => {
  // Загрузка устройства
  await loadDevice();
  
  // Устанавливаем интервал обновления данных каждые 30 секунд
  updateInterval = setInterval(async () => {
    if (device.value && device.value.online) {
      // При обновлении, получаем свежие данные из хранилища
      const updatedDevice = deviceStore.getDeviceById(props.deviceId);
      if (updatedDevice) {
        device.value = updatedDevice;
      }
    }
  }, 30000);
});

onUnmounted(() => {
  if (updateInterval) {
    clearInterval(updateInterval);
  }
});

// Следим за изменениями deviceId
watch(() => props.deviceId, async (newId) => {
  if (newId !== device.value?.id) {
    await loadDevice();
  }
});
</script>

<style scoped>
/* Сохраним стили для круговых индикаторов температуры и влажности */
.temp-cold {
  color: #3b82f6; /* blue-500 */
}

.temp-cool {
  color: #60a5fa; /* blue-400 */
}

.temp-normal {
  color: #10b981; /* green-500 */
}

.temp-warm {
  color: #f59e0b; /* amber-500 */
}

.temp-hot {
  color: #ef4444; /* red-500 */
}

/* Цвета для влажности */
.humidity-dry {
  color: #ef4444; /* red-500 */
}

.humidity-low {
  color: #f59e0b; /* amber-500 */
}

.humidity-normal {
  color: #10b981; /* green-500 */
}

.humidity-high {
  color: #60a5fa; /* blue-400 */
}

.humidity-very-high {
  color: #3b82f6; /* blue-500 */
}

/* Классы для батареи */
.battery-low {
  background-color: #fee2e2; /* red-100 */
  color: #ef4444; /* red-500 */
}

.battery-medium {
  background-color: #fef3c7; /* amber-100 */
  color: #f59e0b; /* amber-500 */
}

.battery-high {
  background-color: #d1fae5; /* green-100 */
  color: #10b981; /* green-500 */
}

.battery-unknown {
  background-color: #f3f4f6; /* gray-100 */
  color: #6b7280; /* gray-500 */
}
</style> 