<template>
  <div class="notifications-widget">
    <div class="widget-header">
      <h3><i class="fas fa-bell"></i> Уведомления</h3>
    </div>
    <div class="widget-content">
      <div v-if="loading" class="loading-indicator">
        <i class="fas fa-spinner fa-spin"></i> Загрузка...
      </div>
      <div v-else-if="notifications.length === 0" class="no-notifications">
        <i class="fas fa-check-circle"></i> Нет новых уведомлений
      </div>
      <div v-else class="notifications-list">
        <div 
          v-for="notification in notifications" 
          :key="notification.id" 
          class="notification-item"
          :class="{'notification-critical': notification.level === 'critical',
                  'notification-warning': notification.level === 'warning',
                  'notification-info': notification.level === 'info'}"
        >
          <div class="notification-icon">
            <i :class="getNotificationIcon(notification.level)"></i>
          </div>
          <div class="notification-content">
            <div class="notification-title">{{ notification.title }}</div>
            <div class="notification-message">{{ notification.message }}</div>
            <div class="notification-time">{{ formatTime(notification.timestamp) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useToast } from 'vue-toastification';

const toast = useToast();
const loading = ref(true);
const notifications = ref([]);

// Пример данных уведомлений
const demoNotifications = [
  {
    id: 1,
    level: 'critical',
    title: 'Критическая температура',
    message: 'Температура в спальне превысила допустимый порог: 31°C',
    timestamp: Date.now() - 1800000 // 30 минут назад
  },
  {
    id: 2,
    level: 'warning',
    title: 'Датчик движения',
    message: 'Обнаружено движение в гостиной во время вашего отсутствия',
    timestamp: Date.now() - 3600000 // 1 час назад
  },
  {
    id: 3,
    level: 'info',
    title: 'Обновление системы',
    message: 'Система умного дома успешно обновлена до версии 2.4.1',
    timestamp: Date.now() - 86400000 // 1 день назад
  }
];

const getNotificationIcon = (level) => {
  switch (level) {
    case 'critical':
      return 'fas fa-exclamation-circle';
    case 'warning':
      return 'fas fa-exclamation-triangle';
    case 'info':
      return 'fas fa-info-circle';
    default:
      return 'fas fa-bell';
  }
};

const formatTime = (timestamp) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffInHours = Math.floor((now - date) / (1000 * 60 * 60));
  
  if (diffInHours < 24) {
    // В течение последних 24 часов
    if (diffInHours === 0) {
      const diffInMinutes = Math.floor((now - date) / (1000 * 60));
      return `${diffInMinutes} мин. назад`;
    } else {
      return `${diffInHours} ч. назад`;
    }
  } else {
    // Более 24 часов назад - показываем дату
    return date.toLocaleString('ru-RU', {
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
};

onMounted(() => {
  // Имитация загрузки данных с API
  setTimeout(() => {
    notifications.value = demoNotifications;
    loading.value = false;
  }, 1500);
});
</script>

<style scoped>
.notifications-widget {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 10px;
  background-color: #ffffff;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.widget-header {
  padding: 15px;
  background-color: #4a6fff;
  color: white;
}

.widget-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  font-size: 16px;
}

.widget-header h3 i {
  margin-right: 8px;
}

.widget-content {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.loading-indicator {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #666;
}

.no-notifications {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #666;
}

.no-notifications i {
  font-size: 32px;
  color: #4CAF50;
  margin-bottom: 10px;
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notification-item {
  display: flex;
  padding: 12px;
  border-radius: 8px;
  background-color: #f5f7ff;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #edf0ff;
}

.notification-critical {
  border-left: 4px solid #f44336;
}

.notification-warning {
  border-left: 4px solid #ff9800;
}

.notification-info {
  border-left: 4px solid #2196f3;
}

.notification-icon {
  margin-right: 12px;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.notification-critical .notification-icon {
  color: #f44336;
}

.notification-warning .notification-icon {
  color: #ff9800;
}

.notification-info .notification-icon {
  color: #2196f3;
}

.notification-content {
  flex: 1;
}

.notification-title {
  font-weight: bold;
  margin-bottom: 5px;
}

.notification-message {
  font-size: 14px;
  color: #333;
  margin-bottom: 5px;
}

.notification-time {
  font-size: 12px;
  color: #666;
}
</style> 