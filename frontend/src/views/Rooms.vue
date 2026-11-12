<template>
  <div class="page">
    <div class="page-title">自习室与座位</div>
    <div class="page-sub">按楼栋 / 楼层查看座位实时状态，选择空闲座位完成预约</div>

    <div class="filters">
      <el-switch v-if="isAdmin" v-model="manageMode" active-text="管理模式" />
      <div class="spacer"></div>
      <template v-if="isAdmin">
        <el-button :icon="Plus" @click="openBuildingForm()">新增楼栋</el-button>
        <el-button :icon="Plus" @click="openRoomForm()">新增自习室</el-button>
        <el-button :icon="Plus" @click="openSeatForm()">新增座位</el-button>
        <el-button :icon="Grid" @click="batchCreateSeats">批量生成座位</el-button>
      </template>
      <el-button :icon="Refresh" @click="reloadAll">刷新</el-button>
    </div>

    <div style="display: grid; grid-template-columns: 340px 1fr; gap: 16px; align-items: start">
      <div class="card" v-loading="loading">
        <div class="card-title">楼栋与自习室</div>
        <div v-for="group in groupedRooms" :key="group.building.id" class="building-block">
          <div style="display: flex; align-items: center; gap: 8px">
            <el-icon color="#4f6ef7"><OfficeBuilding /></el-icon>
            <strong style="font-size: 14px">{{ group.building.name }}</strong>
            <span class="muted" style="font-size: 12px">{{ group.building.location }}</span>
            <div class="spacer"></div>
            <template v-if="isAdmin">
              <el-button link type="primary" size="small" @click="openRoomForm(null, group.building.id)">添加</el-button>
              <el-dropdown trigger="click">
                <el-button link size="small">管理</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openBuildingForm(group.building)">编辑楼栋</el-dropdown-item>
                    <el-dropdown-item divided @click="removeBuilding(group.building)">删除楼栋</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </div>
          <div v-for="floor in group.floors" :key="floor.floor">
            <div class="floor-title">{{ floor.floor }} 层</div>
            <div v-for="room in floor.rooms" :key="room.id" style="margin-bottom: 8px">
              <div class="room-card" :class="{ active: currentRoom && currentRoom.id === room.id }" @click="selectRoom(room)">
                <div style="display: flex; align-items: center; gap: 6px">
                  <strong style="font-size: 13.5px">{{ room.name }}</strong>
                  <span class="muted" style="font-size: 12px">{{ room.code }}</span>
                  <div class="spacer"></div>
                  <template v-if="isAdmin">
                    <el-button link size="small" type="primary" @click.stop="openRoomForm(room)">编辑</el-button>
                    <el-button link size="small" type="danger" @click.stop="removeRoom(room)">删除</el-button>
                  </template>
                </div>
                <div class="room-meta">
                  <span>开放 {{ shortTime(room.openTime) }} - {{ shortTime(room.closeTime) }}</span>
                  <span>座位 {{ room.totalSeats || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="!loading && !groupedRooms.length" class="empty">暂无自习室数据</div>
      </div>

      <div class="card" v-loading="seatsLoading">
        <div class="card-title">
          <span>{{ currentRoom ? `${buildingName(currentRoom.buildingId)} · ${currentRoom.name}` : '请选择自习室' }}</span>
          <div class="legend">
            <span><i class="seat legend-dot free"></i>空闲</span>
            <span><i class="seat legend-dot occupied"></i>使用中</span>
            <span><i class="seat legend-dot reserved"></i>已预约</span>
            <span><i class="seat legend-dot maintenance"></i>维护</span>
          </div>
        </div>

        <div v-if="currentRoom" class="seat-map">
          <div class="seat-grid" :style="{ gridTemplateColumns: `repeat(${gridCols}, 46px)` }">
            <div
              v-for="seat in seats"
              :key="seat.id"
              class="seat"
              :class="seatClass(seat)"
              :title="`${seat.seatNo} · ${seatText(seat.status)}`"
              @click="onSeatClick(seat)"
            >
              {{ seat.seatNo }}
            </div>
          </div>
          <div v-if="!seats.length" class="empty">该自习室还没有座位，管理员可批量生成</div>
        </div>
        <div v-else class="empty">左侧选择一个自习室查看座位</div>
      </div>
    </div>

    <!-- 预约 -->
    <el-dialog v-model="bookingVisible" title="预约座位" width="440px" destroy-on-close>
      <el-form ref="bookingRef" :model="bookingForm" :rules="bookingRules" label-width="82px">
        <el-form-item label="座位">
          <el-input :model-value="`${currentRoom?.name || ''} · ${selectedSeat?.seatNo || ''}`" disabled />
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="bookingForm.date" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间" prop="time">
          <el-time-select
            v-model="bookingForm.time"
            start="06:00"
            step="00:30"
            end="22:00"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预计时长" prop="minutes">
          <el-select v-model="bookingForm.minutes" style="width: 100%">
            <el-option v-for="m in durations" :key="m" :label="`${m / 60} 小时`" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bookingForm.remark" maxlength="100" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookingVisible = false">取消</el-button>
        <el-button type="primary" :loading="bookingSubmitting" @click="submitBooking">确认预约</el-button>
      </template>
    </el-dialog>

    <!-- 预约成功 -->
    <el-dialog v-model="resultVisible" title="预约成功" width="420px">
      <el-result icon="success" :title="`座位 ${booked?.seatNo || ''} 预约成功`" :sub-title="`签到码：${booked?.checkinCode || ''}`">
        <template #extra>
          <div class="muted" style="font-size: 13px; line-height: 1.9">
            请在预约开始后 30 分钟内完成签到，<br />超时将自动释放座位并记录违规。
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button @click="resultVisible = false">继续选座</el-button>
        <el-button type="primary" @click="$router.push('/bookings')">去签到</el-button>
      </template>
    </el-dialog>

    <!-- 楼栋表单 -->
    <el-dialog v-model="buildingVisible" :title="buildingForm.id ? '编辑楼栋' : '新增楼栋'" width="440px" destroy-on-close>
      <el-form ref="buildingRef" :model="buildingForm" :rules="buildingRules" label-width="82px">
        <el-form-item label="名称" prop="name"><el-input v-model="buildingForm.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="buildingForm.code" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="buildingForm.location" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="buildingForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="buildingVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBuilding">保存</el-button>
      </template>
    </el-dialog>

    <!-- 自习室表单 -->
    <el-dialog v-model="roomVisible" :title="roomForm.id ? '编辑自习室' : '新增自习室'" width="480px" destroy-on-close>
      <el-form ref="roomRef" :model="roomForm" :rules="roomRules" label-width="88px">
        <el-form-item label="所属楼栋" prop="buildingId">
          <el-select v-model="roomForm.buildingId" style="width: 100%">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name"><el-input v-model="roomForm.name" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="roomForm.code" /></el-form-item>
        <el-form-item label="楼层" prop="floor">
          <el-input-number v-model="roomForm.floor" :min="1" :max="30" style="width: 100%" />
        </el-form-item>
        <el-form-item label="座位数">
          <div style="display: flex; align-items: center; gap: 10px; width: 100%">
            <el-tag type="info" effect="plain" size="large">
              {{ roomForm.totalSeats || 0 }} 个（由座位表决定）
            </el-tag>
            <span class="muted" style="font-size: 12px">
              在房间卡片上点「管理 → 批量生成座位」调整
            </span>
          </div>
        </el-form-item>
        <el-form-item label="开放时间">
          <div style="display: flex; gap: 8px; width: 100%">
            <el-time-picker v-model="roomForm.openTime" value-format="HH:mm:ss" placeholder="开始" style="flex: 1" />
            <el-time-picker v-model="roomForm.closeTime" value-format="HH:mm:ss" placeholder="结束" style="flex: 1" />
          </div>
        </el-form-item>
        <el-form-item label="设备"><el-input v-model="roomForm.equipment" placeholder="如：空调、插座" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoom">保存</el-button>
      </template>
    </el-dialog>

    <!-- 座位表单 -->
    <el-dialog v-model="seatVisible" :title="seatForm.id ? '编辑座位' : '新增座位'" width="420px" destroy-on-close>
      <el-form ref="seatRef" :model="seatForm" :rules="seatRules" label-width="82px">
        <el-form-item label="所属自习室" prop="roomId">
          <el-select v-model="seatForm.roomId" style="width: 100%">
            <el-option v-for="r in rooms" :key="r.id" :label="`${buildingName(r.buildingId)} · ${r.name}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="座位号" prop="seatNo"><el-input v-model="seatForm.seatNo" placeholder="如 A-01" /></el-form-item>
        <el-form-item label="行"><el-input-number v-model="seatForm.row" :min="1" :max="50" style="width: 100%" /></el-form-item>
        <el-form-item label="列"><el-input-number v-model="seatForm.col" :min="1" :max="50" style="width: 100%" /></el-form-item>
        <el-form-item label="二维码"><el-input v-model="seatForm.qrcode" placeholder="选填，唯一标识" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="seatForm.id" type="danger" plain @click="removeSeat(seatForm.id)">删除</el-button>
        <el-button @click="seatVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveSeat">保存</el-button>
      </template>
    </el-dialog>

    <!-- 座位管理 -->
    <el-dialog v-model="seatManageVisible" :title="`座位管理 · ${selectedSeat?.seatNo || ''}`" width="380px">
      <div style="display: flex; flex-direction: column; gap: 10px">
        <el-button @click="openSeatForm(selectedSeat)">编辑座位信息</el-button>
        <el-button type="warning" plain @click="setSeatStatus(3)">设为维护中</el-button>
        <el-button type="success" plain @click="setSeatStatus(0)">设为空闲</el-button>
        <el-button type="danger" plain @click="removeSeat(selectedSeat.id)">删除座位</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Grid, OfficeBuilding } from '@element-plus/icons-vue'
import { api } from '../api'
import { store } from '../store'

const isAdmin = computed(() => store.user?.role === 1)
const manageMode = ref(false)
const buildings = ref([])
const rooms = ref([])
const seats = ref([])
const currentRoom = ref(null)
const loading = ref(false)
const seatsLoading = ref(false)

const durations = [60, 120, 180, 240, 300, 360]

const groupedRooms = computed(() =>
  buildings.value.map((b) => {
    const list = rooms.value.filter((r) => r.buildingId === b.id)
    const floors = [...new Set(list.map((r) => r.floor || 1))].sort((a, b2) => a - b2)
    return {
      building: b,
      floors: floors.map((f) => ({ floor: f, rooms: list.filter((r) => (r.floor || 1) === f) }))
    }
  })
)

const gridCols = computed(() => {
  const max = seats.value.reduce((m, s) => Math.max(m, s.col || 0), 0)
  return Math.max(max || 6, 2)
})

const selectedSeat = ref(null)
const booked = ref(null)

const bookingVisible = ref(false)
const resultVisible = ref(false)
const bookingRef = ref(null)
const bookingSubmitting = ref(false)
const bookingForm = reactive({ date: '', time: '', minutes: 120, remark: '' })
const bookingRules = {
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  time: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  minutes: [{ required: true, message: '请选择时长', trigger: 'change' }]
}

const buildingVisible = ref(false)
const buildingRef = ref(null)
const buildingForm = reactive({ id: null, name: '', code: '', location: '', description: '' })
const buildingRules = { name: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }] }

const roomVisible = ref(false)
const roomRef = ref(null)
const roomForm = reactive({
  id: null,
  buildingId: null,
  name: '',
  code: '',
  floor: 1,
  totalSeats: 0,
  openTime: '08:00:00',
  closeTime: '22:00:00',
  equipment: ''
})
const roomRules = {
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  name: [{ required: true, message: '请输入自习室名称', trigger: 'blur' }],
  floor: [{ required: true, message: '请输入楼层', trigger: 'change' }]
}

const seatVisible = ref(false)
const seatRef = ref(null)
const seatForm = reactive({ id: null, roomId: null, seatNo: '', row: 1, col: 1, qrcode: '' })
const seatRules = {
  roomId: [{ required: true, message: '请选择自习室', trigger: 'change' }],
  seatNo: [{ required: true, message: '请输入座位号', trigger: 'blur' }]
}

const seatManageVisible = ref(false)
const saving = ref(false)

function shortTime(t) {
  return t ? String(t).slice(0, 5) : '--:--'
}

function buildingName(id) {
  const b = buildings.value.find((x) => x.id === id)
  return b ? b.name : '未知楼栋'
}

function seatClass(seat) {
  return ['free', 'occupied', 'reserved', 'maintenance'][seat.status] || 'free'
}

function seatText(s) {
  return ['空闲', '使用中', '已预约', '维护中'][s] || '未知'
}

function pad(n) {
  return String(n).padStart(2, '0')
}

function todayStr() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function defaultTime() {
  const d = new Date()
  d.setMinutes(Math.ceil(d.getMinutes() / 30) * 30, 0, 0)
  if (d.getHours() < 6) return '08:00'
  if (d.getHours() > 22) return '21:30'
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadBuildings() {
  buildings.value = await api.buildings()
}

async function loadRooms() {
  rooms.value = await api.rooms()
}

async function selectRoom(room) {
  currentRoom.value = room
  seatsLoading.value = true
  try {
    seats.value = await api.seats(room.id)
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    seatsLoading.value = false
  }
}

async function reloadAll() {
  loading.value = true
  try {
    await Promise.all([loadBuildings(), loadRooms()])
    if (currentRoom.value) {
      const fresh = rooms.value.find((r) => r.id === currentRoom.value.id)
      if (fresh) await selectRoom(fresh)
    } else if (rooms.value.length) {
      await selectRoom(rooms.value[0])
    }
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function refreshSeats() {
  if (currentRoom.value) {
    seats.value = await api.seats(currentRoom.value.id)
  }
}

function onSeatClick(seat) {
  selectedSeat.value = seat
  if (isAdmin.value && manageMode.value) {
    seatManageVisible.value = true
    return
  }
  if (seat.status === 3) {
    ElMessage.warning('该座位正在维护中')
    return
  }
  if (seat.status !== 0) {
    ElMessage.warning(`该座位${seatText(seat.status)}，请选择空闲座位`)
    return
  }
  bookingForm.date = todayStr()
  bookingForm.time = defaultTime()
  bookingForm.minutes = 120
  bookingForm.remark = ''
  bookingVisible.value = true
}

async function submitBooking() {
  await bookingRef.value.validate(async (ok) => {
    if (!ok) return
    bookingSubmitting.value = true
    try {
      const data = await api.createBooking({
        seatId: selectedSeat.value.id,
        startTime: `${bookingForm.date}T${bookingForm.time}:00`,
        expectedMinutes: bookingForm.minutes,
        remark: bookingForm.remark || undefined
      })
      booked.value = data
      bookingVisible.value = false
      resultVisible.value = true
      await refreshSeats()
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      bookingSubmitting.value = false
    }
  })
}

function openBuildingForm(b) {
  Object.assign(buildingForm, {
    id: null,
    name: '',
    code: '',
    location: '',
    description: ''
  })
  if (b) Object.assign(buildingForm, b)
  buildingVisible.value = true
}

async function saveBuilding() {
  await buildingRef.value.validate(async (ok) => {
    if (!ok) return
    saving.value = true
    try {
      const payload = {
        name: buildingForm.name,
        code: buildingForm.code,
        location: buildingForm.location,
        description: buildingForm.description
      }
      if (buildingForm.id) await api.updateBuilding(buildingForm.id, payload)
      else await api.createBuilding(payload)
      ElMessage.success('保存成功')
      buildingVisible.value = false
      await reloadAll()
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      saving.value = false
    }
  })
}

async function removeBuilding(b) {
  try {
    await ElMessageBox.confirm(`确定删除楼栋「${b.name}」吗？其下自习室将不可见。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  try {
    await api.deleteBuilding(b.id)
    ElMessage.success('已删除')
    if (currentRoom.value && currentRoom.value.buildingId === b.id) {
      currentRoom.value = null
      seats.value = []
    }
    await reloadAll()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openRoomForm(room, buildingId) {
  Object.assign(roomForm, {
    id: null,
    buildingId: buildingId || (buildings.value[0] && buildings.value[0].id) || null,
    name: '',
    code: '',
    floor: 1,
    totalSeats: 0,
    openTime: '08:00:00',
    closeTime: '22:00:00',
    equipment: ''
  })
  if (room) Object.assign(roomForm, room)
  roomVisible.value = true
}

async function saveRoom() {
  await roomRef.value.validate(async (ok) => {
    if (!ok) return
    saving.value = true
    try {
      const payload = { ...roomForm }
      delete payload.id
      if (roomForm.id) await api.updateRoom(roomForm.id, payload)
      else await api.createRoom(payload)
      ElMessage.success('保存成功')
      roomVisible.value = false
      await reloadAll()
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      saving.value = false
    }
  })
}

async function removeRoom(room) {
  try {
    await ElMessageBox.confirm(`确定删除自习室「${room.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除'
    })
  } catch {
    return
  }
  try {
    await api.deleteRoom(room.id)
    ElMessage.success('已删除')
    if (currentRoom.value && currentRoom.value.id === room.id) {
      currentRoom.value = null
      seats.value = []
    }
    await reloadAll()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openSeatForm(seat) {
  const roomId = seat?.roomId || currentRoom.value?.id || rooms.value[0]?.id || null
  Object.assign(seatForm, { id: null, roomId, seatNo: '', row: 1, col: 1, qrcode: '' })
  if (seat) Object.assign(seatForm, seat)
  seatManageVisible.value = false
  seatVisible.value = true
}

async function saveSeat() {
  await seatRef.value.validate(async (ok) => {
    if (!ok) return
    saving.value = true
    try {
      const payload = {
        roomId: seatForm.roomId,
        seatNo: seatForm.seatNo,
        row: seatForm.row,
        col: seatForm.col,
        qrcode: seatForm.qrcode || undefined
      }
      if (seatForm.id) await api.updateSeat(seatForm.id, payload)
      else await api.createSeat(payload)
      ElMessage.success('保存成功')
      seatVisible.value = false
      if (currentRoom.value) await selectRoom(currentRoom.value)
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      saving.value = false
    }
  })
}

async function removeSeat(id) {
  const seatId = typeof id === 'number' ? id : seatForm.id || selectedSeat.value?.id
  if (!seatId) return
  try {
    await ElMessageBox.confirm('确定删除该座位吗？', '删除确认', { type: 'warning', confirmButtonText: '删除' })
  } catch {
    return
  }
  try {
    await api.deleteSeat(seatId)
    ElMessage.success('已删除')
    seatVisible.value = false
    seatManageVisible.value = false
    await refreshSeats()
    await loadRooms()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function setSeatStatus(status) {
  try {
    await api.seatStatus(selectedSeat.value.id, status)
    ElMessage.success('状态已更新')
    seatManageVisible.value = false
    await refreshSeats()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function batchCreateSeats() {
  if (!currentRoom.value) {
    ElMessage.warning('请先选择自习室')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('格式：行数 x 列数，例如 6x10', '批量生成座位', {
      inputValue: '6x10',
      inputPattern: /^\d{1,2}x\d{1,2}$/,
      inputErrorMessage: '请输入如 6x10 的格式'
    })
    const [rows, cols] = value.toLowerCase().split('x').map(Number)
    await api.batchSeats(currentRoom.value.id, rows, cols)
    ElMessage.success('批量生成成功')
    await selectRoom(currentRoom.value)
    await loadRooms()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message || '操作取消')
  }
}

onMounted(reloadAll)
</script>