import { defineStore } from 'pinia'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    widgets: [],
    availableWidgetTypes: [
      { 
        id: 'tv', 
        name: 'Телевизор', 
        description: 'Управление телевизором', 
        icon: 'fa-tv',
        component: 'TVWidget',
        allowsMultiple: false
      },
      { 
        id: 'humidity', 
        name: 'Датчик влажности', 
        description: 'Отображение показателей влажности', 
        icon: 'fa-tint',
        component: 'HumidityWidget',
        allowsMultiple: false
      },
      { 
        id: 'temperature', 
        name: 'Датчик температуры', 
        description: 'Отображение показателей температуры', 
        icon: 'fa-thermometer-half',
        component: 'TemperatureWidget',
        allowsMultiple: false
      },
      {
        id: 'notifications',
        name: 'Уведомления',
        component: 'NotificationsWidget',
        allowsMultiple: false,
        description: 'Показывает последние уведомления системы',
        icon: 'fa-bell',
        recommended: true
      },
      { 
        id: 'smartLight', 
        name: 'Умная лампа', 
        description: 'Управление умной лампой', 
        icon: 'fa-lightbulb',
        component: 'LightWidget',
        allowsMultiple: false
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
    }
  },
  
  actions: {
    // Добавить новый виджет
    addWidget(type, deviceId, settings = {}) {
      const widgetType = this.availableWidgetTypes.find(t => t.id === type);
      if (!widgetType) {
        console.error(`Тип виджета ${type} не найден`);
        return;
      }
      
      // Проверяем, можно ли добавить еще один такой виджет
      if (!this.canAddWidgetType(type)) {
        console.warn(`Виджет типа ${type} уже добавлен и не может быть добавлен повторно`);
        return;
      }
      
      const newWidget = {
        id: `widget_${Date.now()}`,
        type,
        deviceId,
        settings,
        component: widgetType.component,
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
      // Добавляем только телевизор, так как он у нас уже функционирует
      this.widgets = [];
      
      // В будущем здесь можно добавить больше виджетов по умолчанию
      this.addWidget('tv', null);
    },
    
    // Сбросить все виджеты к настройкам по умолчанию
    resetToDefaults() {
      this.widgets = [];
      this.addDefaultWidgets();
      this.saveWidgets();
    }
  }
}) 