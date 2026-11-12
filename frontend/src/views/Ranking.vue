<template>
  <div class="page">
    <div class="page-title">排行榜</div>
    <div class="page-sub">按学习时长排名，添加好友互相监督</div>

    <div class="stat-grid" style="margin-bottom: 16px">
      <div class="stat-card">
        <div class="stat-label">我的排名</div>
        <div class="stat-value">#{{ myRank.rank || '-' }}</div>
        <div class="stat-hint">{{ periodLabel }}学习 {{ fmt(myRank.totalMinutes) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">好友数量</div>
        <div class="stat-value">{{ friends.length }}</div>
        <div class="stat-hint">待处理申请 {{ requests.length }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">好友本周榜首</div>
        <div class="stat-value" style="font-size: 18px">{{ topFriend ? topFriend.nickname : '-' }}</div>
        <div class="stat-hint">{{ topFriend ? fmt(topFriend.totalMinutes) : '暂无好友数据' }}</div>
      </div>
    </div>

    <div class="card">
      <div class="card-title">
        <span>学习榜</span>
        <el-radio-group v-model="period" size="small" @change="loadRanking">
          <el-radio-button value="daily">日榜</el-radio-button>
          <el-radio-button value="weekly">周榜</el-radio-button>
          <el-radio-button value="monthly">月榜</el-radio-button>
          <el-radio-button value="all">总榜</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="ranking" v-loading="loading" size="default">
        <el-table-column label="排名" width="80">
          <template #default="{ row }">
            <span class="rank-badge" :class="row.rank <= 3 ? `top${row.rank}` : ''">{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 10px">
              <el-avatar :size="30" :src="assetUrl(row.avatar)">{{ (row.nickname || 'U').slice(0, 1) }}</el-avatar>
              <span>{{ row.nickname || `用户${row.userId}` }}</span>
              <el-tag v-if="row.userId === store.user?.userId" size="small" type="primary">我</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="学习时长" width="140">
          <template #default="{ row }">{{ fmt(row.totalMinutes) }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !ranking.length" class="empty">该周期暂无学习记录</div>
    </div>

    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px">
      <div class="card" style="margin: 0">
        <div class="card-title">
          <span>好友监督</span>
          <el-button size="small" :icon="Plus" @click="addVisible = true">添加好友</el-button>
        </div>
        <el-table :data="friends" size="small">
          <el-table-column label="好友" min-width="140">
            <template #default="{ row }">{{ row.nickname || `用户${row.userId}` }}</template>
          </el-table-column>
          <el-table-column label="本月时长" width="120">
            <template #default="{ row }">{{ fmt(row.totalMinutes) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button link type="danger" size="small" @click="removeFriend(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!friends.length" class="empty">还没有好友，添加好友一起学习吧</div>
      </div>

      <div class="card" style="margin: 0">
        <div class="card-title">好友申请</div>
        <el-table :data="requests" size="small">
          <el-table-column label="申请人" min-width="140">
            <template #default="{ row }">{{ row.nickname || `用户${row.userId}` }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="accept(row)">通过</el-button>
              <el-button link type="danger" size="small" @click="reject(row)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!requests.length" class="empty">暂无待处理的好友申请</div>
      </div>
    </div>

    <el-dialog v-model="addVisible" title="添加好友" width="380px" destroy-on-close>
      <el-form @submit.prevent>
        <el-form-item label="用户 ID">
          <el-input v-model="friendId" placeholder="请输入对方的用户 ID" />
        </el-form-item>
      </el-form>
      <div class="muted" style="font-size: 12px">提示：可在排行榜中查看用户 ID</div>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="adding" @click="sendRequest">发送申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { api, assetUrl } from '../api'
import { store } from '../store'

const period = ref('weekly')
const ranking = ref([])
const myRank = ref({})
const friends = ref([])
const requests = ref([])
const loading = ref(false)
const addVisible = ref(false)
const friendId = ref('')
const adding = ref(false)

const periodLabel = computed(
  () => ({ daily: '今日', weekly: '本周', monthly: '本月', all: '累计' })[period.value]
)

const topFriend = computed(() => {
  if (!friends.value.length) return null
  return [...friends.value].sort((a, b) => (b.totalMinutes || 0) - (a.totalMinutes || 0))[0]
})

function fmt(min) {
  const v = Number(min || 0)
  if (v < 60) return `${v} 分钟`
  return `${Math.floor(v / 60)} 小时 ${v % 60} 分`
}

async function loadRanking() {
  loading.value = true
  try {
    const [list, mine] = await Promise.all([api.ranking(period.value, 50), api.myRank(period.value)])
    ranking.value = list || []
    myRank.value = mine || {}
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadFriends() {
  try {
    const [f, req] = await Promise.all([api.friends(), api.friendRequests({ current: 1, size: 50 })])
    friends.value = f || []
    requests.value = req.records || []
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function sendRequest() {
  const id = Number(friendId.value)
  if (!id) {
    ElMessage.warning('请输入用户 ID')
    return
  }
  adding.value = true
  try {
    await api.requestFriend(id)
    ElMessage.success('好友申请已发送')
    addVisible.value = false
    friendId.value = ''
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    adding.value = false
  }
}

async function accept(row) {
  try {
    await api.acceptFriend(row.userId)
    ElMessage.success('已添加好友')
    loadFriends()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function reject(row) {
  try {
    await api.rejectFriend(row.userId)
    ElMessage.success('已拒绝')
    loadFriends()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function removeFriend(row) {
  try {
    await ElMessageBox.confirm(`确定删除好友「${row.nickname || row.userId}」吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除'
    })
  } catch {
    return
  }
  try {
    await api.unfriend(row.userId)
    ElMessage.success('已删除')
    loadFriends()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(() => {
  loadRanking()
  loadFriends()
})
</script>
