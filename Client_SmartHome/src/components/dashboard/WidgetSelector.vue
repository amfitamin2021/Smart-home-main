<template>
<<<<<<< HEAD
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 overflow-auto">
    <div class="bg-white rounded-lg shadow-xl w-full max-w-2xl mx-4">
      <!-- Заголовок -->
      <div class="border-b p-4 flex justify-between items-center">
        <h3 class="text-xl font-semibold text-gray-800">
          {{ step === 'category' ? 'Выбор типа устройства' : 'Выбор устройства' }}
        </h3>
        <button 
          @click="$emit('close')" 
          class="text-gray-500 hover:text-gray-700 focus:outline-none"
        >
          <i class="fas fa-times"></i>
        </button>
      </div>
      
      <!-- Шаг 1: Выбор категории -->
      <div v-if="step === 'category'" class="p-6">
        <p class="text-gray-600 mb-4">Выберите тип устройства, которое вы хотите добавить на дашборд</p>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div 
            v-for="category in availableCategories" 
            :key="category.id"
            class="border rounded-lg p-4 cursor-pointer transition-colors"
            :class="{ 'border-blue-500 bg-blue-50': selectedCategory === category.id, 
                     'hover:bg-gray-50': selectedCategory !== category.id }"
            @click="selectCategory(category.id)"
          >
            <div class="flex items-center">
              <div class="w-12 h-12 flex items-center justify-center bg-blue-100 rounded-full mr-4">
                <i :class="['fas', category.icon, 'text-blue-600 text-xl']"></i>
              </div>
              <div>
                <h4 class="font-medium text-gray-900">{{ category.name }}</h4>
                <p class="text-sm text-gray-600">{{ category.description }}</p>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Кнопки действий -->
        <div class="flex justify-end space-x-3 pt-4 border-t">
          <button 
            @click="$emit('close')" 
            class="px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg"
          >
            Отмена
          </button>
          <button 
            @click="step = 'device'" 
            class="px-4 py-2 text-white bg-blue-600 hover:bg-blue-700 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="!selectedCategory"
          >
            Далее
          </button>
        </div>
      </div>
      
      <!-- Шаг 2: Выбор устройства -->
      <div v-else-if="step === 'device'" class="p-6">
        <p class="text-gray-600 mb-4">Выберите устройство, которое хотите добавить на дашборд</p>
        
        <!-- Специальный тип виджета - безопасность -->
        <div v-if="selectedCategory === 'security'" class="mb-6 bg-blue-50 p-4 rounded-lg">
          <div class="flex items-center mb-4">
            <div class="w-10 h-10 flex items-center justify-center bg-blue-100 rounded-full mr-3">
              <i class="fas fa-shield-alt text-blue-600"></i>
            </div>
            <div>
              <h5 class="font-medium text-gray-900">Виджет безопасности</h5>
              <p class="text-sm text-gray-600">Общая информация о системе безопасности, камерах и замках</p>
            </div>
          </div>
          
          <button 
            @click="addSecurityWidget" 
            class="w-full px-4 py-2 text-white bg-blue-600 hover:bg-blue-700 rounded-lg flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed"
            :disabled="isAdding"
          >
            <i v-if="isAdding" class="fas fa-spinner fa-spin mr-2"></i>
            <span>{{ isAdding ? 'Добавление...' : 'Добавить виджет безопасности' }}</span>
          </button>
        </div>
        
        <!-- Загрузка -->
        <div v-else-if="isLoading" class="py-8 text-center">
          <div class="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-blue-500 mx-auto"></div>
          <p class="mt-3 text-gray-600">Загрузка устройств...</p>
        </div>
        
        <!-- Нет устройств -->
        <div v-else-if="devicesByCategory.length === 0" class="py-8 text-center bg-gray-50 rounded-lg mb-6">
          <i class="fas fa-exclamation-circle text-gray-400 text-3xl mb-2"></i>
          <h4 class="text-lg font-medium text-gray-700 mb-1">Нет доступных устройств</h4>
          <p class="text-gray-600">В этой категории нет устройств или они уже добавлены на дашборд</p>
        </div>
        
        <!-- Список устройств -->
        <div v-else class="space-y-3 mb-6 max-h-96 overflow-y-auto">
          <div 
            v-for="device in devicesByCategory" 
            :key="device.id"
            class="border rounded-lg p-4 cursor-pointer transition-colors"
            :class="{ 'border-blue-500 bg-blue-50': selectedDevice === device.id, 
                     'hover:bg-gray-50': selectedDevice !== device.id }"
            @click="selectDevice(device.id)"
          >
            <div class="flex items-center justify-between">
              <div class="flex items-center">
                <div class="w-10 h-10 flex items-center justify-center bg-gray-100 rounded-full mr-3">
                  <i :class="['fas', getDeviceIcon(device), 'text-gray-600']"></i>
                </div>
                <div>
                  <h5 class="font-medium text-gray-900">{{ device.name }}</h5>
                  <p class="text-sm text-gray-600">{{ device.room || 'Без комнаты' }}</p>
                </div>
              </div>
              <div class="px-3 py-1 rounded-full text-xs font-medium" 
                :class="device.online ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'">
                {{ device.online ? 'Онлайн' : 'Офлайн' }}
=======
  <div class="widget-selector">
    <div class="add-widget-button" @click="showModal = true">
      <div class="add-icon">
        <i class="fas fa-plus"></i>
      </div>
      <div class="add-text">Добавить виджет</div>
    </div>
    
    <!-- Модальное окно -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Добавить виджет</h3>
          <button class="close-btn" @click="showModal = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        
        <!-- Тип виджета -->
        <div class="modal-body">
          <div v-if="step === 1">
            <h4>Выберите тип виджета</h4>
            <div class="widget-types">
              <div 
                v-for="type in availableWidgetTypes" 
                :key="type.id"
                class="widget-type-item"
                :class="{ 
                  'disabled': !canAddWidgetType(type.id),
                  'selected': selectedWidgetType && selectedWidgetType.id === type.id 
                }"
                @click="selectWidgetType(type)"
              >
                <div class="widget-type-icon">
                  <i :class="['fas', type.icon]"></i>
                </div>
                <div class="widget-type-info">
                  <div class="widget-type-name">{{ type.name }}</div>
                  <div class="widget-type-desc">{{ type.description }}</div>
                </div>
                <div v-if="!canAddWidgetType(type.id)" class="widget-type-badge">
                  Уже добавлен
                </div>
              </div>
            </div>
          </div>
          
          <!-- Выбор устройства -->
          <div v-if="step === 2">
            <h4>Выберите устройство</h4>
            <div v-if="devicesByType.length === 0" class="no-devices">
              <p>Нет доступных устройств данного типа</p>
            </div>
            <div v-else class="device-list">
              <div 
                v-for="device in devicesByType" 
                :key="device.id"
                class="device-item"
                :class="{ 'selected': selectedDevice && selectedDevice.id === device.id }"
                @click="selectDevice(device)"
              >
                <div class="device-icon">
                  <i :class="['fas', getDeviceIcon(device)]"></i>
                </div>
                <div class="device-info">
                  <div class="device-name">{{ device.name }}</div>
                  <div class="device-location">{{ device.roomName || 'Нет местоположения' }}</div>
                </div>
                <div class="device-status" :class="device.online ? 'online' : 'offline'">
                  {{ device.online ? 'Онлайн' : 'Офлайн' }}
                </div>
              </div>
            </div>
          </div>
          
          <!-- Настройки виджета -->
          <div v-if="step === 3">
            <h4>Настройки виджета</h4>
            <div class="widget-settings">
              <div class="form-group">
                <label>Название</label>
                <input 
                  type="text" 
                  v-model="widgetSettings.title" 
                  placeholder="Название виджета"
                  class="input-field"
                >
              </div>
              
              <div class="form-group">
                <label>Обновление данных</label>
                <select v-model="widgetSettings.refreshInterval" class="select-field">
                  <option value="5000">Каждые 5 секунд</option>
                  <option value="10000">Каждые 10 секунд</option>
                  <option value="30000">Каждые 30 секунд</option>
                  <option value="60000">Каждую минуту</option>
                </select>
>>>>>>> 6c7fe1a1b428a4b484d858561b589922bdb4e9fd
              </div>
            </div>
          </div>
        </div>
        
<<<<<<< HEAD
        <!-- Кнопки действий -->
        <div class="flex justify-between space-x-3 pt-4 border-t">
          <button 
            @click="step = 'category'" 
            class="px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg"
=======
        <div class="modal-footer">
          <button 
            v-if="step > 1" 
            class="back-btn" 
            @click="step--"
>>>>>>> 6c7fe1a1b428a4b484d858561b589922bdb4e9fd
          >
            Назад
          </button>
          
<<<<<<< HEAD
          <div class="flex space-x-3">
            <button 
              @click="$emit('close')" 
              class="px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg"
            >
              Отмена
            </button>
            <button 
              @click="addDeviceWidget" 
              class="px-4 py-2 text-white bg-blue-600 hover:bg-blue-700 rounded-lg flex items-center disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="!selectedDevice || isAdding"
            >
              <i v-if="isAdding" class="fas fa-spinner fa-spin mr-2"></i>
              <span>{{ isAdding ? 'Добавление...' : 'Добавить' }}</span>
            </button>
          </div>
=======
          <button 
            v-if="step < 3" 
            class="next-btn" 
            @click="nextStep"
            :disabled="!canGoNext"
          >
            Далее
          </button>
          
          <button 
            v-if="step === 3" 
            class="add-btn" 
            @click="addWidget"
          >
            Добавить
          </button>
>>>>>>> 6c7fe1a1b428a4b484d858561b589922bdb4e9fd
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useDashboardStore } from '../../store/dashboardStore';
import { useDeviceStore } from '../../store/deviceStore';

const dashboardStore = useDashboardStore();
const deviceStore = useDeviceStore();

<<<<<<< HEAD
// Эмиты
const emit = defineEmits(['close', 'widget-added']);

// Состояние
const step = ref('category');
const selectedCategory = ref('');
const selectedDevice = ref('');
const isLoading = ref(false);
const isAdding = ref(false);

// Доступные категории устройств
const availableCategories = computed(() => [
  {
    id: 'lighting',
    name: 'Освещение',
    icon: 'fa-lightbulb',
    category: 'LIGHTING',
    description: 'Лампы, светильники и другие устройства освещения'
  },
  {
    id: 'climate',
    name: 'Климат',
    icon: 'fa-temperature-high',
    category: 'CLIMATE',
    description: 'Датчики температуры, влажности и другие климатические устройства'
  },
  {
    id: 'appliances',
    name: 'Бытовая техника',
    icon: 'fa-tv',
    category: 'APPLIANCES',
    description: 'Телевизоры, аудиосистемы и другая умная бытовая техника'
  },
  {
    id: 'security',
    name: 'Безопасность',
    icon: 'fa-shield-alt',
    category: 'SECURITY',
    description: 'Камеры, датчики движения и другие устройства безопасности'
  }
]);

// Получение устройств для выбранной категории
const devicesByCategory = computed(() => {
  if (!selectedCategory.value) return [];
  
  // Находим категорию
  const category = availableCategories.value.find(c => c.id === selectedCategory.value);
  if (!category) return [];
  
  // Фильтруем устройства по категории, исключая уже добавленные
  return deviceStore.devices.filter(device => {
    return (
      device.category === category.category && 
      !dashboardStore.hasWidgetForDevice(device.id)
    );
  });
});

// Выбор категории
function selectCategory(categoryId) {
  selectedCategory.value = categoryId;
  selectedDevice.value = '';
}

// Выбор устройства
function selectDevice(deviceId) {
  selectedDevice.value = deviceId;
}

// Получение иконки для устройства
function getDeviceIcon(device) {
  switch (device.category) {
    case 'APPLIANCES':
      if (device.subType === 'TV') return 'fa-tv';
      if (device.subType === 'AUDIO') return 'fa-volume-up';
      return 'fa-plug';
    case 'CLIMATE':
      if (device.subType === 'HUMIDITY_SENSOR') return 'fa-tint';
      if (device.subType === 'TEMPERATURE_SENSOR') return 'fa-thermometer-half';
      if (device.subType === 'THERMOSTAT') return 'fa-temperature-high';
      return 'fa-fan';
    case 'LIGHTING':
      return 'fa-lightbulb';
    case 'SECURITY':
      if (device.subType === 'CAMERA') return 'fa-video';
      if (device.subType === 'MOTION_SENSOR') return 'fa-running';
      return 'fa-shield-alt';
    default:
      return 'fa-cog';
  }
}

// Добавление виджета устройства
async function addDeviceWidget() {
  if (!selectedDevice.value || isAdding.value) return;
  
  try {
    isAdding.value = true;
    
    // Находим выбранную категорию
    const category = availableCategories.value.find(c => c.id === selectedCategory.value);
    if (!category) {
      throw new Error('Категория не найдена');
    }
    
    // Получаем информацию об устройстве
    const device = deviceStore.getDeviceById(selectedDevice.value);
    if (!device) {
      throw new Error('Устройство не найдено');
    }
    
    // Добавляем виджет через хранилище
    await dashboardStore.addDeviceWidget(selectedDevice.value);
    
    emit('widget-added');
  } catch (error) {
    console.error('Ошибка при добавлении виджета:', error);
  } finally {
    isAdding.value = false;
  }
}

// Добавление виджета безопасности
async function addSecurityWidget() {
  if (isAdding.value) return;
  
  try {
    isAdding.value = true;
    
    // Добавляем виджет безопасности через хранилище
    await dashboardStore.addSecurityWidget();
    
    emit('widget-added');
  } catch (error) {
    console.error('Ошибка при добавлении виджета безопасности:', error);
  } finally {
    isAdding.value = false;
  }
}
</script>

<style scoped>
/* Плавная анимация при показе/скрытии */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
=======
// Состояние модального окна
const showModal = ref(false);
const step = ref(1);

// Выбранный тип виджета
const selectedWidgetType = ref(null);
// Выбранное устройство
const selectedDevice = ref(null);
// Настройки виджета
const widgetSettings = ref({
  title: '',
  refreshInterval: '10000'
});

// Доступные типы виджетов
const availableWidgetTypes = computed(() => {
  return dashboardStore.getAvailableWidgetTypes;
});

// Устройства, соответствующие выбранному типу виджета
const devicesByType = computed(() => {
  if (!selectedWidgetType.value) return [];
  
  // Если выбран виджет уведомлений, не нужны устройства
  if (selectedWidgetType.value.id === 'notifications') return [];
  
  // Для остальных виджетов фильтруем по категории
  return deviceStore.devices.filter(device => {
    if (selectedWidgetType.value.category === device.category) {
      // Проверяем, нет ли уже виджета для этого устройства
      return !dashboardStore.hasWidgetForDevice(device.id);
    }
    return false;
  });
});

// Метод для получения иконки устройства
const getDeviceIcon = (device) => {
  switch (device.category) {
    case 'APPLIANCES':
      if (device.type === 'TV' || device.subType === 'TV') {
        return 'fa-tv';
      }
      return 'fa-plug';
    case 'CLIMATE':
      if (device.type === 'HUMIDITY_SENSOR' || device.subType === 'HUMIDITY_SENSOR') {
        return 'fa-tint';
      } else if (device.type === 'TEMPERATURE_SENSOR' || device.subType === 'TEMPERATURE_SENSOR') {
        return 'fa-thermometer-half';
      }
      return 'fa-cloud';
    case 'LIGHTING':
      return 'fa-lightbulb';
    default:
      return 'fa-cog';
  }
};

// Проверка возможности добавления виджета
const canAddWidgetType = (typeId) => {
  return dashboardStore.canAddWidgetType(typeId);
};

// Можно ли перейти дальше
const canGoNext = computed(() => {
  if (step.value === 1) {
    return selectedWidgetType.value !== null;
  } else if (step.value === 2) {
    return selectedDevice.value !== null;
  }
  return true;
});

// Выбор типа виджета
const selectWidgetType = (type) => {
  // Если виджет уже добавлен и не может быть добавлен повторно
  if (!canAddWidgetType(type.id)) return;
  
  selectedWidgetType.value = type;
  
  // Если для выбранного виджета не требуется устройство (виджет уведомлений)
  if (type.id === 'notifications') {
    step.value = 3;
    // Для виджета уведомлений предзаполняем название
    widgetSettings.value.title = 'Уведомления системы';
  }
};

// Выбор устройства
const selectDevice = (device) => {
  selectedDevice.value = device;
  
  // Предзаполняем настройки на основе выбранного устройства
  widgetSettings.value.title = device.name;
};

// Следующий шаг
const nextStep = () => {
  if (step.value < 3) {
    step.value++;
  }
};

// Добавление виджета
const addWidget = () => {
  if (selectedWidgetType.value) {
    const deviceId = selectedDevice.value ? selectedDevice.value.id : null;
    const deviceType = selectedDevice.value ? selectedDevice.value.type : null;
    
    dashboardStore.addWidget(
      selectedWidgetType.value.id,
      deviceId,
      deviceType,
      widgetSettings.value
    );
    
    // Сбрасываем состояние и закрываем модальное окно
    resetState();
    showModal.value = false;
  }
};

// Сброс состояния
const resetState = () => {
  step.value = 1;
  selectedWidgetType.value = null;
  selectedDevice.value = null;
  widgetSettings.value = {
    title: '',
    refreshInterval: '10000'
  };
};
</script>

<style scoped>
.widget-selector {
  display: flex;
  justify-content: center;
  align-items: center;
}

.add-widget-button {
  background-color: #f9f9f9;
  border: 2px dashed #ccc;
  border-radius: 10px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666;
}

.add-widget-button:hover {
  background-color: #f0f0f0;
  border-color: #999;
  color: #333;
}

.add-icon {
  font-size: 24px;
  margin-bottom: 10px;
}

.add-text {
  font-size: 14px;
}

/* Модальное окно */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 10px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #666;
}

.modal-body {
  padding: 20px;
}

.modal-body h4 {
  margin: 0 0 15px;
  font-size: 16px;
  font-weight: 500;
}

.modal-footer {
  padding: 15px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Типы виджетов */
.widget-types {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
}

.widget-type-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.widget-type-item:hover {
  background-color: #f9f9f9;
  border-color: #ddd;
}

.widget-type-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.widget-type-item.selected {
  border-color: #3b82f6;
  background-color: #eff6ff;
  box-shadow: 0 0 0 1px #3b82f6;
}

.widget-type-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  border-radius: 50%;
  margin-right: 15px;
  font-size: 18px;
  color: #4a4e69;
}

.widget-type-info {
  flex: 1;
}

.widget-type-name {
  font-weight: 500;
  margin-bottom: 5px;
}

.widget-type-desc {
  font-size: 13px;
  color: #666;
}

.widget-type-badge {
  position: absolute;
  top: 5px;
  right: 5px;
  background-color: #f44336;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
}

/* Список устройств */
.device-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.device-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #eee;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.device-item:hover {
  background-color: #f9f9f9;
  border-color: #ddd;
}

.device-item.selected {
  border-color: #3b82f6;
  background-color: #eff6ff;
  box-shadow: 0 0 0 1px #3b82f6;
}

.device-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  border-radius: 50%;
  margin-right: 15px;
  font-size: 18px;
  color: #4a4e69;
}

.device-info {
  flex: 1;
}

.device-name {
  font-weight: 500;
  margin-bottom: 5px;
}

.device-location {
  font-size: 13px;
  color: #666;
}

.device-status {
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 10px;
}

.device-status.online {
  background-color: #e6f7e6;
  color: #388e3c;
}

.device-status.offline {
  background-color: #ffebee;
  color: #d32f2f;
}

.no-devices {
  padding: 20px;
  text-align: center;
  color: #666;
  background-color: #f9f9f9;
  border-radius: 8px;
}

/* Настройки виджета */
.widget-settings {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-weight: 500;
  font-size: 14px;
}

.input-field, .select-field {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.input-field:focus, .select-field:focus {
  outline: none;
  border-color: #4a4e69;
}

/* Кнопки */
.back-btn, .next-btn, .add-btn {
  padding: 8px 15px;
  border-radius: 6px;
  border: none;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn {
  background-color: #f0f0f0;
  color: #333;
}

.back-btn:hover {
  background-color: #e0e0e0;
}

.next-btn, .add-btn {
  background-color: #4a4e69;
  color: white;
}

.next-btn:hover, .add-btn:hover {
  background-color: #3d405b;
}

.next-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
>>>>>>> 6c7fe1a1b428a4b484d858561b589922bdb4e9fd
}
</style> 