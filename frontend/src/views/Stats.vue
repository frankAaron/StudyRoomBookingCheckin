<template>
  <div class="page">
    <div class="page-title">学习统计</div>
    <div class="page-sub">每日 / 每周学习时长与热力图</div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">今日学习</div>
        <div class="stat-value">{{ fmtMinutes(stats.todayMinutes) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本周学习</div>
        <div class="stat-value">{{ fmtMinutes(stats.weekMinutes) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本月学习</div>
        <div class="stat-value">{{ fmtMinutes(stats.monthMinutes) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计学习</div>
        <div class="stat-value">{{ fmtMinutes(stats.totalMinutes) }}</div>
        <div class="stat-hint">有效学习 {{ stats.totalDays || 0 }} 天</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">连续打卡</div>
        <div class="stat-value">{{ stats.currentStreak || 0 }} 天</div>
        <div class="stat-hint">最长 {{ stats.maxStreak || 0 }} 天</div>
      </div>
    </div>

    <div class="card" style="margin-top: 16px">
      <div class="card-title">近 30 天学习趋势（分钟）</div>
      <div ref="lineEl" class="chart"></div>
    </div>

    <div class="card">
      <div class="card-title">学习热力图（最近 52 周）</div>
      <div ref="heatEl" class="chart" style="height: 300px"></div>
    </div>

    <div class="card">
      <div class="card-title">星期学习分布（分钟）</div>
      <div ref="barEl" class="chart" style="height: 260px"></div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const stats = ref({})
const lineEl = ref(null)
const heatEl = ref(null)
const barEl = ref(null)
let lineChart = null
let heatChart = null
let barChart = null

function fmtMinutes(m) {
  const v = Number(m || 0)
  if (v < 60) return `${v} 分钟`
  return `${Math.floor(v / 60)} 小时 ${v % 60} 分`
}

function initLine() {
  if (!lineEl.value) return
  lineChart = echarts.init(lineEl.value)
  const days = stats.value.dailyMinutes || []
  lineChart.setOption({
    grid: { left: 44, right: 20, top: 24, bottom: 34 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: days.map((d) => String(d.date).slice(5)),
      axisLine: { lineStyle: { color: '#dbe1ec' } },
      axisLabel: { color: '#8a94a6' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#eef1f6' } },
      axisLabel: { color: '#8a94a6' }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: days.map((d) => d.minutes || 0),
        lineStyle: { width: 3, color: '#4f6ef7' },
        itemStyle: { color: '#4f6ef7' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(79,110,247,0.28)' },
            { offset: 1, color: 'rgba(79,110,247,0.02)' }
          ])
        }
      }
    ]
  })
}

function initHeat() {
  if (!heatEl.value) return
  heatChart = echarts.init(heatEl.value)
  const days = stats.value.heatmapDays || []
  if (!days.length) return
  const start = days[0].date
  const end = days[days.length - 1].date
  heatChart.setOption({
    tooltip: {
      formatter: (p) => `${p.value[0]}<br/>学习 ${p.value[1]} 分钟`
    },
    visualMap: {
      min: 0,
      max: 240,
      type: 'piecewise',
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      itemWidth: 14,
      itemHeight: 14,
      textStyle: { color: '#8a94a6', fontSize: 12 },
      pieces: [
        { lte: 0, label: '0', color: '#eef1f6' },
        { gt: 0, lte: 29, label: '1-29', color: '#c7d7fe' },
        { gt: 29, lte: 59, label: '30-59', color: '#8fa8fb' },
        { gt: 59, lte: 119, label: '60-119', color: '#5b7cfa' },
        { gt: 119, label: '≥120', color: '#2f4bd0' }
      ]
    },
    calendar: {
      top: 52,
      left: 64,
      right: 20,
      bottom: 40,
      range: [start, end],
      cellSize: [16, 16],
      splitLine: { show: false },
      itemStyle: { borderWidth: 1, borderColor: '#fff' },
      yearLabel: { show: false },
      monthLabel: {
        color: '#5c6678',
        fontSize: 14,
        fontWeight: 600,
        padding: [0, 0, 10, 0],
        nameMap: [
          '1月', '2月', '3月', '4月', '5月', '6月',
          '7月', '8月', '9月', '10月', '11月', '12月'
        ]
      },
      dayLabel: {
        color: '#8a94a6',
        fontSize: 14,
        padding: [0, 12, 0, 0],
        firstDay: 0,
        nameMap: ['日', '一', '二', '三', '四', '五', '六']
      }
    },
    series: [
      {
        type: 'heatmap',
        coordinateSystem: 'calendar',
        data: days.map((d) => [d.date, Math.max(0, d.minutes || 0)])
      }
    ]
  })
}

function initBar() {
  if (!barEl.value) return
  barChart = echarts.init(barEl.value)
  const heat = stats.value.heatmap || [0, 0, 0, 0, 0, 0, 0]
  barChart.setOption({
    grid: { left: 44, right: 20, top: 20, bottom: 34 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'],
      axisLine: { lineStyle: { color: '#dbe1ec' } },
      axisLabel: { color: '#8a94a6', fontSize: 13 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#eef1f6' } },
      axisLabel: { color: '#8a94a6' }
    },
    series: [
      {
        type: 'bar',
        barWidth: 40,
        data: heat,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#6d8bff' },
            { offset: 1, color: '#b6c6ff' }
          ])
        }
      }
    ]
  })
}

function resize() {
  lineChart && lineChart.resize()
  heatChart && heatChart.resize()
  barChart && barChart.resize()
}

onMounted(async () => {
  try {
    stats.value = (await api.statistics()) || {}
    await nextTick()
    initLine()
    initHeat()
    initBar()
    window.addEventListener('resize', resize)
  } catch (e) {
    ElMessage.error(e.message)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  lineChart && lineChart.dispose()
  heatChart && heatChart.dispose()
  barChart && barChart.dispose()
})
</script>