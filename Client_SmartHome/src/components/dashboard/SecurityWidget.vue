<template>
  <div class="security-widget h-full flex flex-col">
    <!-- Статус системы безопасности -->
    <div class="security-status p-4 border-b flex items-center justify-between">
      <div class="flex items-center">
        <div class="w-10 h-10 rounded-full flex items-center justify-center"
          :class="securityStatus.system === 'armed' ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-500'">
          <i class="fas fa-shield-alt text-lg"></i>
        </div>
        <div class="ml-3">
          <h3 class="text-sm font-medium">Система безопасности</h3>
          <p class="text-xs" :class="securityStatus.system === 'armed' ? 'text-green-600' : 'text-gray-500'">
            {{ securityStatus.system === 'armed' ? 'Активна' : 'Отключена' }}
          </p>
        </div>
      </div>
      <button @click="toggleSystem" 
        class="toggle-btn px-3 py-1 rounded-lg text-sm"
        :class="securityStatus.system === 'armed' 
          ? 'bg-red-100 text-red-600 hover:bg-red-200' 
          : 'bg-green-100 text-green-600 hover:bg-green-200'">
        {{ securityStatus.system === 'armed' ? 'Отключить' : 'Включить' }}
      </button>
    </div>
    
    <!-- Статистика и плитки -->
    <div class="security-stats grid grid-cols-2 gap-3 p-4">
      <!-- Камеры -->
      <router-link to="/security/cameras" class="security-stat-card p-3 bg-blue-50 rounded-lg border border-blue-100 hover:bg-blue-100 transition-colors">
        <div class="flex items-center mb-2">
          <div class="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-500">
            <i class="fas fa-video"></i>
          </div>
          <h4 class="font-medium ml-2 text-blue-700">Камеры</h4>
        </div>
        <div class="text-sm text-blue-800">
          <p>{{ securityStatus.activeCameras }} из {{ securityStatus.totalCameras }} активны</p>
        </div>
      </router-link>
      
      <!-- Замки -->
      <router-link to="/security/locks" class="security-stat-card p-3 bg-violet-50 rounded-lg border border-violet-100 hover:bg-violet-100 transition-colors">
        <div class="flex items-center mb-2">
          <div class="w-8 h-8 rounded-full bg-violet-100 flex items-center justify-center text-violet-500">
            <i class="fas fa-lock"></i>
          </div>
          <h4 class="font-medium ml-2 text-violet-700">Замки</h4>
        </div>
        <div class="text-sm text-violet-800">
          <p>{{ locksStatus.lockedCount }} из {{ locksStatus.totalLocks }} заблокированы</p>
        </div>
      </router-link>
      
      <!-- Движение -->
      <div class="security-stat-card p-3 rounded-lg border"
        :class="securityStatus.motion === 'detected' 
          ? 'bg-yellow-50 border-yellow-100' 
          : 'bg-green-50 border-green-100'">
        <div class="flex items-center mb-2">
          <div class="w-8 h-8 rounded-full flex items-center justify-center"
            :class="securityStatus.motion === 'detected' 
              ? 'bg-yellow-100 text-yellow-500' 
              : 'bg-green-100 text-green-500'">
            <i class="fas fa-running"></i>
          </div>
          <h4 class="font-medium ml-2"
            :class="securityStatus.motion === 'detected' 
              ? 'text-yellow-700' 
              : 'text-green-700'">
            Движение
          </h4>
        </div>
        <div class="text-sm"
          :class="securityStatus.motion === 'detected' 
            ? 'text-yellow-800' 
            : 'text-green-800'">
          <p>{{ securityStatus.motion === 'detected' ? 'Обнаружено' : 'Не обнаружено' }}</p>
        </div>
      </div>
      
      <!-- Двери и окна -->
      <div class="security-stat-card p-3 rounded-lg border"
        :class="securityStatus.doorsWindows === 'secured' 
          ? 'bg-green-50 border-green-100' 
          : 'bg-red-50 border-red-100'">
        <div class="flex items-center mb-2">
          <div class="w-8 h-8 rounded-full flex items-center justify-center"
            :class="securityStatus.doorsWindows === 'secured' 
              ? 'bg-green-100 text-green-500' 
              : 'bg-red-100 text-red-500'">
            <i class="fas fa-door-closed"></i>
          </div>
          <h4 class="font-medium ml-2"
            :class="securityStatus.doorsWindows === 'secured' 
              ? 'text-green-700' 
              : 'text-red-700'">
            Двери и окна
          </h4>
        </div>
        <div class="text-sm"
          :class="securityStatus.doorsWindows === 'secured' 
            ? 'text-green-800' 
            : 'text-red-800'">
          <p>{{ securityStatus.doorsWindows === 'secured' ? 'Закрыты' : 'Открыты' }}</p>
        </div>
      </div>
    </div>
    
    <!-- Последние события -->
    <div class="security-events flex-grow overflow-auto px-4 pb-4">
      <h3 class="text-sm font-medium mb-2 text-gray-700">Последние события</h3>
      <div v-for="event in securityEvents.slice(0, 4)" :key="event.id" 
        class="security-event-item bg-white p-2 rounded-lg shadow-sm mb-2 border-l-2"
        :class="getEventBorderClass(event.status)">
        <div class="flex justify-between text-xs mb-1">
          <span class="text-gray-500">{{ formatDate(event.timestamp) }}</span>
          <span :class="getEventStatusClass(event.status)">{{ event.status }}</span>
        </div>
        <p class="text-sm">{{ event.device }}: {{ event.event }}</p>
      </div>
      
      <router-link to="/security" class="view-all-link text-sm text-blue-600 hover:text-blue-800 flex items-center mt-2">
        <span>Просмотреть все события</span>
        <i class="fas fa-chevron-right ml-1 text-xs"></i>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// Состояние системы безопасности
const securityStatus = ref({
  system: 'armed', // armed или disarmed
  doorsWindows: 'secured', // secured или open
  activeCameras: 4,
  totalCameras: 5,
  motion: 'none' // none или detected
});

// Состояние замков
const locksStatus = ref({
  lockedCount: 2,
  totalLocks: 3
});

// События безопасности
const securityEvents = ref([
  {
    id: 1,
    timestamp: new Date(Date.now() - 300000),
    device: 'Входная дверь',
    event: 'Открыта',
    status: 'Внимание'
  },
  {
    id: 2,
    timestamp: new Date(Date.now() - 600000),
    device: 'Камера в гостиной',
    event: 'Движение обнаружено',
    status: 'Информация'
  },
  {
    id: 3,
    timestamp: new Date(Date.now() - 1800000),
    device: 'Система безопасности',
    event: 'Активирована',
    status: 'Нормально'
  },
  {
    id: 4,
    timestamp: new Date(Date.now() - 3600000),
    device: 'Датчик открытия окна',
    event: 'Активация датчика',
    status: 'Тревога'
  },
  {
    id: 5,
    timestamp: new Date(Date.now() - 7200000),
    device: 'Замок черного входа',
    event: 'Разблокирован',
    status: 'Информация'
  }
]);

// Форматирование даты
function formatDate(date) {
  return new Date(date).toLocaleString('ru-RU', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit'
  });
}

// Получение класса для статуса события
function getEventStatusClass(status) {
  switch (status) {
    case 'Тревога':
      return 'text-red-600';
    case 'Внимание':
      return 'text-yellow-600';
    case 'Информация':
      return 'text-blue-600';
    case 'Нормально':
      return 'text-green-600';
    default:
      return 'text-gray-600';
  }
}

// Получение класса для границы события
function getEventBorderClass(status) {
  switch (status) {
    case 'Тревога':
      return 'border-red-500';
    case 'Внимание':
      return 'border-yellow-500';
    case 'Информация':
      return 'border-blue-500';
    case 'Нормально':
      return 'border-green-500';
    default:
      return 'border-gray-300';
  }
}

// Переключение состояния системы безопасности
function toggleSystem() {
  securityStatus.value.system = securityStatus.value.system === 'armed' 
    ? 'disarmed' 
    : 'armed';
  
  // Добавляем событие
  securityEvents.value.unshift({
    id: Date.now(),
    timestamp: new Date(),
    device: 'Система безопасности',
    event: securityStatus.value.system === 'armed' ? 'Активирована' : 'Деактивирована',
    status: 'Нормально'
  });
}

// Имитация получения данных с сервера
function fetchSecurityData() {
  // В реальном приложении здесь был бы запрос к API
  console.log('Получение данных безопасности...');
  
  // Случайно изменим некоторые значения для демонстрации
  if (Math.random() > 0.8) {
    securityStatus.value.motion = securityStatus.value.motion === 'detected' ? 'none' : 'detected';
    
    if (securityStatus.value.motion === 'detected') {
      securityEvents.value.unshift({
        id: Date.now(),
        timestamp: new Date(),
        device: 'Датчик движения',
        event: 'Движение обнаружено',
        status: 'Внимание'
      });
    }
  }
}

onMounted(() => {
  // Симуляция периодического обновления данных
  setInterval(fetchSecurityData, 10000);
});
</script>

<style scoped>
.security-widget {
  background-color: #f9fafb;
}

.security-events {
  min-height: 100px;
}
</style> 