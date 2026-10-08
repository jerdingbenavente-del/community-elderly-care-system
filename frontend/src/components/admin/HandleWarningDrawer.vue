<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { handleHealthWarning } from '@/api/healthWarning'
import {
  directionLabel,
  indicatorLabel,
  warningLevelMeta,
  warningStatusMeta,
} from '@/utils/familyHome'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  warning: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  handlingResult: '',
})

const rules = {
  handlingResult: [
    { required: true, message: '请填写处理备注', trigger: 'blur' },
    { max: 500, message: '处理备注不能超过500字', trigger: 'blur' },
  ],
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      form.handlingResult = ''
      formRef.value?.clearValidate?.()
    }
  },
)

function alertTitle(item) {
  if (!item) return '-'
  const name = indicatorLabel(item.indicator)
  const dir = directionLabel(item.direction)
  return dir ? `${name}异常（${dir}）` : `${name}异常`
}

function close() {
  visible.value = false
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!props.warning?.id) return
  submitting.value = true
  try {
    const res = await handleHealthWarning(props.warning.id, {
      handlingResult: form.handlingResult.trim(),
    })
    if (res?.code !== 200) throw new Error(res?.message || '处理失败')
    ElMessage.success('预警已处理')
    emit('success')
    close()
  } catch (e) {
    // 业务错误已由 request.js 提示（含 409 冲突）
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-drawer v-model="visible" title="处理健康预警" size="440px" destroy-on-close>
    <div v-if="warning" class="summary">
      <div><span>老人</span>{{ warning.elderName || '-' }}</div>
      <div><span>类型</span>{{ alertTitle(warning) }}</div>
      <div>
        <span>等级</span>{{ warningLevelMeta(warning.warningLevel).label }}
      </div>
      <div><span>实测</span>{{ warning.actualValue ?? '-' }}</div>
      <div><span>阈值</span>{{ warning.thresholdDesc || '-' }}</div>
      <div>
        <span>状态</span>{{ warningStatusMeta(warning.status).label }}
      </div>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="form">
      <el-form-item label="处理备注" prop="handlingResult">
        <el-input
          v-model="form.handlingResult"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="必填，记录处理说明（非医疗诊断）"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认处理</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: rgba(242, 166, 90, 0.1);
  font-size: 13px;
}

.summary span {
  display: inline-block;
  width: 48px;
  color: var(--ec-text-secondary, #6b7280);
}

.form {
  padding-right: 4px;
}
</style>
