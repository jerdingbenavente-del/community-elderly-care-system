<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TooltipComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { formatDateTime } from '@/utils/familyHome'

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  loading: Boolean,
  records: { type: Array, default: () => [] },
  rangeDays: { type: Number, default: 7 },
})

const emit = defineEmits(['update:rangeDays'])

const tempRef = ref(null)
const bpRef = ref(null)
const hrRef = ref(null)

let tempChart
let bpChart
let hrChart

const sorted = computed(() => {
  const list = Array.isArray(props.records) ? [...props.records] : []
  return list
    .filter((r) => r?.measuredAt)
    .sort((a, b) => String(a.measuredAt).localeCompare(String(b.measuredAt)))
})

const labels = computed(() =>
  sorted.value.map((r) => formatDateTime(r.measuredAt).slice(5, 16)),
)

function baseOption(yName) {
  return {
    color: [],
    tooltip: { trigger: 'axis' },
    legend: {
      top: 4,
      textStyle: { color: '#4a6072', fontSize: 12 },
    },
    grid: { left: 48, right: 24, top: 42, bottom: 36 },
    xAxis: {
      type: 'category',
      data: labels.value,
      axisLabel: { color: '#8aa0b5', fontSize: 11, hideOverlap: true },
      axisLine: { lineStyle: { color: 'rgba(138,160,181,0.35)' } },
    },
    yAxis: {
      type: 'value',
      name: yName,
      nameTextStyle: { color: '#8aa0b5', fontSize: 11 },
      axisLabel: { color: '#8aa0b5', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(138,160,181,0.18)' } },
    },
  }
}

function renderCharts() {
  if (!tempRef.value || !bpRef.value || !hrRef.value) return

  if (!tempChart) tempChart = echarts.init(tempRef.value)
  if (!bpChart) bpChart = echarts.init(bpRef.value)
  if (!hrChart) hrChart = echarts.init(hrRef.value)

  const empty = !sorted.value.length

  tempChart.setOption(
    {
      ...baseOption('℃'),
      color: ['#e89a9a'],
      legend: { data: ['体温'] },
      series: [
        {
          name: '体温',
          type: 'line',
          smooth: true,
          showSymbol: sorted.value.length <= 20,
          data: empty ? [] : sorted.value.map((r) => r.bodyTemperature ?? null),
          lineStyle: { width: 2.5 },
          areaStyle: { color: 'rgba(232,154,154,0.12)' },
        },
      ],
    },
    true,
  )

  bpChart.setOption(
    {
      ...baseOption('mmHg'),
      color: ['#7eb6de', '#3a78a8'],
      legend: { data: ['收缩压', '舒张压'] },
      series: [
        {
          name: '收缩压',
          type: 'line',
          smooth: true,
          showSymbol: sorted.value.length <= 20,
          data: empty ? [] : sorted.value.map((r) => r.systolicPressure ?? null),
          lineStyle: { width: 2.5 },
        },
        {
          name: '舒张压',
          type: 'line',
          smooth: true,
          showSymbol: sorted.value.length <= 20,
          data: empty ? [] : sorted.value.map((r) => r.diastolicPressure ?? null),
          lineStyle: { width: 2.5 },
        },
      ],
    },
    true,
  )

  hrChart.setOption(
    {
      ...baseOption('次/分'),
      color: ['#e8a0b8'],
      legend: { data: ['心率'] },
      series: [
        {
          name: '心率',
          type: 'line',
          smooth: true,
          showSymbol: sorted.value.length <= 20,
          data: empty ? [] : sorted.value.map((r) => r.heartRate ?? null),
          lineStyle: { width: 2.5 },
          areaStyle: { color: 'rgba(232,160,184,0.12)' },
        },
      ],
    },
    true,
  )
}

function resize() {
  tempChart?.resize()
  bpChart?.resize()
  hrChart?.resize()
}

onMounted(() => {
  renderCharts()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  tempChart?.dispose()
  bpChart?.dispose()
  hrChart?.dispose()
  tempChart = bpChart = hrChart = null
})

watch(
  () => [props.records, props.loading],
  () => {
    if (!props.loading) {
      requestAnimationFrame(renderCharts)
    }
  },
  { deep: true },
)
</script>

<template>
  <section class="trends">
    <div class="trends__head">
      <div>
        <h3>健康趋势</h3>
        <p>了解老人近期健康变化</p>
      </div>
      <el-radio-group
        :model-value="rangeDays"
        size="small"
        @update:model-value="emit('update:rangeDays', $event)"
      >
        <el-radio-button :value="7">最近 7 天</el-radio-button>
        <el-radio-button :value="30">最近 30 天</el-radio-button>
      </el-radio-group>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else>
      <el-empty
        v-if="!sorted.length"
        description="暂无趋势数据"
        :image-size="72"
      >
        <template #description>
          <p class="empty-title">暂无趋势数据</p>
          <p class="empty-desc">所选时间范围内还没有健康测量记录。</p>
        </template>
      </el-empty>
      <div v-else class="trends__grid">
        <div class="chart-card">
          <h4>体温趋势</h4>
          <div ref="tempRef" class="chart-box" />
        </div>
        <div class="chart-card">
          <h4>心率趋势</h4>
          <div ref="hrRef" class="chart-box" />
        </div>
        <div class="chart-card chart-card--wide">
          <h4>血压趋势</h4>
          <div ref="bpRef" class="chart-box" />
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.trends {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 18px 20px;
}

.trends__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.trends__head h3 {
  margin: 0;
  font-size: 16px;
  color: #2c4a5e;
}

.trends__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.trends__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.chart-card {
  background: rgba(248, 252, 255, 0.8);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  padding: 12px 12px 8px;
}

.chart-card--wide {
  grid-column: 1 / -1;
}

.chart-card h4 {
  margin: 0 0 4px 8px;
  font-size: 13px;
  color: #4a6072;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.empty-title {
  margin: 0;
  color: #4a6072;
  font-size: 14px;
  font-weight: 600;
}

.empty-desc {
  margin: 6px 0 0;
  color: #8aa0b5;
  font-size: 12px;
}

@media (max-width: 900px) {
  .trends__grid {
    grid-template-columns: 1fr;
  }

  .chart-card--wide {
    grid-column: auto;
  }

  .chart-box {
    height: 260px;
  }

  .trends__head {
    flex-direction: column;
  }
}
</style>
