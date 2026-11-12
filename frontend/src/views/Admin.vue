<template>
  <div class="page">
    <div class="page-title">管理后台</div>
    <div class="page-sub">预约、违规与自习室使用率统一管理</div>

    <el-tabs v-model="tab" @tab-change="onTabChange">
      <el-tab-pane label="运营概览" name="overview">
        <div class="stat-grid">
          <div class="stat-card">
            <div class="stat-label">用户总数</div>
            <div class="stat-value">{{ dashboard.userCount || 0 }}</div>
            <div class="stat-hint">今日活跃 {{ dashboard.activeUsers || 0 }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">自习室 / 座位</div>
            <div class="stat-value">{{ dashboard.roomCount || 0 }} / {{ dashboard.seatCount || 0 }}</div>
            <div class="stat-hint">楼栋 {{ dashboard.buildingCount || 0 }} 栋</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">今日预约</div>
            <div class="stat-value">{{ dashboard.todayBookings || 0 }}</div>
            <div class="stat-hint">今日学习 {{ fmtMinutes(dashboard.todayTotalMinutes) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">待处理违规</div>
            <div class="stat-value" :style="{ color: dashboard.pendingViolations ? '#ef4444' : '#22c55e' }">
              {{ dashboard.pendingViolations || 0 }}
            </div>
            <div class="stat-hint">爽约 / 超时占座</div>
          </div>
        </div>

        <div class="card" style="margin-top: 16px">
          <div class="card-title">自习室使用率</div>
          <el-table :data="utilization" size="small">
            <el-table-column prop="roomName" label="自习室" min-width="180" />
            <el-table-column prop="availableSeats" label="空闲座位" width="110" />
            <el-table-column prop="totalSeats" label="可用座位" width="110" />
            <el-table-column label="使用率" min-width="220">
              <template #default="{ row }">
                <el-progress
                  :percentage="Math.round((row.utilizationRate || 0) * 100)"
                  :stroke-width="10"
                  :color="rateColor(row.utilizationRate)"
                />
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!utilization.length" class="empty">暂无使用率数据</div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="预约记录" name="bookings">
        <div class="card">
          <div class="filters">
            <el-select v-model="bookingQuery.status" placeholder="全部状态" clearable style="width: 150px" @change="loadBookings(1)">
              <el-option v-for="(t, i) in statusList" :key="i" :label="t" :value="i" />
            </el-select>
            <el-button :icon="Refresh" @click="loadBookings()">刷新</el-button>
          </div>
          <el-table :data="bookings" v-loading="bookingLoading" size="small">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="userNickname" label="用户" width="120" />
            <el-table-column prop="seatNo" label="座位" width="90" />
            <el-table-column label="位置" min-width="170">
              <template #default="{ row }">{{ row.buildingName }} · {{ row.roomName }}</template>
            </el-table-column>
            <el-table-column label="时段" min-width="200">
              <template #default="{ row }">{{ fmtTime(row.startTime) }} ~ {{ timeOnly(row.endTime) }}</template>
            </el-table-column>
            <el-table-column prop="checkinCode" label="签到码" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="tagType(row.status)" size="small">{{ statusList[row.status] }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div style="display: flex; justify-content: flex-end; margin-top: 14px">
            <el-pagination
              :current-page="bookingQuery.current"
              :page-size="bookingQuery.size"
              @update:current-page="(v) => (bookingQuery.current = v)"
              @update:page-size="(v) => (bookingQuery.size = v)"
              :total="bookingTotal"
              layout="total, prev, pager, next"
              @current-change="loadBookings()"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="违规记录" name="violations">
        <div class="card">
          <div class="filters">
            <el-select v-model="violationQuery.type" placeholder="全部类型" clearable style="width: 150px" @change="loadViolations(1)">
              <el-option label="爽约未签到" :value="1" />
              <el-option label="超时占座" :value="2" />
              <el-option label="其他" :value="3" />
            </el-select>
            <el-select v-model="violationQuery.status" placeholder="全部状态" clearable style="width: 140px" @change="loadViolations(1)">
              <el-option label="未处理" :value="0" />
              <el-option label="已处理" :value="1" />
            </el-select>
            <el-button :icon="Refresh" @click="loadViolations()">刷新</el-button>
          </div>
          <el-table :data="violations" v-loading="violationLoading" size="small">
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="nickname" label="用户" width="120" />
            <el-table-column label="位置" min-width="170">
              <template #default="{ row }">{{ row.roomName || '-' }} · {{ row.seatNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="row.type === 1 ? 'danger' : row.type === 2 ? 'warning' : 'info'" size="small">
                  {{ row.typeName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="240" show-overflow-tooltip />
            <el-table-column prop="penalty" label="扣分" width="80" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '已处理' : '未处理' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="150">
              <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 0" link type="primary" size="small" @click="handle(row)">标记处理</el-button>
                <span v-else class="muted" style="font-size: 12px">已处理</span>
              </template>
            </el-table-column>
          </el-table>
          <div style="display: flex; justify-content: flex-end; margin-top: 14px">
            <el-pagination
              :current-page="violationQuery.current"
              :page-size="violationQuery.size"
              @update:current-page="(v) => (violationQuery.current = v)"
              @update:page-size="(v) => (violationQuery.size = v)"
              :total="violationTotal"
              layout="total, prev, pager, next"
              @current-change="loadViolations()"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="用户统计" name="stats">
        <div class="stat-grid">
          <div class="stat-card">
            <div class="stat-label">注册用户</div>
            <div class="stat-value">{{ userStats.totalUsers || 0 }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">本周活跃用户</div>
            <div class="stat-value">{{ userStats.weekActiveUsers || 0 }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">累计学习时长</div>
            <div class="stat-value">{{ fmtMinutes(userStats.totalStudyMinutes) }}</div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { api } from '../api'

const tab = ref('overview')
const statusList = ['待签到', '学习中', '已完成', '已取消', '已过期']

const dashboard = ref({})
const utilization = ref([])
const bookings = ref([])
const bookingTotal = ref(0)
const bookingLoading = ref(false)
const bookingQuery = reactive({ current: 1, size: 10, status: null })

const violations = ref([])
const violationTotal = ref(0)
const violationLoading = ref(false)
const violationQuery = reactive({ current: 1, size: 10, type: null, status: null })

const userStats = ref({})

function fmtMinutes(m) {
  const v = Number(m || 0)
  if (v < 60) return `${v} 分钟`
  return `${Math.floor(v / 60)} 小时 ${v % 60} 分`
}

function fmtTime(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
}

function timeOnly(v) {
  return v ? String(v).slice(11, 16) : '-'
}

function tagType(s) {
  return ['warning', 'success', 'info', 'info', 'danger'][s] || 'info'
}

function rateColor(rate) {
  const r = Number(rate || 0)
  if (r >= 0.8) return '#ef4444'
  if (r >= 0.5) return '#f59e0b'
  return '#22c55e'
}

async function loadOverview() {
  try {
    const [d, u] = await Promise.all([api.adminDashboard(), api.adminUtilization()])
    dashboard.value = d || {}
    utilization.value = (u && u.rooms) || []
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function loadBookings(page) {
  if (page) bookingQuery.current = page
  bookingLoading.value = true
  try {
    const data = await api.adminBookings(bookingQuery)
    bookings.value = data.records || []
    bookingTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    bookingLoading.value = false
  }
}

async function loadViolations(page) {
  if (page) violationQuery.current = page
  violationLoading.value = true
  try {
    const data = await api.adminViolations(violationQuery)
    violations.value = data.records || []
    violationTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    violationLoading.value = false
  }
}

async function loadUserStats() {
  try {
    userStats.value = (await api.adminUserStats()) || {}
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function handle(row) {
  try {
    await ElMessageBox.confirm('确认该违规记录已处理？', '处理违规', { confirmButtonText: '确认' })
  } catch {
    return
  }
  try {
    await api.handleViolation(row.id, 1)
    ElMessage.success('已标记处理')
    loadViolations()
    loadOverview()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function onTabChange(name) {
  if (name === 'bookings' && !bookings.value.length) loadBookings()
  if (name === 'violations' && !violations.value.length) loadViolations()
  if (name === 'stats') loadUserStats()
}

onMounted(() => {
  loadOverview()
  loadBookings()
})
</script>
