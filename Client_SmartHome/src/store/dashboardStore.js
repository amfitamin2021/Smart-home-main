import { defineStore } from 'pinia'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    widgets: [],
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
    
    // Получить список доступных типов виджетов
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
    }
  },
  
  actions: {
    // Добавить новый виджет
    addWidget(type, deviceId, deviceType, settings = {}) {
      const widgetType = this.availableWidgetTypes.find(t => t.id === type);
      if (!widgetType) {
        console.error(`Тип виджета ${type} не найден`);
        return;
      }
      
      // Проверяем, нет ли уже виджета для этого устройства
      if (deviceId && this.hasWidgetForDevice(deviceId)) {
        console.warn(`Виджет для устройства ${deviceId} уже существует`);
        return;
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
      
      this.widgets.push(newWidget);
      this.saveWidgets();
      
      return newWidget;
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
        } else {
          // Если виджетов нет, добавляем телевизор по умолчанию
          this.addDefaultWidgets();
        }
      } catch (error) {
        console.error('Ошибка при загрузке виджетов:', error);
        this.addDefaultWidgets();
      }
    },
    
    // Добавить виджеты по умолчанию
    addDefaultWidgets() {
      // Начинаем с пустого массива виджетов
      this.widgets = [];
      
      // В будущем здесь можно добавить виджеты по умолчанию, если необходимо
      // Например, добавить виджет уведомлений
      this.addWidget('notifications', null, null);
    },
    
    // Сбросить все виджеты к настройкам по умолчанию
    resetToDefaults() {
      this.widgets = [];
      this.addDefaultWidgets();
      this.saveWidgets();
    }
  }
}) 