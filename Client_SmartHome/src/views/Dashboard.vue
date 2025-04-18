<template>
  <div class="dashboard-page h-full flex flex-col">
    <!-- Хедер страницы -->
    <div class="bg-white border-b border-gray-200 p-4 shadow-sm">
      <div class="container mx-auto">
        <div class="flex justify-between items-center">
          <h1 class="text-2xl font-semibold text-gray-800">Дашборд</h1>
          <div class="flex gap-3">
            <button 
              v-if="widgets.length > 0"
              @click="toggleEditMode" 
              class="px-4 py-2 rounded-lg flex items-center"
              :class="{ 
                'bg-yellow-500 hover:bg-yellow-600 text-white': isEditMode,
                'bg-gray-100 hover:bg-gray-200 text-gray-700': !isEditMode
              }"
            >
              <i class="fas mr-2" :class="isEditMode ? 'fa-check' : 'fa-edit'"></i>
              {{ isEditMode ? 'Сохранить' : 'Редактировать' }}
            </button>
            <button 
              @click="refreshData" 
              class="bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium px-4 py-2 rounded-lg flex items-center"
              :disabled="isRefreshing"
            >
              <i class="fas fa-sync-alt mr-2" :class="{ 'animate-spin': isRefreshing }"></i>
              Обновить
            </button>
            <button 
              @click="showWidgetSelector = true" 
              class="bg-blue-600 hover:bg-blue-700 text-white font-medium px-4 py-2 rounded-lg flex items-center"
            >
              <i class="fas fa-plus mr-2"></i>
              Добавить виджет
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Основное содержимое дашборда -->
    <div class="flex-grow bg-gray-50 p-4 overflow-auto">
      <div class="container mx-auto">
        <!-- Загрузка или ошибки -->
        <div v-if="isLoading" class="h-full flex items-center justify-center">
          <div class="text-center">
            <div class="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500 mx-auto"></div>
            <p class="mt-3 text-gray-600">Загрузка данных...</p>
          </div>
        </div>
        
        <div v-else-if="loadError" class="h-full flex items-center justify-center">
          <div class="text-center p-6 bg-white rounded-lg shadow-sm">
            <i class="fas fa-exclamation-circle text-4xl text-red-500 mb-3"></i>
            <h2 class="text-xl font-semibold text-gray-800 mb-2">Ошибка загрузки</h2>
            <p class="text-gray-600 mb-4">{{ loadError }}</p>
            <button 
              @click="retryLoading" 
              class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg"
            >
              Повторить
            </button>
          </div>
        </div>
        
        <!-- Виджеты дашборда -->
        <div v-else>
          <div v-if="widgets.length === 0" class="h-64 flex flex-col items-center justify-center p-6 bg-white rounded-lg shadow-sm">
            <i class="fas fa-th-large text-5xl text-gray-300 mb-4"></i>
            <h2 class="text-xl font-semibold text-gray-700 mb-2">Дашборд пока пуст</h2>
            <p class="text-gray-600 mb-4 text-center max-w-md">
              Добавьте виджеты для отображения ваших устройств и датчиков. Для этого нажмите кнопку "Добавить виджет".
            </p>
            <button 
              @click="showWidgetSelector = true" 
              class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center"
            >
              <i class="fas fa-plus mr-2"></i>
              Добавить виджет
            </button>
          </div>
          
          <!-- Инструкции для режима редактирования -->
          <div v-else-if="isEditMode" class="mb-4 p-3 bg-blue-50 text-blue-700 rounded-lg">
            <i class="fas fa-info-circle mr-2"></i>
            <span>Используйте кнопки со стрелками, чтобы изменить порядок виджетов. Нажмите "Сохранить" для применения изменений.</span>
          </div>
          
          <!-- Сетка виджетов (общая для обоих режимов) -->
          <div 
            v-if="widgets.length > 0" 
            class="widgets-grid"
          >
            <div 
              v-for="widget in sortedWidgets" 
              :key="widget.id" 
              class="widget-wrapper"
              :class="{ 'edit-mode': isEditMode }"
            >
              <div class="widget-container">
                <div class="widget-header">
                  <div class="flex items-center">
                    <h3 class="widget-title">{{ getDeviceName(widget.deviceId) }}</h3>
                  </div>
                  <div class="flex gap-2">
                    <button v-if="isEditMode" @click.stop="moveWidgetUp(widget.id)" class="move-btn" title="Переместить вверх">
                      <i class="fas fa-arrow-up"></i>
                    </button>
                    <button v-if="isEditMode" @click.stop="moveWidgetDown(widget.id)" class="move-btn" title="Переместить вниз">
                      <i class="fas fa-arrow-down"></i>
                    </button>
                    <button @click="removeWidget(widget.id)" class="delete-btn">
                      <i class="fas fa-times"></i>
                    </button>
                  </div>
                </div>
                <div class="widget-content">
                  <device-widget
                    :device-id="widget.deviceId"
                    :widget-id="widget.id"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Модальное окно для выбора виджета -->
    <widget-selector 
      v-if="showWidgetSelector" 
      @close="showWidgetSelector = false"
      @widget-added="onWidgetAdded"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useDashboardStore } from '../store/dashboardStore';
import { useDeviceStore } from '../store/deviceStore';
import DeviceWidget from '../components/dashboard/DeviceWidget.vue';
import WidgetSelector from '../components/dashboard/WidgetSelector.vue';

const dashboardStore = useDashboardStore();
const deviceStore = useDeviceStore();

// Состояние UI
const showWidgetSelector = ref(false);
const isLoading = ref(true);
const loadError = ref(null);
const isRefreshing = ref(false);
const isEditMode = ref(false);

// Порядок виджетов
const widgetOrder = ref({});

// Получение данных из хранилища
const widgets = computed(() => dashboardStore.widgets);

// Сортировка виджетов по их порядку
const sortedWidgets = computed(() => {
  const widgetsArray = [...widgets.value];
  return widgetsArray.sort((a, b) => {
    const orderA = widgetOrder.value[a.id] || 999;
    const orderB = widgetOrder.value[b.id] || 999;
    return orderA - orderB;
  });
});

// Функция переключения режима редактирования
function toggleEditMode() {
  // Перед переключением в обычный режим сохраняем изменения
  if (isEditMode.value) {
    saveWidgetOrder();
  }
  
  isEditMode.value = !isEditMode.value;
}

// Функции для изменения порядка виджетов
function moveWidgetUp(widgetId) {
  const currentIndex = getWidgetIndex(widgetId);
  if (currentIndex > 0) {
    // Находим предыдущий виджет
    const prevWidgetId = sortedWidgets.value[currentIndex - 1].id;
    
    // Меняем местами порядковые номера
    const currentOrder = widgetOrder.value[widgetId] || currentIndex;
    const prevOrder = widgetOrder.value[prevWidgetId] || (currentIndex - 1);
    
    widgetOrder.value[widgetId] = prevOrder;
    widgetOrder.value[prevWidgetId] = currentOrder;
  }
}

function moveWidgetDown(widgetId) {
  const currentIndex = getWidgetIndex(widgetId);
  if (currentIndex < sortedWidgets.value.length - 1) {
    // Находим следующий виджет
    const nextWidgetId = sortedWidgets.value[currentIndex + 1].id;
    
    // Меняем местами порядковые номера
    const currentOrder = widgetOrder.value[widgetId] || currentIndex;
    const nextOrder = widgetOrder.value[nextWidgetId] || (currentIndex + 1);
    
    widgetOrder.value[widgetId] = nextOrder;
    widgetOrder.value[nextWidgetId] = currentOrder;
  }
}

function getWidgetIndex(widgetId) {
  return sortedWidgets.value.findIndex(widget => widget.id === widgetId);
}

// Ключ для хранения порядка виджетов
const WIDGET_ORDER_KEY = 'dashboard_widget_order';

// Сохранение порядка виджетов
function saveWidgetOrder() {
  localStorage.setItem(WIDGET_ORDER_KEY, JSON.stringify(widgetOrder.value));
}

// Загрузка порядка виджетов
function loadWidgetOrder() {
  try {
    const savedOrder = localStorage.getItem(WIDGET_ORDER_KEY);
    if (savedOrder) {
      widgetOrder.value = JSON.parse(savedOrder);
    } else {
      // Если порядок не сохранен, инициализируем его текущими индексами
      widgets.value.forEach((widget, index) => {
        widgetOrder.value[widget.id] = index;
      });
    }
  } catch (error) {
    console.error('Ошибка при загрузке порядка виджетов:', error);
  }
}

// Интервал для автоматического обновления данных
let updateInterval = null;

// Функция для обновления данных устройств
async function refreshData() {
  if (isRefreshing.value) return;
  
  isRefreshing.value = true;
  
  try {
    // Обновляем данные устройств
    await deviceStore.fetchDevices();
    
    // Сбрасываем ошибку, если она была
    loadError.value = null;
  } catch (error) {
    console.error('Ошибка при обновлении данных:', error);
    loadError.value = 'Не удалось обновить данные устройств. Проверьте подключение к сети.';
  } finally {
    isRefreshing.value = false;
  }
}

// Загрузка данных при монтировании компонента
async function loadDashboardData() {
  try {
    isLoading.value = true;
    
    // Загрузка виджетов
    await dashboardStore.loadWidgets();
    
    // Если устройств нет, загружаем их
    if (deviceStore.devices.length === 0) {
      await deviceStore.fetchDevices();
    }
    
    // Загрузка порядка виджетов
    loadWidgetOrder();
    
    loadError.value = null;
  } catch (error) {
    console.error('Ошибка при загрузке данных:', error);
    loadError.value = 'Не удалось загрузить данные дашборда. Проверьте подключение к сети.';
  } finally {
    isLoading.value = false;
  }
}

// Повторная попытка загрузки данных
function retryLoading() {
  loadDashboardData();
}

// Получение имени устройства по его ID
function getDeviceName(deviceId) {
  const device = deviceStore.getDeviceById(deviceId);
  return device ? device.name : 'Устройство';
}

// Функции для получения размеров виджетов
function getWidgetWidth(widgetId) {
  return 300; // Фиксированная ширина
}

function getWidgetHeight(widgetId) {
  return 360; // Фиксированная высота
}

// Методы для управления виджетами
function removeWidget(widgetId) {
  dashboardStore.removeWidget(widgetId);
  
  // Удаляем также информацию о порядке
  if (widgetOrder.value[widgetId]) {
    delete widgetOrder.value[widgetId];
    saveWidgetOrder();
  }
}

function onWidgetAdded() {
  showWidgetSelector.value = false;
  
  // Получаем последний добавленный виджет и задаем ему порядок в конце списка
  if (widgets.value.length > 0) {
    const lastWidget = widgets.value[widgets.value.length - 1];
    widgetOrder.value[lastWidget.id] = Object.keys(widgetOrder.value).length;
    saveWidgetOrder();
  }
  
  // Обновляем данные устройств после добавления виджета
  refreshData();
}

// Жизненный цикл
onMounted(() => {
  // Первоначальная загрузка данных
  loadDashboardData();
  
  // Настройка автоматического обновления каждые 30 секунд
  updateInterval = setInterval(() => {
    refreshData();
  }, 30000);
});

onUnmounted(() => {
  // Очистка интервала при размонтировании компонента
  if (updateInterval) {
    clearInterval(updateInterval);
    updateInterval = null;
  }
});
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
}

.container {
  max-width: 1600px;
}

/* Сетка для виджетов */
.widgets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  grid-gap: 16px;
  margin-top: 16px;
}

.widget-wrapper {
  position: relative;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.widget-wrapper.edit-mode {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border: 2px dashed #4299e1;
}

.widget-container {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 1rem;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.widget-title {
  font-size: 0.875rem;
  font-weight: 500;
  color: #4b5563;
}

.move-btn,
.delete-btn {
  background: none;
  border: none;
  font-size: 0.875rem;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  cursor: pointer;
}

.move-btn {
  color: #4b5563;
}

.move-btn:hover {
  background-color: #e2e8f0;
}

.delete-btn {
  color: #dc3545;
}

.delete-btn:hover {
  background-color: #f8d7da;
}

.widget-content {
  padding: 0;
  flex-grow: 1;
  overflow: hidden;
}

@media (min-width: 768px) {
  .widgets-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .widgets-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1600px) {
  .widgets-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style> 