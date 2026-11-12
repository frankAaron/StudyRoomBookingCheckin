<template>
  <div class="page">
    <div class="page-title">用户管理</div>
    <div class="page-sub">仅管理员可访问，新增 / 编辑 / 删除 / 启停账号均会记录操作日志</div>

    <div class="card">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="用户名 / 学号"
          clearable
          style="width: 200px"
          :prefix-icon="Search"
          @keyup.enter="load(1)"
        />
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 130px" @change="load(1)">
          <el-option label="学生" :value="0" />
          <el-option label="管理员" :value="1" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="load(1)">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="load(1)">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <div class="spacer"></div>
        <el-button type="primary" :icon="Plus" @click="openForm()">新增用户</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" size="default">
        <el-table-column label="用户名" min-width="150">
          <template #default="{ row }">
            <div style="font-weight: 600">{{ row.username }}</div>
            <div class="muted" style="font-size: 12px">学号 {{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="真实姓名" width="130" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'primary'" size="small">
              {{ row.role === 1 ? '管理员' : '学生' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="联系电话" width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              :disabled="row.id === store.user?.userId"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              :disabled="row.id === store.user?.userId"
              @click="remove(row)"
            >
              删除
            </el-button>
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

    <div class="card">
      <div class="card-title">
        <span>操作日志</span>
        <el-button text type="primary" @click="loadLogs(1)">刷新</el-button>
      </div>
      <el-table :data="logs" size="small" v-loading="logLoading">
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ fmt(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="130" />
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="targetName" label="对象" width="130" />
        <el-table-column prop="detail" label="详情" min-width="240" show-overflow-tooltip />
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 12px">
        <el-pagination
          :current-page="logQuery.current"
          :page-size="logQuery.size"
          @update:current-page="(v) => (logQuery.current = v)"
          @update:page-size="(v) => (logQuery.size = v)"
          :total="logTotal"
          layout="total, prev, pager, next"
          @current-change="loadLogs()"
        />
      </div>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑用户' : '新增用户'" width="460px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" maxlength="32" placeholder="字母、数字、下划线" />
        </el-form-item>
        <el-form-item label="登录密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="form.id ? '留空表示不修改密码' : '6-32 位'"
          />
        </el-form-item>
        <el-form-item label="所属角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio :value="0">学生</el-radio>
            <el-radio :value="1">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="真实姓名" prop="nickname">
          <el-input v-model="form.nickname" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="选填" />
        </el-form-item>
        <el-form-item label="账号状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ saveFailed ? '重试保存' : '保存' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { api } from '../api'
import { store } from '../store'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, keyword: '', role: null, status: null })

const logs = ref([])
const logTotal = ref(0)
const logLoading = ref(false)
const logQuery = reactive({ current: 1, size: 10 })

const visible = ref(false)
const saving = ref(false)
const saveFailed = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, username: '', password: '', role: 0, nickname: '', phone: '', status: 1 })

const USERNAME_RE = /^[A-Za-z0-9_]{2,32}$/
const PHONE_RE = /^1[3-9]\d{9}$/

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        USERNAME_RE.test(value) ? cb() : cb(new Error('2-32 位字母、数字或下划线')),
      trigger: 'blur'
    }
  ],
  password: [
    {
      validator: (rule, value, cb) => {
        if (form.id && !value) return cb()
        if (!value) return cb(new Error('请输入登录密码'))
        if (value.length < 6 || value.length > 32) return cb(new Error('密码长度需为 6-32 位'))
        return cb()
      },
      trigger: 'blur'
    }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  nickname: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { max: 20, message: '不能超过 20 个字符', trigger: 'blur' }
  ],
  phone: [
    {
      validator: (rule, value, cb) => (!value || PHONE_RE.test(value) ? cb() : cb(new Error('手机号格式不正确'))),
      trigger: 'blur'
    }
  ]
}

function fmt(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '-'
}

async function load(page) {
  if (page) query.current = page
  loading.value = true
  try {
    const data = await api.adminUsers(query)
    rows.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

async function loadLogs(page) {
  if (page) logQuery.current = page
  logLoading.value = true
  try {
    const data = await api.adminLogs(logQuery)
    logs.value = data.records || []
    logTotal.value = data.total || 0
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    logLoading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.role = null
  query.status = null
  load(1)
}

function resetForm() {
  Object.assign(form, { id: null, username: '', password: '', role: 0, nickname: '', phone: '', status: 1 })
  saveFailed.value = false
  formRef.value?.clearValidate()
}

function openForm(row) {
  resetForm()
  if (row) {
    Object.assign(form, {
      id: row.id,
      username: row.username,
      password: '',
      role: row.role,
      nickname: row.nickname,
      phone: row.phone || '',
      status: row.status
    })
  }
  visible.value = true
}

async function save() {
  await formRef.value.validate(async (ok) => {
    if (!ok) return
    saving.value = true
    saveFailed.value = false
    try {
      const payload = {
        username: form.username,
        password: form.password || undefined,
        role: form.role,
        nickname: form.nickname,
        phone: form.phone || '',
        status: form.status
      }
      if (form.id) await api.adminUpdateUser(form.id, payload)
      else await api.adminCreateUser(payload)
      ElMessage.success('保存成功')
      visible.value = false
      await load()
      await loadLogs(1)
    } catch (e) {
      saveFailed.value = true
      ElMessage.error(e.message)
    } finally {
      saving.value = false
    }
  })
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  if (next === 0) {
    try {
      await ElMessageBox.confirm(`禁用后「${row.username}」将无法登录，确定禁用吗？`, '禁用账号', {
        type: 'warning',
        confirmButtonText: '禁用'
      })
    } catch {
      return
    }
  }
  try {
    await api.adminUserStatus(row.id, next)
    ElMessage.success(next === 1 ? '账号已启用' : '账号已禁用')
    await load()
    await loadLogs(1)
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`删除后「${row.username}」将无法登录，确定删除吗？`, '删除用户', {
      type: 'warning',
      confirmButtonText: '删除'
    })
  } catch {
    return
  }
  try {
    await api.adminDeleteUser(row.id)
    ElMessage.success('用户已删除')
    if (rows.value.length === 1 && query.current > 1) query.current--
    await load()
    await loadLogs(1)
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(() => {
  load()
  loadLogs()
})
</script>
