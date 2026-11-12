<template>
  <div class="auth-wrap">
    <div class="auth-card">
      <div class="auth-hero">
        <div>
          <div style="font-size: 13px; letter-spacing: 3px; opacity: 0.7">CAMPUS STUDY</div>
          <h1>校园自习室<br />预约与学习打卡系统</h1>
          <p>
            按楼层查看座位实时状态，一键预约签到；<br />
            自动统计每日学习时长，见证每一次坚持。
          </p>
          <div class="auth-features">
            <div>✓ 楼栋 / 房间 / 座位可视化</div>
            <div>✓ 预约 · 签到 · 离座全流程</div>
            <div>✓ 学习热力图与排行榜</div>
            <div>✓ 好友监督与学习目标</div>
          </div>
        </div>
        <div style="font-size: 12px; opacity: 0.6">StudyRoomBookingCheckin · v2.0</div>
      </div>

      <div class="auth-form">
        <h2>{{ tab === 'login' ? '欢迎回来' : '创建账号' }}</h2>
        <div class="sub">{{ tab === 'login' ? '登录后开始今天的自习' : '注册后即可预约座位' }}</div>

        <el-tabs v-model="tab" stretch>
          <el-tab-pane label="登录" name="login" />
          <el-tab-pane label="注册" name="register" />
        </el-tabs>

        <el-form
          v-if="tab === 'login'"
          ref="loginRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          @keyup.enter="doLogin"
        >
          <el-form-item label="账号 / 学号" prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名或学号" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" show-password size="large" :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doLogin">
            登录
          </el-button>
        </el-form>

        <el-form
          v-else
          ref="registerRef"
          :model="registerForm"
          :rules="registerRules"
          label-position="top"
        >
          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0 12px">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="registerForm.studentNo" placeholder="学号" />
            </el-form-item>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="登录用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="6-20 位" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirm">
              <el-input v-model="registerForm.confirm" type="password" show-password />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="选填" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" placeholder="选填" maxlength="11" />
            </el-form-item>
          </div>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="registerForm.email" placeholder="选填" />
          </el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doRegister">
            注册并登录
          </el-button>
        </el-form>

        <div class="muted" style="font-size: 12px; margin-top: 16px; text-align: center">
          提示：首次使用请先注册；管理员账号由系统预置
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { api } from '../api'
import { setSession } from '../store'

const router = useRouter()
const tab = ref('login')
const loading = ref(false)
const loginRef = ref(null)
const registerRef = ref(null)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({
  studentNo: '',
  username: '',
  password: '',
  confirm: '',
  nickname: '',
  phone: '',
  email: ''
})

const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 32, message: '用户名长度 2-32 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        value === registerForm.password ? cb() : cb(new Error('两次输入的密码不一致')),
      trigger: 'blur'
    }
  ],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ pattern: /^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/, message: '邮箱格式不正确', trigger: 'blur' }]
}

async function doLogin() {
  await loginRef.value.validate(async (ok) => {
    if (!ok) return
    loading.value = true
    try {
      const data = await api.login({ ...loginForm })
      setSession(data.token, {
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        role: data.role,
        avatar: data.avatar
      })
      ElMessage.success(`欢迎回来，${data.nickname || data.username}`)
      router.push({ name: data.role === 1 ? 'admin' : 'home' })
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      loading.value = false
    }
  })
}

async function doRegister() {
  await registerRef.value.validate(async (ok) => {
    if (!ok) return
    loading.value = true
    try {
      await api.register({
        studentNo: registerForm.studentNo,
        username: registerForm.username,
        password: registerForm.password,
        nickname: registerForm.nickname || undefined,
        phone: registerForm.phone || undefined,
        email: registerForm.email || undefined
      })
      const data = await api.login({
        username: registerForm.username,
        password: registerForm.password
      })
      setSession(data.token, {
        userId: data.userId,
        username: data.username,
        nickname: data.nickname,
        role: data.role,
        avatar: data.avatar
      })
      ElMessage.success('注册成功')
      router.push({ name: 'home' })
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      loading.value = false
    }
  })
}
</script>
