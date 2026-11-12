<template>
  <div class="page">
    <div class="page-title">你好，{{ store.user?.nickname || store.user?.username }}</div>
    <div class="page-sub">今天也要专注学习哦，当前已连续打卡 {{ stats.currentStreak || 0 }} 天</div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label"><el-icon><Timer /></el-icon>今日学习</div>
        <div class="stat-value">{{ fmtMinutes(stats.todayMinutes) }}</div>
        <div class="stat-hint">本周 {{ fmtMinutes(stats.weekMinutes) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label"><el-icon><TrendCharts /></el-icon>本月学习</div>
        <div class="stat-value">{{ fmtMinutes(stats.monthMinutes) }}</div>
        <div class="stat-hint">累计 {{ fmtMinutes(stats.totalMinutes) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label"><el-icon><Sunny /></el-icon>连续打卡</div>
        <div class="stat-value">{{ stats.currentStreak || 0 }} 天</div>
        <div class="stat-hint">最长 {{ stats.maxStreak || 0 }} 天</div>
      </div>
      <div class="stat-card">
        <div class="stat-label"><el-icon><Tickets /></el-icon>进行中的预约</div>
        <div class="stat-value">{{ activeBookings.length }}</div>
        <div class="stat-hint">今日共 {{ todayBookings.length }} 条预约</div>
      </div>
    </div>

    <div class="card" style="margin-top: 16px">
      <div class="card-title">
        <span>快捷入口</span>
      </div>
      <div style="display: flex; flex-wrap: wrap; gap: 10px">
        <el-button type="primary" @click="$router.push('/rooms')">预约座位</el-button>
        <el-button @click="$router.push('/bookings')">扫码/签到码签到</el-button>
        <el-button @click="$router.push('/study')">开始学习计时</el-button>
        <el-button @click="$router.push('/stats')">查看学习统计</el-button>
        <el-button @click="$router.push('/goals')">设置学习目标</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-title">
        <span>我的预约</span>
        <el-button text type="primary" @click="$router.push('/bookings')">查看全部</el-button>
      </div>
      <el-table :data="bookings" v-loading="loading" size="small">
        <el-table-column prop="seatNo" label="座位" width="90" />
        <el-table-column label="位置" min-width="180">
          <template #default="{ row }">{{ row.buildingName }} · {{ row.roomName }}</template>
        </el-table-column>
        <el-table-column label="时间段" min-width="210">
          <template #default="{ row }">{{ fmt(row.startTime) }} ~ {{ timeOnly(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="签到码" width="120">
          <template #default="{ row }"><span class="mono">{{ row.checkinCode }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !bookings.length" class="empty">暂无预约，去自习室挑一个座位吧</div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Timer, TrendCharts, Sunny, Tickets } from '@element-plus/icons-vue'
import { api } from '../api'
import { store } from '../store'

const stats = ref({})
const bookings = ref([])
const loading = ref(false)

const activeBookings = computed(() => bookings.value.filter((b) => b.status === 0 || b.status === 1))
const todayBookings = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return bookings.value.filter((b) => String(b.bookingDate || '').slice(0, 10) === today)
})

function fmtMinutes(m) {
  const v = Number(m || 0)
  if (v < 60) return `${v} 分钟`
  const h = Math.floor(v / 60)
  const r = v % 60
  return r ? `${h}h ${r}m` : `${h} 小时`
}

function fmt(v) {
  return v ? String(v).replace('T', ' ').slice(5, 16) : '-'
}

function timeOnly(v) {
  return v ? String(v).slice(11, 16) : '-'
}

function statusText(s) {
  return ['待签到', '学习中', '已完成', '已取消', '已过期'][s] || '未知'
}

function statusType(s) {
  return ['warning', 'success', 'info', 'info', 'danger'][s] || 'info'
}

onMounted(async () => {
  loading.value = true
  try {
    const [s, page] = await Promise.all([api.statistics(), api.myBookings({ current: 1, size: 5 })])
    stats.value = s || {}
    bookings.value = page.records || []
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
})
</script>
