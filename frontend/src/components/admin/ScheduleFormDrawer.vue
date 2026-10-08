<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createCareStaffSchedule,
  getCareStaffSchedule,
  updateCareStaffSchedule,
} from '@/api/careStaffSchedule'
import { pageCareStaff } from '@/api/careStaff'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  scheduleId: { type: Number, default: null },
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => props.scheduleId != null)
const title = computed(() => (isEdit.value ? '编辑排班' : '新增排班'))

const loading = ref(false)
const submitting = ref(false)
const staffLoading = ref(false)
const staffOptions = ref([])
const formRef = ref(null)

const form = reactive({
  careStaffId: null,
  careStaffName: '',
  scheduleDate: '',
  startTime: '',
  endTime: '',
  status: 'AVAILABLE',
  remark: '',
})

const rules = {
  careStaffId: [
    {
      validator: (_r, v, cb) => {
        if (isEdit.value) return cb()
        if (v == null) return cb(new Error('请选择护理员'))
        cb()
      },
      trigger: 'change',
    },
  ],
  scheduleDate: [{ required: true, message: '请选择排班日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  status: [
    {
      validator: (_r, v, cb) => {
        if (!isEdit.value) return cb()
        if (v !== 'AVAILABLE' && v !== 'CANCELLED') return cb(new Error('请选择状态'))
        cb()
      },
      trigger: 'change',
    },
  ],
}

function resetForm() {
  form.careStaffId = null
  form.careStaffName = ''
  form.scheduleDate = ''
  form.startTime = ''
  form.endTime = ''
  form.status = 'AVAILABLE'
  form.remark = ''
  formRef.value?.clearValidate?.()
}

function normalizeTime(v) {
  if (!v) return ''
  const s = String(v)
  // Element may return HH:mm; backend needs HH:mm:ss
  if (/^\d{2}:\d{2}$/.test(s)) return `${s}:00`
  return s
}

async function loadStaff() {
  staffLoading.value = true
  try {
    const res = await pageCareStaff({ page: 1, size: 100, status: 1 })
    if (res?.code !== 200) throw new Error(res?.message || '加载护理员失败')
    staffOptions.value = (res.data?.records || []).map((s) => ({
      id: s.id,
      label: `${s.name || '-'}${s.employeeNo ? `（${s.employeeNo}）` : ''}`,
    }))
  } catch (e) {
    staffOptions.value = []
    if (!e?.toastShown) ElMessage.error(e.message || '加载护理员失败')
  } finally {
    staffLoading.value = false
  }
}

async function loadDetail() {
  if (!props.scheduleId) return
  loading.value = true
  try {
    const res = await getCareStaffSchedule(props.scheduleId)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    const d = res.data || {}
    form.careStaffId = d.careStaffId ?? null
    form.careStaffName = d.careStaffName || ''
    form.scheduleDate = d.scheduleDate || ''
    form.startTime = d.startTime || ''
    form.endTime = d.endTime || ''
    form.status = d.status === 'CANCELLED' ? 'CANCELLED' : 'AVAILABLE'
    form.remark = d.remark || ''
  } catch (e) {
    if (!e?.toastShown) ElMessage.error(e.message || '加载失败')
    visible.value = false
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      resetForm()
      return
    }
    resetForm()
    if (props.scheduleId) {
      loadDetail()
    } else {
      loadStaff()
    }
  },
)

function close() {
  visible.value = false
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const start = normalizeTime(form.startTime)
  const end = normalizeTime(form.endTime)
  if (start >= end) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateCareStaffSchedule(props.scheduleId, {
        scheduleDate: form.scheduleDate,
        startTime: start,
        endTime: end,
        status: form.status,
        remark: form.remark?.trim() || undefined,
      })
    } else {
      res = await createCareStaffSchedule({
        careStaffId: form.careStaffId,
        scheduleDate: form.scheduleDate,
        startTime: start,
        endTime: end,
        remark: form.remark?.trim() || undefined,
      })
    }
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    ElMessage.success(isEdit.value ? '保存成功' : '新增成功')
    emit('success', res.data)
    close()
  } catch (e) {
    if (!e?.toastShown) ElMessage.error(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-drawer v-model="visible" :title="title" size="460px" destroy-on-close @close="resetForm">
    <div v-loading="loading" class="drawer-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="!isEdit" label="护理员" prop="careStaffId">
          <el-select
            v-model="form.careStaffId"
            filterable
            :loading="staffLoading"
            placeholder="选择在职护理员"
            style="width: 100%"
          >
            <el-option
              v-for="o in staffOptions"
              :key="o.id"
              :label="o.label"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="护理员">
          <el-input :model-value="form.careStaffName || `ID ${form.careStaffId}`" disabled />
        </el-form-item>

        <el-form-item label="排班日期" prop="scheduleDate">
          <el-date-picker
            v-model="form.scheduleDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="开始时间" prop="startTime">
          <el-time-picker
            v-model="form.startTime"
            value-format="HH:mm:ss"
            placeholder="开始"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束时间" prop="endTime">
          <el-time-picker
            v-model="form.endTime"
            value-format="HH:mm:ss"
            placeholder="结束"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="AVAILABLE">可用</el-radio>
            <el-radio value="CANCELLED">已取消</el-radio>
          </el-radio-group>
        </el-form-item>
        <p v-else class="hint">新建后默认为「可用」。重叠时段将由后端拒绝。</p>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.drawer-body {
  min-height: 160px;
}

.hint {
  margin: 0 0 12px 100px;
  font-size: 12px;
  color: var(--ec-text-secondary, #6b7280);
}
</style>
