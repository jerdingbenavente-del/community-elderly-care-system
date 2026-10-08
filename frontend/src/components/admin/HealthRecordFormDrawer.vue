<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createHealthRecord,
  formatMeasuredAtBody,
  getHealthRecord,
  updateHealthRecord,
} from '@/api/health'
import { pageElders } from '@/api/adminElder'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  recordId: { type: Number, default: null },
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => props.recordId != null)
const title = computed(() => (isEdit.value ? '编辑健康记录' : '新增健康记录'))

const loading = ref(false)
const submitting = ref(false)
const elderLoading = ref(false)
const elderOptions = ref([])
const formRef = ref(null)

const form = reactive({
  elderId: null,
  elderName: '',
  measuredAt: '',
  systolicPressure: null,
  diastolicPressure: null,
  bloodGlucose: null,
  bodyTemperature: null,
  heartRate: null,
  remark: '',
})

function hasAnyIndicator() {
  return [
    form.systolicPressure,
    form.diastolicPressure,
    form.bloodGlucose,
    form.bodyTemperature,
    form.heartRate,
  ].some((v) => v !== null && v !== undefined && v !== '')
}

const rules = {
  elderId: [
    {
      validator: (_r, v, cb) => {
        if (isEdit.value) return cb()
        if (v == null) return cb(new Error('请选择老人'))
        cb()
      },
      trigger: 'change',
    },
  ],
  measuredAt: [{ required: true, message: '请选择测量时间', trigger: 'change' }],
  systolicPressure: [
    {
      validator: (_r, v, cb) => {
        if (v === null || v === undefined || v === '') return cb()
        const n = Number(v)
        if (!Number.isInteger(n) || n < 40 || n > 250) return cb(new Error('收缩压范围 40-250'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  diastolicPressure: [
    {
      validator: (_r, v, cb) => {
        if (v === null || v === undefined || v === '') return cb()
        const n = Number(v)
        if (!Number.isInteger(n) || n < 20 || n > 150) return cb(new Error('舒张压范围 20-150'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  bloodGlucose: [
    {
      validator: (_r, v, cb) => {
        if (v === null || v === undefined || v === '') return cb()
        const n = Number(v)
        if (Number.isNaN(n) || n < 0.5 || n > 40) return cb(new Error('血糖范围 0.5-40'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  bodyTemperature: [
    {
      validator: (_r, v, cb) => {
        if (v === null || v === undefined || v === '') return cb()
        const n = Number(v)
        if (Number.isNaN(n) || n < 30 || n > 45) return cb(new Error('体温范围 30.0-45.0'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  heartRate: [
    {
      validator: (_r, v, cb) => {
        if (v === null || v === undefined || v === '') return cb()
        const n = Number(v)
        if (!Number.isInteger(n) || n < 20 || n > 250) return cb(new Error('心率范围 20-250'))
        cb()
      },
      trigger: 'blur',
    },
  ],
}

function resetForm() {
  form.elderId = null
  form.elderName = ''
  form.measuredAt = ''
  form.systolicPressure = null
  form.diastolicPressure = null
  form.bloodGlucose = null
  form.bodyTemperature = null
  form.heartRate = null
  form.remark = ''
  formRef.value?.clearValidate?.()
}

function numOrNull(v) {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isNaN(n) ? null : n
}

async function loadElders(keyword) {
  elderLoading.value = true
  try {
    const res = await pageElders({
      page: 1,
      size: 20,
      status: 1,
      name: keyword?.trim() || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载老人失败')
    elderOptions.value = (res.data?.records || []).map((e) => ({
      id: e.id,
      label: e.name || `老人#${e.id}`,
    }))
  } catch {
    elderOptions.value = []
  } finally {
    elderLoading.value = false
  }
}

async function loadDetail() {
  if (!props.recordId) return
  loading.value = true
  try {
    const res = await getHealthRecord(props.recordId)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    const d = res.data || {}
    form.elderId = d.elderId
    form.elderName = d.elderName || ''
    form.measuredAt = formatMeasuredAtBody(d.measuredAt)
    form.systolicPressure = d.systolicPressure ?? null
    form.diastolicPressure = d.diastolicPressure ?? null
    form.bloodGlucose = d.bloodGlucose ?? null
    form.bodyTemperature = d.bodyTemperature ?? null
    form.heartRate = d.heartRate ?? null
    form.remark = d.remark || ''
  } catch (e) {
    if (!e?.toastShown) ElMessage.error(e.message || '加载失败')
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
    loadElders()
    if (isEdit.value) loadDetail()
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
  if (!hasAnyIndicator()) {
    ElMessage.warning('至少填写一项健康指标')
    return
  }
  const sys = numOrNull(form.systolicPressure)
  const dia = numOrNull(form.diastolicPressure)
  if (sys != null && dia != null && sys < dia) {
    ElMessage.warning('收缩压不能小于舒张压')
    return
  }
  submitting.value = true
  try {
    const body = {
      measuredAt: formatMeasuredAtBody(form.measuredAt),
      systolicPressure: sys,
      diastolicPressure: dia,
      bloodGlucose: numOrNull(form.bloodGlucose),
      bodyTemperature: numOrNull(form.bodyTemperature),
      heartRate: numOrNull(form.heartRate),
      remark: form.remark?.trim() || undefined,
    }
    let res
    if (isEdit.value) {
      res = await updateHealthRecord(props.recordId, body)
    } else {
      res = await createHealthRecord({ ...body, elderId: form.elderId })
    }
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    ElMessage.success(isEdit.value ? '修改成功' : '保存成功')
    emit('success')
    close()
  } catch (e) {
    if (!e?.toastShown) ElMessage.error(e.message || '保存失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-drawer v-model="visible" :title="title" size="480px" destroy-on-close>
    <div v-loading="loading">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="至少填写一项指标。异常值仍可保存，系统会按阈值自动生成健康预警（非医疗诊断）。"
        class="hint"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="form">
        <el-form-item v-if="isEdit" label="老人">
          <el-input :model-value="form.elderName || `老人#${form.elderId}`" disabled />
        </el-form-item>
        <el-form-item v-else label="老人" prop="elderId">
          <el-select
            v-model="form.elderId"
            filterable
            remote
            :remote-method="loadElders"
            :loading="elderLoading"
            placeholder="搜索并选择老人"
            style="width: 100%"
          >
            <el-option
              v-for="o in elderOptions"
              :key="o.id"
              :label="o.label"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="测量时间" prop="measuredAt">
          <el-date-picker
            v-model="form.measuredAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择测量时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="收缩压" prop="systolicPressure">
          <el-input-number v-model="form.systolicPressure" :min="40" :max="250" :controls="false" placeholder="mmHg" style="width: 100%" />
        </el-form-item>
        <el-form-item label="舒张压" prop="diastolicPressure">
          <el-input-number v-model="form.diastolicPressure" :min="20" :max="150" :controls="false" placeholder="mmHg" style="width: 100%" />
        </el-form-item>
        <el-form-item label="血糖" prop="bloodGlucose">
          <el-input-number v-model="form.bloodGlucose" :min="0.5" :max="40" :precision="1" :step="0.1" :controls="false" placeholder="mmol/L" style="width: 100%" />
        </el-form-item>
        <el-form-item label="体温" prop="bodyTemperature">
          <el-input-number v-model="form.bodyTemperature" :min="30" :max="45" :precision="1" :step="0.1" :controls="false" placeholder="℃" style="width: 100%" />
        </el-form-item>
        <el-form-item label="心率" prop="heartRate">
          <el-input-number v-model="form.heartRate" :min="20" :max="250" :controls="false" placeholder="次/分" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
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
.hint {
  margin-bottom: 16px;
  border-radius: 12px;
}

.form {
  padding-right: 8px;
}
</style>
