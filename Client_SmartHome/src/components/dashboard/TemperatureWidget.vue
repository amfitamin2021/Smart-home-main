<template>
  <div class="bg-white p-6 rounded-xl shadow-sm">
    <div class="flex justify-between items-center mb-4">
      <div>
        <p class="text-sm text-gray-500">ПКУ</p>
        <div class="flex items-center gap-2">
          <i class="text-blue-600 fas fa-microchip"></i>
          <h3 class="text-lg font-semibold">Датчик температуры</h3>
        </div>
      </div>
    </div>

    <div class="flex justify-between items-center mb-4">
      <p class="text-gray-500">Состояние</p>
      <label class="relative inline-flex items-center cursor-pointer">
        <input type="checkbox" v-model="isEnabled" class="sr-only peer" @change="toggleState">
        <div class="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
      </label>
    </div>

    <div class="flex items-start gap-3">
      <div class="relative w-24 h-24">
        <svg class="w-full h-full" viewBox="0 0 120 120">
          <circle
            class="text-gray-100"
            stroke-width="12"
            stroke="currentColor"
            fill="transparent"
            r="54"
            cx="60"
            cy="60"
          />
          <circle
            class="text-red-600"
            stroke-width="12"
            :stroke-dasharray="circumference"
            :stroke-dashoffset="dashOffset"
            stroke-linecap="round"
            stroke="currentColor"
            fill="transparent"
            r="54"
            cx="60"
            cy="60"
            transform="rotate(-90, 60, 60)"
          />
          <circle cx="60" cy="60" r="40" fill="white" />
          <text x="60" y="52" dominant-baseline="middle" text-anchor="middle" style="font-size: 24px">🌡️</text>
          <text x="60" y="75" dominant-baseline="middle" text-anchor="middle" :fill="temperatureStatus.color.replace('text-', '')" style="font-size: 16px; font-weight: bold">
            {{ device?.rawProperties?.tb_temperature || '--' }}°C
          </text>
        </svg>
      </div>

      <div class="flex-grow">
        <p class="font-medium text-base mb-1">{{ temperatureStatus.text }}</p>
        <p class="text-sm text-gray-500 mb-3">
          {{ temperatureStatus.recommendation }}
        </p>

        <div class="flex items-center justify-between mt-2">
          <p class="text-sm text-gray-500">Заряд:</p>
          <p class="flex items-center gap-1 text-green-500 text-sm bg-green-50 px-2 py-0.5 rounded-full">
            <i class="fas fa-battery-three-quarters text-xs"></i>
            {{ device?.rawProperties?.tb_battery || '--' }}%
          </p>
        </div>

        <div class="flex items-center justify-between mt-2">
          <p class="text-sm text-gray-500">Обновлено:</p>
          <p class="flex items-center gap-1 text-sm">
            {{ formatLastUpdated(device?.rawProperties?.tb_last_updated) }}
            <i class="fas fa-sync-alt cursor-pointer hover:text-blue-600" @click="refreshData"></i>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { useDeviceStore } from '@/store/deviceStore'
import { formatDistanceToNow } from 'date-fns'
import { ru } from 'date-fns/locale'

export default {
  name: 'TemperatureWidget',
  props: {
    deviceId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      isEnabled: false,
      circumference: 2 * Math.PI * 54,
      selectedPeriod: '24ч',
      temperatureDelta: -0.5,
      updateInterval: null
    }
  },
  computed: {
    device() {
      return useDeviceStore().getDeviceById(this.deviceId)
    },
    dashOffset() {
      const temperature = this.device?.rawProperties?.tb_temperature || 0
      const percentage = Math.min((temperature / 40) * 100, 100)
      return this.circumference - (percentage / 100) * this.circumference
    },
    temperatureStatus() {
      const temp = this.device?.rawProperties?.tb_temperature
      if (!temp) return {
        text: 'Нет данных',
        color: 'text-gray-500',
        recommendation: 'Проверьте подключение датчика',
        title: 'Данные отсутствуют',
        detailedMessage: 'Устройство не передает данные о температуре'
      }
      
      if (temp > 28) {
        return {
          text: 'Слишком жарко',
          color: 'text-red-500',
          recommendation: 'Рекомендуется включить кондиционер',
          title: 'Высокая температура',
          detailedMessage: 'Текущий уровень температуры может вызвать дискомфорт. Рекомендуется включить кондиционер или открыть окно.'
        }
      }
      if (temp < 18) {
        return {
          text: 'Слишком холодно',
          color: 'text-blue-500',
          recommendation: 'Рекомендуется включить обогреватель',
          title: 'Низкая температура',
          detailedMessage: 'Текущий уровень температуры может вызвать дискомфорт. Рекомендуется включить обогреватель.'
        }
      }
      return {
        text: 'Оптимальная температура',
        color: 'text-green-500',
        recommendation: 'Температура в пределах нормы',
        title: 'Оптимальная температура',
        detailedMessage: 'Текущий уровень температуры оптимален для здоровья и комфорта человека.'
      }
    }
  },
  methods: {
    async toggleState() {
      try {
        await useDeviceStore().toggleDevice(this.deviceId, this.isEnabled);
      } catch (error) {
        console.error('Ошибка при обновлении состояния:', error);
        this.isEnabled = !this.isEnabled;
      }
    },
    formatLastUpdated(timestamp) {
      if (!timestamp) return 'Нет данных'
      return formatDistanceToNow(new Date(timestamp), { addSuffix: true, locale: ru })
    },
    refreshData() {
      useDeviceStore().fetchDevices()
    },
    setChartPeriod(period) {
      this.selectedPeriod = period
    },
    startAutoUpdate() {
      this.stopAutoUpdate()
      this.updateInterval = setInterval(() => {
        useDeviceStore().fetchDevices()
      }, 5000)
    },
    stopAutoUpdate() {
      if (this.updateInterval) {
        clearInterval(this.updateInterval)
        this.updateInterval = null
      }
    }
  },
  mounted() {
    this.startAutoUpdate()
    if (this.device) {
      this.isEnabled = this.device.active;
    }
  },
  beforeUnmount() {
    this.stopAutoUpdate()
  }
}
</script> 