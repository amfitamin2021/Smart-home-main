import { defineStore } from 'pinia'
import { useDeviceStore } from './deviceStore'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    widgets: [],
    nextWidgetId: 1,
    availableWidgetTypes: [
      { 
        id: 'appliances', 
        name: 'Бытовая техника', 
        description: 'Управление бытовой техникой', 
        icon: 'fa-tv',
        allowsMultiple: true,
        category: 'APPLIANCES'
      },
      { 
        id: 'climate', 
        name: 'Датчики климата', 
        description: 'Отображение показателей климата', 
        icon: 'fa-temperature-half',
        allowsMultiple: true,
        category: 'CLIMATE'
      },
      { 
        id: 'lighting', 
        name: 'Освещение', 
        description: 'Управление умным освещением', 
        icon: 'fa-lightbulb',
        allowsMultiple: true,
        category: 'LIGHTING'
      },
      {
        id: 'notifications',
        name: 'Уведомления',
        component: 'NotificationsWidget',
        allowsMultiple: false,
        description: 'Показывает последние уведомления системы',
        icon: 'fa-bell',
        recommended: true
      }
    ]
  }),
  
  getters: {
    // Получить список виджетов
    getWidgets: (state) => state.widgets,
    
    // Получить список доступных типов виджетов (категорий)
    getAvailableWidgetTypes: (state) => state.availableWidgetTypes,
    
    // Проверить, можно ли добавить еще виджет определенного типа
    canAddWidgetType: (state) => (typeId) => {
      const widgetType = state.availableWidgetTypes.find(type => type.id === typeId);
      if (!widgetType) return false;
      
      // Если виджет может быть добавлен многократно
      if (widgetType.allowsMultiple) return true;
      
      // Иначе проверяем, есть ли уже такой виджет
      return !state.widgets.some(widget => widget.type === typeId);
    },

    // Проверить, есть ли уже виджет для конкретного устройства
    hasWidgetForDevice: (state) => (deviceId) => {
      return state.widgets.some(widget => widget.deviceId === deviceId);
    },
    
    // Получить устройства для выбранной категории
    getDevicesForCategory: () => (category) => {
      const deviceStore = useDeviceStore();
      return deviceStore.devices.filter(device => device.category === category);
    }
  },
  
  actions: {
    // Добавить новый виджет
    addWidget(type, deviceId, deviceType, settings = {}) {
      try {
        console.log('Добавление виджета:', { type, deviceId, deviceType, settings });
        
        const widgetType = this.availableWidgetTypes.find(t => t.id === type);
        if (!widgetType) {
          console.error(`Тип виджета ${type} не найден в списке доступных типов:`, this.availableWidgetTypes);
          return null;
        }
        
        // Проверяем, нет ли уже виджета для этого устройства
        if (deviceId && this.hasWidgetForDevice(deviceId)) {
          console.warn(`Виджет для устройства ${deviceId} уже существует на дашборде`);
          return null;
        }
        
        // Определяем компонент виджета на основе типа устройства
        let component;
        switch (type) {
          case 'appliances':
            if (deviceType === 'TV') {
              component = 'TVWidget';
            } else {
              component = 'GenericApplianceWidget';
            }
            break;
          case 'climate':
            if (deviceType === 'TEMPERATURE_SENSOR') {
              component = 'TemperatureWidget';
            } else if (deviceType === 'HUMIDITY_SENSOR') {
              component = 'HumidityWidget';
            } else {
              component = 'GenericClimateWidget';
            }
            break;
          case 'lighting':
            component = 'LightWidget';
            break;
          case 'notifications':
            component = 'NotificationsWidget';
            break;
          default:
            component = 'GenericWidget';
        }
        
        const newWidget = {
          id: `widget_${Date.now()}`,
          type,
          deviceId,
          deviceType,
          settings,
          component,
          createdAt: new Date().toISOString()
        };
        
        console.log('Создан новый виджет:', newWidget);
        
        this.widgets.push(newWidget);
        this.saveWidgets();
        
        return newWidget;
      } catch (error) {
        console.error('Ошибка в методе addWidget:', error);
        return null;
      }
    },
    
    // Добавить виджет с устройством
    addDeviceWidget(deviceId) {
      try {
        console.log('Вызван метод addDeviceWidget с deviceId:', deviceId);
        
        // Получить информацию об устройстве
        const deviceStore = useDeviceStore();
        const device = deviceStore.getDeviceById(deviceId);
        
        if (!device) {
          console.error(`Устройство с ID ${deviceId} не найдено`);
          return null;
        }
        
        console.log('Найдено устройство:', device);
        
        // Определяем тип виджета на основе категории устройства
        let widgetType;
        switch (device.category) {
          case 'APPLIANCES':
            widgetType = 'appliances';
            break;
          case 'CLIMATE':
            widgetType = 'climate';
            break;
          case 'LIGHTING':
            widgetType = 'lighting';
            break;
          default:
            widgetType = 'appliances'; // По умолчанию
        }
        
        console.log(`Добавляем виджет типа ${widgetType} для устройства ${deviceId}, подтип: ${device.subType || device.type}`);
        
        // Добавляем виджет
        return this.addWidget(widgetType, deviceId, device.subType || device.type);
      } catch (error) {
        console.error('Ошибка в методе addDeviceWidget:', error);
        return null;
      }
    },
    
    // Удалить виджет
    removeWidget(widgetId) {
      const index = this.widgets.findIndex(widget => widget.id === widgetId);
      if (index !== -1) {
        this.widgets.splice(index, 1);
        this.saveWidgets();
        return true;
      }
      return false;
    },
    
    // Обновить настройки виджета
    updateWidgetSettings(widgetId, settings) {
      const widget = this.widgets.find(w => w.id === widgetId);
      if (widget) {
        widget.settings = { ...widget.settings, ...settings };
        this.saveWidgets();
        return true;
      }
      return false;
    },
    
    // Сохранить виджеты в localStorage
    saveWidgets() {
      try {
        localStorage.setItem('dashboard_widgets', JSON.stringify(this.widgets));
      } catch (error) {
        console.error('Ошибка при сохранении виджетов:', error);
      }
    },
    
    // Загрузить виджеты из localStorage
    loadWidgets() {
      try {
        const savedWidgets = localStorage.getItem('dashboard_widgets');
        if (savedWidgets) {
          this.widgets = JSON.parse(savedWidgets);
        }
      } catch (error) {
        console.error('Ошибка при загрузке виджетов:', error);
        this.resetWidgets();
      }
    },
    
    // Сбросить все виджеты к настройкам по умолчанию
    resetWidgets() {
      this.widgets = [];
      this.saveWidgets();
    },
    
    /**
     * Полностью очищает все данные о виджетах
     */
    clearWidgets() {
      this.widgets = [];
      localStorage.removeItem('dashboard_widgets');
      localStorage.removeItem('dashboard_nextWidgetId');
      this.nextWidgetId = 1;
    }
  }
}) 