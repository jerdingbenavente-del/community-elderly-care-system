<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createCareServiceItem,
  getCareServiceItem,
  updateCareServiceItem,
} from '@/api/careServiceItem'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  itemId: { type: Number, default: null },
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => props.itemId != null)
const title = computed(() => (isEdit.value ? '编辑照护服务项目' : '新增照护服务项目'))

const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const typeOptions = [
  { label: '生活照料', value: 'DAILY' },
  { label: '康复服务', value: 'REHAB' },
  { label: '健康护理', value: 'HEALTH' },
  { label: '陪伴服务', value: 'COMPANION' },
]

const form = reactive({
  serviceCode: '',
  serviceName: '',
  serviceType: '',
  description: '',
  durationMinutes: 60,
  price: null,
  status: 'ENABLED',
})

const rules = {
  serviceCode: [
    {
      validator: (_r, v, cb) => {
        if (isEdit.value) return cb()
        if (!v || !String(v).trim()) return cb(new Error('请输入服务编码'))
        if (String(v).trim().length > 64) return cb(new Error('编码长度不能超过64'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  serviceName: [
    { required: true, message: '请输入服务名称', trigger: 'blur' },
    { max: 128, message: '名称长度不能超过128', trigger: 'blur' },
  ],
  durationMinutes: [
    { required: true, message: '请输入服务时长', trigger: 'blur' },
    {
      type: 'number',
      min: 1,
      message: '服务时长必须大于0',
      trigger: 'blur',
    },
  ],
  price: [
    {
      validator: (_r, v, cb) => {
        if (v === null || v === undefined || v === '') return cb()
        const n = Number(v)
        if (Number.isNaN(n)) return cb(new Error('价格格式不正确'))
        if (n < 0) return cb(new Error('价格不能为负数'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  status: [
    {
      validator: (_r, v, cb) => {
        if (!isEdit.value) return cb()
        if (v !== 'ENABLED' && v !== 'DISABLED') return cb(new Error('请选择状态'))
        cb()
      },
      trigger: 'change',
    },
  ],
}

function resetForm() {
  form.serviceCode = ''
  form.serviceName = ''
  form.serviceType = ''
  form.description = ''
  form.durationMinutes = 60
  form.price = null
  form.status = 'ENABLED'
  formRef.value?.clearValidate?.()
}

async function loadDetail() {
  if (!props.itemId) return
  loading.value = true
  try {
    const res = await getCareServiceItem(props.itemId)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    const d = res.data || {}
    form.serviceCode = d.serviceCode || ''
    form.serviceName = d.serviceName || ''
    form.serviceType = d.serviceType || ''
    form.description = d.description || ''
    form.durationMinutes = d.durationMinutes ?? 60
    form.price = d.price != null ? Number(d.price) : null
    form.status = d.status === 'DISABLED' ? 'DISABLED' : 'ENABLED'
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
    if (props.itemId) loadDetail()
  },
)

function close() {
  visible.value = false
}

function buildPayload() {
  const payload = {
    serviceName: form.serviceName.trim(),
    serviceType: form.serviceType || undefined,
    description: form.description?.trim() || undefined,
    durationMinutes: Number(form.durationMinutes),
  }
  if (form.price !== null && form.price !== undefined && form.price !== '') {
    payload.price = Number(form.price)
  } else {
    payload.price = null
  }
  if (isEdit.value) {
    payload.status = form.status
  } else {
    payload.serviceCode = form.serviceCode.trim()
  }
  return payload
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const payload = buildPayload()
    let res
    if (isEdit.value) {
      res = await updateCareServiceItem(props.itemId, payload)
    } else {
      res = await createCareServiceItem(payload)
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
  <el-drawer v-model="visible" :title="title" size="500px" destroy-on-close @close="resetForm">
    <div v-loading="loading" class="drawer-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="!isEdit" label="服务编码" prop="serviceCode">
          <el-input v-model="form.serviceCode" maxlength="64" placeholder="唯一编码，创建后不可修改" />
        </el-form-item>
        <el-form-item v-else label="服务编码">
          <el-input :model-value="form.serviceCode" disabled />
        </el-form-item>

        <el-form-item label="服务名称" prop="serviceName">
          <el-input v-model="form.serviceName" maxlength="128" show-word-limit />
        </el-form-item>

        <el-form-item label="服务类型" prop="serviceType">
          <el-select v-model="form.serviceType" clearable placeholder="选填" style="width: 100%">
            <el-option v-for="o in typeOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="服务说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="时长(分钟)" prop="durationMinutes">
          <el-input-number v-model="form.durationMinutes" :min="1" :max="24 * 60" style="width: 100%" />
        </el-form-item>

        <el-form-item label="参考价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            :step="1"
            controls-position="right"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <p v-else class="hint">新建后默认为「启用」状态。</p>
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
  margin: 0 0 0 100px;
  font-size: 12px;
  color: var(--ec-text-secondary, #6b7280);
}
</style>
