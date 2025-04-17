<template>
  <div class="dashboard-page h-full flex flex-col">
    <!-- Хедер страницы -->
    <div class="bg-white border-b border-gray-200 p-4 shadow-sm">
      <div class="container mx-auto">
        <div class="flex justify-between items-center">
          <h1 class="text-2xl font-semibold text-gray-800">Дашборд</h1>
          <div class="flex gap-3">
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
          <div 
            v-else 
            class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4"
          >
            <div 
              v-for="widget in widgets" 
              :key="widget.id" 
              class="relative"
            >
              <device-widget 
                :device-id="widget.deviceId" 
                :widget-id="widget.id"
              />
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
import { ref, computed, onMounted, onUnmounted } from 'vue';
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

// Получение данных из хранилища
const widgets = computed(() => dashboardStore.widgets);

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

// Методы для управления виджетами
function removeWidget(widgetId) {
  dashboardStore.removeWidget(widgetId);
}

function onWidgetAdded() {
  showWidgetSelector.value = false;
  
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

@media (min-width: 1600px) {
  .xl\:grid-cols-5 {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}
</style> 