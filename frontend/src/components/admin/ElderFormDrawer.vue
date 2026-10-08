<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createElder, getElder, updateElder } from '@/api/adminElder'
import { maskIdCard } from '@/utils/sensitive'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** null = 新增；有值 = 编辑 */
  elderId: { type: Number, default: null },
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => props.elderId != null)
const title = computed(() => (isEdit.value ? '编辑老人档案' : '新增老人档案'))

const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)
/** 详情接口返回的脱敏身份证，仅展示 */
const maskedIdCard = ref('')

const form = reactive({
  name: '',
  gender: null,
  birthDate: '',
  phone: '',
  address: '',
  idCard: '',
  careLevel: '',
  status: 1,
  registeredAt: '',
  medicalHistory: '',
  allergyHistory: '',
  specialCareRequirement: '',
  remark: '',
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: false }],
  phone: [
    {
      validator: (_r, v, cb) => {
        if (!v) return cb()
        if (!/^1\d{10}$/.test(v)) return cb(new Error('手机号格式不正确'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  idCard: [
    {
      validator: (_r, v, cb) => {
        if (!v) return cb()
        if (!/^\d{17}[\dXx]$/.test(v)) return cb(new Error('身份证号格式不正确'))
        cb()
      },
      trigger: 'blur',
    },
  ],
}

function resetForm() {
  form.name = ''
  form.gender = null
  form.birthDate = ''
  form.phone = ''
  form.address = ''
  form.idCard = ''
  form.careLevel = ''
  form.status = 1
  form.registeredAt = ''
  form.medicalHistory = ''
  form.allergyHistory = ''
  form.specialCareRequirement = ''
  form.remark = ''
  maskedIdCard.value = ''
  formRef.value?.clearValidate?.()
}

function blankToUndef(v) {
  if (v == null) return undefined
  const s = String(v).trim()
  return s === '' ? undefined : s
}

function buildPayload() {
  const payload = {
    name: form.name.trim(),
    gender: form.gender ?? undefined,
    birthDate: blankToUndef(form.birthDate),
    phone: blankToUndef(form.phone),
    address: blankToUndef(form.address),
    careLevel: blankToUndef(form.careLevel),
    status: form.status ?? 1,
    registeredAt: blankToUndef(form.registeredAt),
    medicalHistory: blankToUndef(form.medicalHistory),
    allergyHistory: blankToUndef(form.allergyHistory),
    specialCareRequirement: blankToUndef(form.specialCareRequirement),
    remark: blankToUndef(form.remark),
  }
  // 编辑时留空：不传 idCard，避免用脱敏值覆盖；MP 跳过 null 可保留原值
  const idCard = blankToUndef(form.idCard)
  if (idCard) {
    payload.idCard = idCard
  } else if (!isEdit.value) {
    payload.idCard = ''
  }
  return payload
}

async function loadDetail() {
  if (!props.elderId) return
  loading.value = true
  try {
    const res = await getElder(props.elderId)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    const d = res.data || {}
    form.name = d.name || ''
    form.gender = d.gender ?? null
    form.birthDate = d.birthDate || ''
    form.phone = d.phone || ''
    form.address = d.address || ''
    form.careLevel = d.careLevel || ''
    form.status = d.status ?? 1
    form.registeredAt = d.registeredAt || ''
    form.medicalHistory = d.medicalHistory || ''
    form.allergyHistory = d.allergyHistory || ''
    form.specialCareRequirement = d.specialCareRequirement || ''
    form.remark = d.remark || ''
    const raw = d.idCard || ''
    maskedIdCard.value = raw.includes('*') ? raw : maskIdCard(raw)
    // 脱敏值不可回填提交
    form.idCard = raw && !raw.includes('*') ? raw : ''
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
    if (props.elderId) loadDetail()
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
  submitting.value = true
  try {
    const payload = buildPayload()
    let res
    if (isEdit.value) {
      res = await updateElder(props.elderId, payload)
    } else {
      res = await createElder(payload)
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
  <el-drawer v-model="visible" :title="title" size="520px" destroy-on-close @close="resetForm">
    <div v-loading="loading" class="drawer-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" label-position="right">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="64" show-word-limit placeholder="必填" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input
            v-model="form.idCard"
            maxlength="18"
            :placeholder="isEdit && maskedIdCard ? '留空则保持原值不变' : '选填，18 位'"
          />
          <p v-if="isEdit && maskedIdCard" class="hint">当前：{{ maskedIdCard }}（接口脱敏，勿把带 * 的值提交）</p>
        </el-form-item>
        <el-form-item label="住址" prop="address">
          <el-input v-model="form.address" maxlength="255" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="护理等级" prop="careLevel">
          <el-input v-model="form.careLevel" maxlength="32" placeholder="如：一级 / 二级" />
        </el-form-item>
        <el-form-item label="档案状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">档案正常</el-radio>
            <el-radio :value="0">已停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="建档日期" prop="registeredAt">
          <el-date-picker
            v-model="form.registeredAt"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="病史" prop="medicalHistory">
          <el-input v-model="form.medicalHistory" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="过敏史" prop="allergyHistory">
          <el-input v-model="form.allergyHistory" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
        <el-form-item label="特殊照护" prop="specialCareRequirement">
          <el-input v-model="form.specialCareRequirement" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
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
  min-height: 200px;
  padding-right: 8px;
}

.hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--ec-text-secondary, #6b7280);
  line-height: 1.5;
}
</style>
