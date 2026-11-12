<template>
  <div class="page">
    <div class="page-title">学习计时</div>
    <div class="page-sub">签到后自动开始计时，离座或手动结束即可记录本次学习</div>

    <div class="card" v-loading="loading">
      <div class="card-title">当前学习</div>
      <div v-if="active" style="display: flex; align-items: center; gap: 26px; flex-wrap: wrap">
        <div>
          <div class="muted" style="font-size: 12.5px">已学习</div>
          <div class="timer">{{ elapsedText }}</div>
        </div>
        <div style="line-height: 2; font-size: 13.5px">
          <div><span class="muted">座位：</span>{{ active.roomName }} · {{ active.seatNo }}</div>
          <div><span class="muted">开始时间：</span>{{ fmt(active.startTime) }}</div>
          <div><span class="muted">状态：</span><el-tag type="success" size="small">进行中</el-tag></div>
        </div>
        <div class="spacer"></div>
        <el-button type="primary" size="large" @click="endCurrent">结束学习</el-button>
      </div>
      <div v-else class="empty">
        <div style="margin-bottom: 12px">当前没有进行中的学习计时</div>
        <el-button type="primary" @click="$router.push('/bookings')">去签到开始学习</el-button>
      </div>
    </div>

    <div class="card">
      <div class="card-title">
        <span>打卡记录</span>
        <div class="legend">
          <span>累计 {{ fmtMinutes(stats.totalMinutes) }}</span>
          <span>有效天数 {{ stats.totalDays || 0 }} 天</span>
        </div>
      </div>
      <el-table :data="rows" size="small">
        <el-table-column label="日期" width="120">
          <template #default="{ row }">{{ dateOnly(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="位置" min-width="180">
          <template #default="{ row }">{{ row.buildingName }} · {{ row.roomName }} · {{ row.seatNo }}</template>
        </el-table-column>
        <el-table-column label="开始" width="90">
          <template #default="{ row }">{{ time(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束" width="90">
          <template #default="{ row }">{{ row.endTime ? time(row.endTime) : '进行中' }}</template>
        </el-table-column>
        <el-table-column label="时长" width="110">
          <template #default="{ row }">{{ row.durationMinutes != null ? `${row.durationMinutes} 分钟` : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">
              {{ ['进行中', '已结束', '暂离'][row.status] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 14px">
        <el-pagination
          :current-page="query.current"
          :page-size="query.size"
          @update:current-page="(v) => (query.current = v)"
          @update:page-size="(v) => (query.size = v)"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load()"
          @size-change="load(1)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const active = ref(null)
const now = ref(Date.now())
let timer = null

const query = reactive({ current: 1, size: 10 })

const elapsedText = computed(() => {
  if (!active.value) return '00:00:00'
  const start = new Date(String(active.value.startTime).replace('T', ' ').replace(/-/g, '/')).getTime()
  const diff = Math.max(0, now.value - start)
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function fmt(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
}

function dateOnly(v) {
  return v ? String(v).slice(0, 10) : '-'
}

function time(v) {
  return v ? String(v).slice(11, 16) : '-'
}

function fmtMinutes(m) {
  const v = Number(m || 0)
  if (v < 60) return `${v} 分钟`
  return `${Math.floor(v / 60)} 小时 ${v % 60} 分`
}

async function load(page) {
  if (page) query.current = page
  loading.value = true
  try {
    const [data, st] = await Promise.all([api.myCheckins(query), api.statistics()])
    rows.value = data.records || []
    total.value = data.total || 0
    stats.value = st || {}
    active.value = rows.value.find((r) => r.status === 0) || null
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function endCurrent() {
  try {
    await ElMessageBox.confirm('结束后将记录本次学习时长，确定吗？', '结束学习', {
      confirmButtonText: '结束'
    })
  } catch {
    return
  }
  try {
    const data = await api.endCheckin(active.value.id)
    ElMessage.success(`本次学习 ${data.durationMinutes || 0} 分钟`)
    load(1)
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(() => {
  load()
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>
