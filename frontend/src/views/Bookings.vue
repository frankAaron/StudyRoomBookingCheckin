<template>
  <div class="page">
    <div class="page-title">我的预约</div>
    <div class="page-sub">支持 8 位数字签到码签到；预约开始后 30 分钟内未签到将自动释放</div>

    <div class="card">
      <div class="filters">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 150px" @change="load(1)">
          <el-option v-for="(t, i) in statusList" :key="i" :label="t" :value="i" />
        </el-select>
        <el-button :icon="Refresh" @click="load()">刷新</el-button>
        <div class="spacer"></div>
        <el-button type="primary" :icon="Key" @click="codeVisible = true">签到码签到</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" size="default">
        <el-table-column prop="seatNo" label="座位" width="90" />
        <el-table-column label="位置" min-width="190">
          <template #default="{ row }">{{ row.buildingName }} · {{ row.roomName }}</template>
        </el-table-column>
        <el-table-column label="预约时段" min-width="210">
          <template #default="{ row }">
            <div>{{ fmtDate(row.startTime) }}</div>
            <div class="muted" style="font-size: 12px">{{ time(row.startTime) }} ~ {{ time(row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="签到码" width="120">
          <template #default="{ row }"><span class="mono">{{ row.checkinCode || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="学习时长" width="110">
          <template #default="{ row }">
            {{ row.actualMinutes ? `${row.actualMinutes} 分钟` : row.expectedMinutes ? `预计 ${row.expectedMinutes} 分` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" size="small">{{ statusList[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="primary" @click="doCheckin(row)">签到</el-button>
            <el-button v-if="row.status === 1" size="small" type="success" @click="doCheckout(row)">离座</el-button>
            <el-button
              v-if="row.status === 0 || row.status === 1"
              size="small"
              type="danger"
              plain
              @click="doCancel(row)"
            >
              取消
            </el-button>
            <span v-if="row.status > 1" class="muted" style="font-size: 12px">无可用操作</span>
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

    <el-dialog v-model="codeVisible" title="签到码签到" width="400px" destroy-on-close>
      <el-form @submit.prevent>
        <el-form-item label="签到码">
          <el-input v-model="code" maxlength="8" placeholder="请输入 8 位数字签到码" class="mono" />
        </el-form-item>
      </el-form>
      <div class="muted" style="font-size: 12px">签到码可在预约成功弹窗或本页列表中查看</div>
      <template #footer>
        <el-button @click="codeVisible = false">取消</el-button>
        <el-button type="primary" :loading="codeLoading" @click="submitCode">确认签到</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Key } from '@element-plus/icons-vue'
import { api } from '../api'

const statusList = ['待签到', '学习中', '已完成', '已取消', '已过期']
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, status: null })

const codeVisible = ref(false)
const code = ref('')
const codeLoading = ref(false)

function tagType(s) {
  return ['warning', 'success', 'info', 'info', 'danger'][s] || 'info'
}

function fmtDate(v) {
  return v ? String(v).replace('T', ' ').slice(0, 10) : '-'
}

function time(v) {
  return v ? String(v).slice(11, 16) : '-'
}

async function load(page) {
  if (page) query.current = page
  loading.value = true
  try {
    const data = await api.myBookings(query)
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function doCheckin(row) {
  try {
    await ElMessageBox.confirm(`确认签到座位 ${row.seatNo}？`, '签到确认', { confirmButtonText: '签到' })
  } catch {
    return
  }
  try {
    await api.checkin(row.id)
    ElMessage.success('签到成功，开始计时')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function doCheckout(row) {
  try {
    await ElMessageBox.confirm('确认已离座？离座后将结束学习计时并释放座位。', '离座确认', {
      confirmButtonText: '确认离座'
    })
  } catch {
    return
  }
  try {
    const data = await api.checkout(row.id)
    ElMessage.success(`已离座，本次学习 ${data.actualMinutes || 0} 分钟`)
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function doCancel(row) {
  try {
    await ElMessageBox.confirm('确定取消该预约吗？', '取消预约', {
      type: 'warning',
      confirmButtonText: '取消预约'
    })
  } catch {
    return
  }
  try {
    await api.cancelBooking(row.id)
    ElMessage.success('预约已取消')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function submitCode() {
  if (!/^\d{8}$/.test(code.value)) {
    ElMessage.warning('请输入 8 位数字签到码')
    return
  }
  codeLoading.value = true
  try {
    await api.checkinByCode(code.value)
    ElMessage.success('签到成功')
    codeVisible.value = false
    code.value = ''
    load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    codeLoading.value = false
  }
}

onMounted(load)
</script>
