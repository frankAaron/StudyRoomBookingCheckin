<template>
  <div class="page">
    <div class="page-title">学习目标</div>
    <div class="page-sub">设定周期目标，完成情况将根据打卡时长自动更新</div>

    <div class="filters">
      <el-button type="primary" :icon="Plus" @click="openForm()">新建目标</el-button>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </div>

    <div v-loading="loading" style="display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px">
      <div v-for="goal in goals" :key="goal.id" class="card" style="margin: 0">
        <div style="display: flex; align-items: flex-start; gap: 10px">
          <div style="flex: 1">
            <div style="font-weight: 700; font-size: 15.5px">{{ goal.title }}</div>
            <div class="muted" style="font-size: 12.5px; margin-top: 4px">
              {{ goal.startDate }} ~ {{ goal.endDate }}
            </div>
          </div>
          <el-tag :type="goal.status === 1 ? 'success' : 'primary'" size="small">
            {{ goal.status === 1 ? '已完成' : '进行中' }}
          </el-tag>
        </div>
        <div style="margin: 16px 0 8px; display: flex; justify-content: space-between; font-size: 12.5px">
          <span class="muted">完成进度</span>
          <strong>{{ goal.progressPercent }}%</strong>
        </div>
        <el-progress :percentage="goal.progressPercent" :stroke-width="10" :show-text="false" />
        <div class="muted" style="font-size: 12.5px; margin-top: 8px">
          {{ fmt(goal.completedMinutes) }} / {{ fmt(goal.targetMinutes) }}
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px">
          <el-button size="small" @click="openForm(goal)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="remove(goal)">删除</el-button>
        </div>
      </div>
    </div>
    <div v-if="!loading && !goals.length" class="card empty">还没有学习目标，点击「新建目标」开始吧</div>

    <el-dialog v-model="visible" :title="form.id ? '编辑目标' : '新建目标'" width="440px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="目标名称" prop="title">
          <el-input v-model="form.title" maxlength="40" placeholder="如：本周学习 20 小时" />
        </el-form-item>
        <el-form-item label="目标时长" prop="hours">
          <el-input-number v-model="form.hours" :min="0.5" :max="500" :step="0.5" style="width: 100%" />
          <div class="muted" style="font-size: 12px; margin-top: 4px">单位：小时</div>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { api } from '../api'

const goals = ref([])
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const formRef = ref(null)

const form = reactive({ id: null, title: '', hours: 10, startDate: '', endDate: '' })

const rules = {
  title: [{ required: true, message: '请输入目标名称', trigger: 'blur' }],
  hours: [{ required: true, message: '请输入目标时长', trigger: 'change' }],
  endDate: [
    {
      validator: (rule, value, cb) =>
        !value || !form.startDate || value >= form.startDate
          ? cb()
          : cb(new Error('结束日期不能早于开始日期')),
      trigger: 'change'
    }
  ]
}

function fmt(min) {
  const v = Number(min || 0)
  if (v < 60) return `${v} 分钟`
  return `${(v / 60).toFixed(1)} 小时`
}

async function load() {
  loading.value = true
  try {
    goals.value = await api.goals()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function openForm(goal) {
  const today = new Date().toISOString().slice(0, 10)
  const later = new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10)
  Object.assign(form, {
    id: null,
    title: '',
    hours: 10,
    startDate: today,
    endDate: later
  })
  if (goal) {
    Object.assign(form, {
      id: goal.id,
      title: goal.title,
      hours: Math.round((goal.targetMinutes / 60) * 10) / 10,
      startDate: goal.startDate,
      endDate: goal.endDate
    })
  }
  visible.value = true
}

async function save() {
  await formRef.value.validate(async (ok) => {
    if (!ok) return
    saving.value = true
    try {
      const payload = {
        title: form.title,
        targetMinutes: Math.round(form.hours * 60),
        startDate: form.startDate,
        endDate: form.endDate
      }
      if (form.id) await api.updateGoal(form.id, payload)
      else await api.createGoal(payload)
      ElMessage.success('保存成功')
      visible.value = false
      load()
    } catch (e) {
      ElMessage.error(e.message)
    } finally {
      saving.value = false
    }
  })
}

async function remove(goal) {
  try {
    await ElMessageBox.confirm(`确定删除目标「${goal.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除'
    })
  } catch {
    return
  }
  try {
    await api.deleteGoal(goal.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

onMounted(load)
</script>
