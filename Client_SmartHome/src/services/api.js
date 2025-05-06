import axios from 'axios'

// Используем относительный URL (будет проксирован через Vite)
const API_URL = '/api'

// Создаем экземпляр axios с базовыми настройками
const apiClient = axios.create({
  baseURL: API_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Интерцептор для добавления токена авторизации
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// Интерцептор для обработки ошибок (в том числе 401 Unauthorized)
apiClient.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      // Удаляем токен и редиректим на страницу логина
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// API-методы для устройств
const devicesApi = {
  /**
   * Получить список всех устройств
   * @returns {Promise<Array>} - массив устройств
   */
  getDevices() {
    return apiClient.get('/devices')
      .then(response => response.data)
  },

  /**
   * Получить устройство по ID
   * @param {string|number} id - идентификатор устройства
   * @returns {Promise<Object>} - данные устройства
   */
  getDevice(id) {
    return apiClient.get(`/devices/${id}`)
      .then(response => response.data)
  },

  /**
   * Создать новое устройство
   * @param {Object} deviceData - данные устройства
   * @returns {Promise<Object>} - созданное устройство
   */
  createDevice(deviceData) {
    // Проверка обязательных полей перед отправкой на сервер
    if (!deviceData.name) {
      return Promise.reject(new Error('Имя устройства не может быть пустым'));
    }
    
    if (!deviceData.type) {
      return Promise.reject(new Error('Тип устройства должен быть указан'));
    }
    
    // Сохраняем уникальный ID для проверки дубликатов
    if (!deviceData.properties) {
      deviceData.properties = {};
    }
    
    if (!deviceData.properties.device_unique_id) {
      deviceData.properties.device_unique_id = 'device_' + Date.now().toString(36) + Math.random().toString(36).substr(2, 5);
    }
    
    // Отправляем запрос на сервер
    return apiClient.post('/devices', deviceData)
      .then(response => response.data)
      .catch(error => {
        // Специальная обработка ошибок при создании устройства
        if (error.response && error.response.status === 409) {
          throw new Error('Устройство с таким именем уже существует');
        }
        throw error;
      });
  },

  /**
   * Отправить команду устройству
   * @param {string|number} id - идентификатор устройства
   * @param {Object} commandData - данные команды
   * @returns {Promise<Object>} - результат выполнения команды
   */
  sendCommand(id, commandData) {
    return apiClient.post(`/devices/${id}/command`, commandData)
      .then(response => response.data)
  },

  /**
   * Проверить состояние устройства
   * @param {string|number} id - идентификатор устройства
   * @returns {Promise<Object>} - состояние устройства
   */
  checkDeviceStatus(id) {
    return apiClient.get(`/devices/${id}/status`)
      .then(response => response.data)
  },

  /**
   * Обновить устройство
   * @param {string|number} id - идентификатор устройства
   * @param {Object} deviceData - данные устройства
   * @returns {Promise<Object>} - обновленное устройство
   */
  updateDevice(id, deviceData) {
    return apiClient.put(`/devices/${id}`, deviceData)
      .then(response => response.data)
  },

  /**
   * Удалить устройство
   * @param {string|number} id - идентификатор устройства
   * @returns {Promise<void>}
   */
  deleteDevice(id) {
    return apiClient.delete(`/devices/${id}`)
      .then(response => response.data)
  },

  /**
   * Получить список доступных устройств из ThingsBoard
   * @returns {Promise<Array>} - массив доступных устройств
   */
  getAvailableDevices() {
    return apiClient.get('/devices/available-devices')
      .then(response => response.data)
  },
  
  /**
   * Синхронизировать устройство с ThingsBoard
   * @param {string|number} id - идентификатор устройства
   * @returns {Promise<Object>} - результат синхронизации
   */
  syncThingsBoard(id) {
    return apiClient.post(`/devices/${id}/sync-thingsboard`)
      .then(response => response.data)
  },

  /**
   * Получить исторические данные влажности
   * @param {string} deviceId - ID устройства
   * @param {string} interval - Интервал (hour, day, week, month)
   * @returns {Promise<Array>} - массив данных
   */
  getHumidityHistory(deviceId, interval = 'day') {
    return apiClient.get(`/devices/${deviceId}/humidity-history`, {
      params: { interval }
    }).then(response => response.data)
  },

  /**
   * Получить исторические данные температуры
   * @param {string} deviceId - ID устройства
   * @param {string} interval - Интервал (hour, day, week, month)
   * @returns {Promise<Array>} - массив данных
   */
  getTemperatureHistory(deviceId, interval = 'day') {
    return apiClient.get(`/devices/${deviceId}/temperature-history`, {
      params: { interval }
    }).then(response => response.data)
  },

  /**
   * Получить историю замка
   * @param {string} deviceId - ID замка
   * @returns {Promise<Array>} - история замка
   */
  getLockHistory(deviceId) {
    return apiClient.get(`/devices/${deviceId}/lock-history`)
      .then(response => response.data)
  },

  /**
   * Получить общую историю всех замков
   * @returns {Promise<Array>} - история всех замков
   */
  getAllLockHistory() {
    return apiClient.get(`/devices/lock-history`)
      .then(response => response.data)
  },

  /**
   * Добавить запись в историю замка
   * @param {string} deviceId - ID замка
   * @param {Object} historyEntry - запись истории
   * @returns {Promise<Object>} - добавленная запись
   */
  addLockHistoryEntry(deviceId, historyEntry) {
    return apiClient.post(`/devices/${deviceId}/lock-history`, historyEntry)
      .then(response => response.data)
  },
  
  /**
   * Получить историю срабатываний датчика
   * @param {string} deviceId - ID датчика
   * @returns {Promise<Array>} - история датчика
   */
  getSensorHistory(deviceId) {
    return apiClient.get(`/devices/${deviceId}/sensor-history`)
      .then(response => response.data)
  },
  
  /**
   * Получить общую историю всех датчиков
   * @returns {Promise<Array>} - история всех датчиков
   */
  getAllSensorHistory() {
    return apiClient.get(`/devices/sensor-history`)
      .then(response => response.data)
  },
  
  /**
   * Добавить запись в историю датчика
   * @param {string} deviceId - ID датчика
   * @param {Object} historyEntry - запись истории
   * @returns {Promise<Object>} - добавленная запись
   */
  addSensorHistoryEntry(deviceId, historyEntry) {
    return apiClient.post(`/devices/${deviceId}/sensor-history`, historyEntry)
      .then(response => response.data)
  },
  
  /**
   * Подтвердить запись в истории датчика
   * @param {string} entryId - ID записи
   * @returns {Promise<Object>} - обновленная запись
   */
  acknowledgeSensorHistoryEntry(entryId) {
    return apiClient.put(`/devices/sensor-history/${entryId}/acknowledge`)
      .then(response => response.data)
  },
  
  /**
   * Подтвердить все записи в истории датчиков
   * @returns {Promise<Object>} - результат операции
   */
  acknowledgeAllSensorHistory() {
    return apiClient.post(`/devices/sensor-history/acknowledge-all`)
      .then(response => response.data)
  },
  
  /**
   * Подтвердить все записи в истории датчика
   * @param {string} deviceId - ID датчика
   * @returns {Promise<Object>} - результат операции
   */
  acknowledgeAllSensorHistoryForDevice(deviceId) {
    return apiClient.post(`/devices/${deviceId}/sensor-history/acknowledge-all`)
      .then(response => response.data)
  }
}

// API-методы для сценариев
const scenariosApi = {
  /**
   * Получить список всех сценариев
   * @returns {Promise<Array>} - массив сценариев
   */
  getScenarios() {
    return apiClient.get('/scenarios')
      .then(response => response.data)
  },

  /**
   * Получить сценарий по ID
   * @param {string|number} id - идентификатор сценария
   * @returns {Promise<Object>} - данные сценария
   */
  getScenario(id) {
    return apiClient.get(`/scenarios/${id}`)
      .then(response => response.data)
  },

  /**
   * Создать новый сценарий
   * @param {Object} scenarioData - данные сценария
   * @returns {Promise<Object>} - созданный сценарий
   */
  createScenario(scenarioData) {
    return apiClient.post('/scenarios', scenarioData)
      .then(response => response.data)
  },

  /**
   * Обновить сценарий
   * @param {string|number} id - идентификатор сценария
   * @param {Object} scenarioData - данные сценария
   * @returns {Promise<Object>} - обновленный сценарий
   */
  updateScenario(id, scenarioData) {
    return apiClient.put(`/scenarios/${id}`, scenarioData)
      .then(response => response.data)
  },

  /**
   * Удалить сценарий
   * @param {string|number} id - идентификатор сценария
   * @returns {Promise<void>}
   */
  deleteScenario(id) {
    return apiClient.delete(`/scenarios/${id}`)
      .then(response => response.data)
  },

  /**
   * Запустить сценарий
   * @param {string|number} id - идентификатор сценария
   * @returns {Promise<Object>} - результат выполнения
   */
  runScenario(id) {
    return apiClient.post(`/scenarios/${id}/run`)
      .then(response => response.data)
  }
}

// API-методы для уведомлений
const notificationsApi = {
  /**
   * Получить список уведомлений
   * @param {string} status - статус уведомлений (all, read, unread)
   * @returns {Promise<Array>} - массив уведомлений
   */
  getNotifications(status = 'all') {
    return apiClient.get(`/notifications?status=${status}`)
      .then(response => response.data)
  },

  /**
   * Пометить уведомление как прочитанное
   * @param {string|number} id - идентификатор уведомления
   * @returns {Promise<Object>} - обновленное уведомление
   */
  markAsRead(id) {
    return apiClient.put(`/notifications/${id}/read`)
      .then(response => response.data)
  },

  /**
   * Удалить уведомление
   * @param {string|number} id - идентификатор уведомления
   * @returns {Promise<void>}
   */
  deleteNotification(id) {
    return apiClient.delete(`/notifications/${id}`)
      .then(response => response.data)
  }
}

// API-методы для пользователя
const userApi = {
  /**
   * Получить информацию о текущем пользователе
   * @returns {Promise<Object>} - данные пользователя
   */
  getCurrentUser() {
    return apiClient.get('/users/me')
      .then(response => response.data)
  },

  /**
   * Обновить данные пользователя
   * @param {Object} userData - данные пользователя
   * @returns {Promise<Object>} - обновленные данные пользователя
   */
  updateUser(userData) {
    return apiClient.put('/users/me', userData)
      .then(response => response.data)
  },

  /**
   * Вход пользователя
   * @param {Object} credentials - учетные данные
   * @returns {Promise<Object>} - токен и данные пользователя
   */
  login(credentials) {
    return apiClient.post('/auth/login', credentials)
      .then(response => {
        if (response.data.token) {
          localStorage.setItem('token', response.data.token)
          localStorage.setItem('user', JSON.stringify(response.data))
        }
        return response.data
      })
  },

  /**
   * Выход пользователя
   * @returns {Promise<void>}
   */
  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  },

  /**
   * Регистрация нового пользователя
   * @param {Object} userData - данные пользователя
   * @returns {Promise<Object>} - созданный пользователь
   */
  register(userData) {
    return apiClient.post('/auth/register', userData)
      .then(response => response.data)
  }
}

// API-методы для статистики
const statsApi = {
  /**
   * Получить данные по потреблению энергии
   * @param {string} period - период (day, week, month)
   * @returns {Promise<Object>} - данные по потреблению
   */
  getEnergyConsumption(period = 'day') {
    return apiClient.get(`/stats/energy/${period}`)
      .then(response => response.data)
  },

  /**
   * Получить историю активности устройств
   * @param {Object} params - параметры запроса (deviceId, startDate, endDate)
   * @returns {Promise<Array>} - история активности
   */
  getDeviceHistory(params = {}) {
    return apiClient.get('/stats/device-history', { params })
      .then(response => response.data)
  },

  /**
   * Получить сводную статистику системы
   * @returns {Promise<Object>} - сводная статистика
   */
  getDashboardStats() {
    return apiClient.get('/stats/dashboard')
      .then(response => response.data)
  },

  /**
   * Получить статистику потребления
   * @param {string} period - период (day, week, month, year)
   * @returns {Promise<Object>} - статистика
   */
  getConsumptionStats(period = 'day') {
    return apiClient.get(`/statistics/consumption?period=${period}`)
      .then(response => response.data)
  },

  /**
   * Получить статистику использования устройств
   * @returns {Promise<Object>} - статистика
   */
  getDeviceUsageStats() {
    return apiClient.get('/statistics/devices/usage')
      .then(response => response.data)
  }
}

// API-методы для локаций (комнат)
const locationsApi = {
  /**
   * Получить список всех локаций
   * @returns {Promise<Array>} - список локаций
   */
  getLocations() {
    return apiClient.get('/locations')
      .then(response => response.data)
  },

  /**
   * Получить локацию по ID
   * @param {string|number} id - идентификатор локации
   * @returns {Promise<Object>} - данные локации
   */
  getLocation(id) {
    return apiClient.get(`/locations/${id}`)
      .then(response => response.data)
  },

  /**
   * Создать новую локацию
   * @param {Object} locationData - данные локации
   * @returns {Promise<Object>} - созданная локация
   */
  createLocation(locationData) {
    return apiClient.post('/locations', locationData)
      .then(response => response.data)
  },

  /**
   * Обновить локацию
   * @param {string|number} id - идентификатор локации
   * @param {Object} locationData - новые данные
   * @returns {Promise<Object>} - обновленная локация
   */
  updateLocation(id, locationData) {
    return apiClient.put(`/locations/${id}`, locationData)
      .then(response => response.data)
  },

  /**
   * Удалить локацию
   * @param {string|number} id - идентификатор локации
   * @returns {Promise<void>}
   */
  deleteLocation(id) {
    return apiClient.delete(`/locations/${id}`)
      .then(response => response.data)
  },
  
  /**
   * Получить комнаты для локации
   * @param {string|number} locationId - ID локации
   * @returns {Promise<Array>} - список комнат
   */
  getRooms(locationId) {
    return apiClient.get(`/locations/${locationId}/rooms`)
      .then(response => response.data)
  },
  
  /**
   * Создать комнату в локации
   * @param {string|number} locationId - ID локации
   * @param {Object} roomData - данные комнаты
   * @returns {Promise<Object>} - созданная комната
   */
  createRoom(locationId, roomData) {
    return apiClient.post(`/locations/${locationId}/rooms`, roomData)
      .then(response => response.data)
  },
  
  /**
   * Обновить комнату
   * @param {string|number} locationId - ID локации
   * @param {string|number} roomId - ID комнаты
   * @param {Object} roomData - новые данные
   * @returns {Promise<Object>} - обновленная комната
   */
  updateRoom(locationId, roomId, roomData) {
    return apiClient.put(`/locations/${locationId}/rooms/${roomId}`, roomData)
      .then(response => response.data)
  },
  
  /**
   * Удалить комнату
   * @param {string|number} locationId - ID локации
   * @param {string|number} roomId - ID комнаты
   * @returns {Promise<void>}
   */
  deleteRoom(locationId, roomId) {
    return apiClient.delete(`/locations/${locationId}/rooms/${roomId}`)
      .then(response => response.data)
  }
}

// Объединяем все API-методы в один объект
export default {
  devices: devicesApi,
  scenarios: scenariosApi,
  notifications: notificationsApi,
  user: userApi,
  stats: statsApi,
  locations: locationsApi
}

// Отдельные экспорты для прямого импорта
export { devicesApi, scenariosApi, notificationsApi, userApi, statsApi, locationsApi }