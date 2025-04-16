<template>
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
              </div>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button 
            v-if="step > 1" 
            class="back-btn" 
            @click="step--"
          >
            Назад
          </button>
          
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
  
  switch (selectedWidgetType.value.id) {
    case 'tv':
      return deviceStore.devices.filter(d => 
        (d.category === 'APPLIANCES' && d.subType === 'TV') || d.type === 'TV'
      );
    case 'humidity':
      return deviceStore.devices.filter(d => 
        d.type === 'HUMIDITY_SENSOR' || 
        (d.category === 'CLIMATE' && d.subType === 'HUMIDITY_SENSOR')
      );
    case 'temperature':
      return deviceStore.devices.filter(d => 
        d.type === 'TEMPERATURE_SENSOR' || 
        (d.category === 'CLIMATE' && d.subType === 'TEMPERATURE_SENSOR')
      );
    default:
      return [];
  }
});

// Метод для получения иконки устройства
const getDeviceIcon = (device) => {
  if (device.type === 'TV' || (device.category === 'APPLIANCES' && device.subType === 'TV')) {
    return 'fa-tv';
  } else if (device.type === 'HUMIDITY_SENSOR' || (device.category === 'CLIMATE' && device.subType === 'HUMIDITY_SENSOR')) {
    return 'fa-tint';
  } else if (device.type === 'TEMPERATURE_SENSOR' || (device.category === 'CLIMATE' && device.subType === 'TEMPERATURE_SENSOR')) {
    return 'fa-thermometer-half';
  }
  return 'fa-cog';
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
  
  // Если для выбранного виджета не требуется устройство (виджет уведомлений или погоды)
  if (type.id === 'notifications' || type.id === 'weather') {
    step.value = 3;
    // Для виджета уведомлений предзаполняем название
    if (type.id === 'notifications') {
      widgetSettings.value.title = 'Уведомления системы';
    }
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
    dashboardStore.addWidget(
      selectedWidgetType.value.id,
      deviceId,
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
}
</style> 