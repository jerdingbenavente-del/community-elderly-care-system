<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createEmergencyContact,
  updateEmergencyContact,
} from '@/api/adminElder'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  elderId: { type: Number, required: true },
  /** null = 新增 */
  contact: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => Boolean(props.contact?.id))
const title = computed(() => (isEdit.value ? '编辑紧急联系人' : '新增紧急联系人'))

const formRef = ref(null)
const submitting = ref(false)
const form = reactive({
  name: '',
  relationship: '',
  phone: '',
  priority: 1,
  remark: '',
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      pattern: /^1\d{10}$/,
      message: '手机号格式不正确',
      trigger: 'blur',
    },
  ],
}

function reset() {
  form.name = ''
  form.relationship = ''
  form.phone = ''
  form.priority = 1
  form.remark = ''
  formRef.value?.clearValidate?.()
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      reset()
      return
    }
    if (props.contact) {
      form.name = props.contact.name || ''
      form.relationship = props.contact.relationship || ''
      form.phone = props.contact.phone || ''
      form.priority = props.contact.priority ?? 1
      form.remark = props.contact.remark || ''
    } else {
      reset()
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
  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      relationship: form.relationship.trim() || undefined,
      phone: form.phone.trim(),
      priority: form.priority ?? 1,
      remark: form.remark.trim() || undefined,
    }
    let res
    if (isEdit.value) {
      res = await updateEmergencyContact(props.contact.id, payload)
    } else {
      res = await createEmergencyContact(props.elderId, payload)
    }
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    ElMessage.success(isEdit.value ? '保存成功' : '新增成功')
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
  <el-drawer v-model="visible" :title="title" size="420px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" maxlength="64" />
      </el-form-item>
      <el-form-item label="关系" prop="relationship">
        <el-input v-model="form.relationship" maxlength="32" placeholder="如：子女、配偶" />
      </el-form-item>
      <el-form-item label="手机" prop="phone">
        <el-input v-model="form.phone" maxlength="11" />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input-number v-model="form.priority" :min="1" :max="99" />
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>
