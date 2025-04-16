<template>
  <div class="p-4">
    <!-- Заголовок с кнопкой добавления виджета -->
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-2xl font-medium">Умный дом</h1>
      <button 
        @click="isEditMode = !isEditMode" 
        class="px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg flex items-center text-sm"
      >
        <i class="fas fa-pencil-alt mr-2"></i>
        {{ isEditMode ? 'Готово' : 'Настроить' }}
      </button>
    </div>
    
    <!-- Сетка виджетов -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-4">
      <!-- Динамически подключаемые виджеты -->
      <template v-for="widget in widgets" :key="widget.id">
        <DynamicWidget :widget="widget" />
      </template>
      
      <!-- Кнопка добавления нового виджета -->
      <WidgetSelector v-if="widgets.length < maxWidgets" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useDashboardStore } from '../store/dashboardStore';
import { useDeviceStore } from '../store/deviceStore';
import DynamicWidget from '../components/dashboard/DynamicWidget.vue';
import WidgetSelector from '../components/dashboard/WidgetSelector.vue';

const dashboardStore = useDashboardStore();
const deviceStore = useDeviceStore();

// Режим редактирования
const isEditMode = ref(false);

// Максимальное количество виджетов
const maxWidgets = 6;

// Получаем список виджетов
const widgets = computed(() => {
  return dashboardStore.getWidgets;
});

onMounted(async () => {
  // Загружаем список виджетов
  dashboardStore.loadWidgets();
  
  // Загружаем список устройств, если они еще не загружены
  if (deviceStore.devices.length === 0) {
    await deviceStore.fetchDevices();
  }
});
</script>

<style scoped>
/* Дополнительные стили, если необходимо */
</style> 