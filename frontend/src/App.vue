<template>
  <router-view v-if="route.meta.public" />

  <div v-else class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-logo">自</div>
        <div>
          <div class="brand-name">自习室预约</div>
          <div class="brand-sub">Study Room System</div>
        </div>
      </div>
      <nav class="nav">
        <a
          v-for="item in navItems"
          :key="item.name"
          class="nav-item"
          :class="{ active: route.name === item.name }"
          href="javascript:void(0)"
          @click="go(item.name)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
          <el-badge
            v-if="item.name === 'ranking' && store.pendingRequests"
            :value="store.pendingRequests"
            :hidden="!store.pendingRequests"
            style="margin-left: auto"
          >
            <span style="width: 8px; height: 8px; display: inline-block"></span>
          </el-badge>
        </a>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <div>
          <div class="topbar-title">{{ route.meta.title || '首页' }}</div>
          <div class="topbar-sub">{{ subtitle }}</div>
        </div>
        <div class="spacer"></div>
        <button class="avatar-btn" type="button" aria-label="打开个人信息" @click="openProfile">
          <el-avatar :size="34" :src="assetUrl(store.user?.avatar)">
            {{ (store.user?.nickname || 'U').slice(0, 1) }}
          </el-avatar>
          <span class="avatar-name">{{ store.user?.nickname || store.user?.username }}</span>
          <span class="avatar-role">{{ store.user?.role === 1 ? '管理员' : '学生' }}</span>
        </button>
      </header>

      <router-view :key="route.fullPath" />
    </div>

    <!-- 个人信息面板 -->
    <el-dialog v-model="profileVisible" title="个人信息" width="460px" destroy-on-close>
      <div v-loading="profileLoading">
        <div class="profile-head">
          <button
            class="profile-avatar"
            type="button"
            title="点击更换头像"
            aria-label="点击更换头像"
            :disabled="avatarUploading"
            @click="pickAvatar"
          >
            <el-avatar :size="62" :src="avatarPreview || assetUrl(profile.avatar)">
              {{ (profile.nickname || 'U').slice(0, 1) }}
            </el-avatar>
            <span class="profile-avatar-mask">
              <el-icon :class="{ spinning: avatarUploading }"><Loading v-if="avatarUploading" /><Camera v-else /></el-icon>
            </span>
            <input ref="fileInput" type="file" accept="image/jpeg,image/png" style="display: none" @change="onAvatarPick" />
          </button>
          <div>
            <div style="font-size: 17px; font-weight: 700">{{ profile.nickname || '-' }}</div>
            <div class="muted" style="font-size: 12.5px">
              {{ profile.username }} · {{ profile.role === 1 ? '管理员' : '学生' }}
            </div>
            <div class="muted" style="font-size: 12px; margin-top: 3px">点击头像可更换</div>
          </div>
          <div class="spacer"></div>
          <el-button size="small" :icon="Edit" @click="openEdit">编辑</el-button>
        </div>
        <div class="profile-grid">
          <div class="profile-item"><div class="k">用户 ID</div><div class="v">{{ profile.userId ?? '-' }}</div></div>
          <div class="profile-item"><div class="k">学号</div><div class="v">{{ profile.studentNo || '-' }}</div></div>
          <div class="profile-item"><div class="k">联系电话</div><div class="v">{{ profile.phone || '未填写' }}</div></div>
          <div class="profile-item"><div class="k">邮箱</div><div class="v">{{ profile.email || '未填写' }}</div></div>
          <div class="profile-item"><div class="k">累计学习</div><div class="v">{{ fmtMinutes(profile.totalStudyMinutes) }}</div></div>
          <div class="profile-item"><div class="k">最长连续打卡</div><div class="v">{{ profile.maxStreakDays || 0 }} 天</div></div>
          <div class="profile-item"><div class="k">注册时间</div><div class="v">{{ fmtDate(profile.createTime) }}</div></div>
        </div>
      </div>
      <template #footer>
        <el-button text type="primary" @click="openPassword">修改密码</el-button>
        <el-button type="danger" plain @click="logout">退出登录</el-button>
        <el-button @click="profileVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑资料 -->
    <el-dialog v-model="editVisible" title="编辑个人信息" width="420px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="78px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="选填，11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="选填" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="保存后立即同步到本地与服务端" />
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码 -->
    <el-dialog v-model="passwordVisible" title="修改密码" width="420px" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="88px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  House, Grid, Tickets, Timer, TrendCharts, Aim, Trophy, Setting, User, Edit, Camera, Loading
} from '@element-plus/icons-vue'
import { store, clearSession, patchUser, refreshPendingRequests } from './store'
import { api, assetUrl } from './api'

const route = useRoute()
const router = useRouter()

const ALL_NAV = [
  { name: 'home', label: '首页', icon: House },
  { name: 'rooms', label: '自习室与座位', icon: Grid },
  { name: 'bookings', label: '我的预约', icon: Tickets },
  { name: 'study', label: '学习计时', icon: Timer },
  { name: 'stats', label: '学习统计', icon: TrendCharts },
  { name: 'goals', label: '学习目标', icon: Aim },
  { name: 'ranking', label: '排行榜', icon: Trophy },
  { name: 'admin', label: '管理后台', icon: Setting, admin: true },
  { name: 'users', label: '用户管理', icon: User, admin: true }
]

const navItems = computed(() => ALL_NAV.filter((n) => !n.admin || store.user?.role === 1))
const subtitle = computed(() => {
  const map = {
    home: '今日概览与快捷入口',
    rooms: '按楼栋、楼层查看座位并预约',
    bookings: '签到、离座与预约记录',
    study: '学习计时与打卡记录',
    stats: '每日 / 每周学习时长与热力图',
    goals: '设定目标，持续达成',
    ranking: '学习时长排名与好友监督',
    admin: '预约、违规与使用率管理',
    users: '账号、角色与状态管理'
  }
  return map[route.name] || ''
})

const profileVisible = ref(false)
const profileLoading = ref(false)
const profile = reactive({})
const fileInput = ref(null)
const avatarPreview = ref('')
const avatarUploading = ref(false)

const editVisible = ref(false)
const editSaving = ref(false)
const editFormRef = ref(null)
const editForm = reactive({ nickname: '', phone: '', email: '' })

const passwordVisible = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const editRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ pattern: /^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/, message: '邮箱格式不正确', trigger: 'blur' }]
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度需为 6-32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        value === pwdForm.newPassword ? cb() : cb(new Error('两次输入的密码不一致')),
      trigger: 'blur'
    }
  ]
}

function go(name) {
  router.push({ name })
}

function fmtMinutes(min) {
  const m = Number(min || 0)
  if (m < 60) return `${m} 分钟`
  const h = Math.floor(m / 60)
  const rest = m % 60
  return rest ? `${h} 小时 ${rest} 分` : `${h} 小时`
}

function fmtDate(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
}

async function loadProfile() {
  profileLoading.value = true
  try {
    const data = await api.profile()
    Object.assign(profile, data)
    patchUser({ nickname: data.nickname, avatar: data.avatar, role: data.role })
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    profileLoading.value = false
  }
}

function openProfile() {
  profileVisible.value = true
  avatarPreview.value = ''
  loadProfile()
}

function openEdit() {
  editForm.nickname = profile.nickname || ''
  editForm.phone = profile.phone || ''
  editForm.email = profile.email || ''
  editVisible.value = true
}

async function saveProfile() {
  await editFormRef.value.validate(async (ok) => {
    if (!ok) return
    editSaving.value = true
    try {
      const data = await api.updateProfile({ ...editForm })
      Object.assign(profile, data)
      patchUser({ nickname: data.nickname, avatar: data.avatar })
      ElMessage.success('个人信息已保存')
      editVisible.value = false
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      editSaving.value = false
    }
  })
}

function openPassword() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  passwordVisible.value = true
}

async function savePassword() {
  await pwdFormRef.value.validate(async (ok) => {
    if (!ok) return
    pwdSaving.value = true
    try {
      await api.updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
      ElMessage.success('密码修改成功')
      passwordVisible.value = false
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      pwdSaving.value = false
    }
  })
}

function pickAvatar() {
  if (avatarUploading.value) return
  fileInput.value?.click()
}

function onAvatarPick(event) {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    ElMessage.error('仅支持 JPG / PNG 格式的图片')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }
  avatarPreview.value = URL.createObjectURL(file)
  uploadAvatar(file)
}

async function uploadAvatar(file) {
  avatarUploading.value = true
  try {
    const url = await api.uploadAvatar(file)
    profile.avatar = url
    patchUser({ avatar: url })
    ElMessage.success('头像已更新')
  } catch (e) {
    ElMessage.error(`头像上传失败：${e.message}`)
  } finally {
    // 上传结束（成功或失败）都释放本地预览，头像统一以服务端返回地址渲染
    avatarPreview.value = ''
    avatarUploading.value = false
  }
}

async function logout() {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  clearSession()
  profileVisible.value = false
  router.push({ name: 'login' })
}

watch(
  () => store.token,
  (token) => {
    if (token) {
      if (!store.user) loadProfile()
      refreshPendingRequests()
    }
  }
)

onMounted(() => {
  if (store.token) {
    if (!store.user) loadProfile()
    refreshPendingRequests()
  }
})
</script>