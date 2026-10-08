<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createCareStaff, getCareStaff, pageCareStaff, updateCareStaff } from '@/api/careStaff'
import { pageSystemUsers } from '@/api/systemUser'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  /** null = 新增 */
  staffId: { type: Number, default: null },
})
const emit = defineEmits(['update:modelValue', 'success'])
const router = useRouter()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const isEdit = computed(() => props.staffId != null)
const title = computed(() => (isEdit.value ? '编辑护理员档案' : '新增护理员档案'))

const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)

/** 新增：可选 CARE_STAFF 账号 */
const accountLoading = ref(false)
const accountOptions = ref([])
const boundUserIds = ref(new Set())

const form = reactive({
  userId: null,
  employeeNo: '',
  name: '',
  gender: null,
  phone: '',
  position: '',
  remark: '',
  /** 编辑只读展示 */
  username: '',
})

const rules = {
  userId: [
    {
      validator: (_r, v, cb) => {
        if (isEdit.value) return cb()
        if (v == null) return cb(new Error('请选择关联账号'))
        cb()
      },
      trigger: 'change',
    },
  ],
  employeeNo: [
    {
      validator: (_r, v, cb) => {
        if (isEdit.value) return cb()
        if (!v || !String(v).trim()) return cb(new Error('请输入工号'))
        if (String(v).trim().length > 32) return cb(new Error('工号长度不能超过32'))
        cb()
      },
      trigger: 'blur',
    },
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 64, message: '姓名长度不能超过64', trigger: 'blur' },
  ],
  phone: [
    {
      validator: (_r, v, cb) => {
        if (!v) return cb()
        if (String(v).length > 20) return cb(new Error('手机号长度不能超过20'))
        cb()
      },
      trigger: 'blur',
    },
  ],
}

function resetForm() {
  form.userId = null
  form.employeeNo = ''
  form.name = ''
  form.gender = null
  form.phone = ''
  form.position = ''
  form.remark = ''
  form.username = ''
  accountOptions.value = []
  boundUserIds.value = new Set()
  formRef.value?.clearValidate?.()
}

async function loadBoundUserIds() {
  const ids = new Set()
  let page = 1
  const size = 100
  // 有限页拉取已绑定 userId，用于禁用选项；最终仍以后端为准
  for (let i = 0; i < 5; i++) {
    const res = await pageCareStaff({ page, size })
    if (res?.code !== 200) break
    const records = res.data?.records || []
    records.forEach((r) => {
      if (r.userId != null) ids.add(r.userId)
    })
    const total = Number(res.data?.total || 0)
    if (page * size >= total) break
    page += 1
  }
  boundUserIds.value = ids
}

async function loadAccounts() {
  accountLoading.value = true
  try {
    await loadBoundUserIds()
    const res = await pageSystemUsers({
      page: 1,
      size: 100,
      roleCode: 'CARE_STAFF',
      status: 1,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载账号失败')
    accountOptions.value = (res.data?.records || []).map((u) => ({
      id: u.id,
      username: u.username,
      realName: u.realName,
      disabled: boundUserIds.value.has(u.id),
      label: `${u.realName || '-'}（${u.username}）`,
    }))
  } catch (e) {
    accountOptions.value = []
    ElMessage.error(e.message || '加载护理员账号失败')
  } finally {
    accountLoading.value = false
  }
}

async function loadDetail() {
  if (!props.staffId) return
  loading.value = true
  try {
    const res = await getCareStaff(props.staffId)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    const d = res.data || {}
    form.name = d.name || ''
    form.gender = d.gender ?? null
    form.phone = d.phone || ''
    form.position = d.position || ''
    form.remark = d.remark || ''
    form.employeeNo = d.employeeNo || ''
    form.userId = d.userId ?? null
    form.username = d.username || ''
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
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
    if (props.staffId) {
      loadDetail()
    } else {
      loadAccounts()
    }
  },
)

function close() {
  visible.value = false
}

function goUsers() {
  close()
  router.push('/admin/users')
}

const availableCount = computed(
  () => accountOptions.value.filter((o) => !o.disabled).length,
)

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateCareStaff(props.staffId, {
        name: form.name.trim(),
        gender: form.gender ?? undefined,
        phone: form.phone?.trim() || undefined,
        position: form.position?.trim() || undefined,
        remark: form.remark?.trim() || undefined,
      })
    } else {
      res = await createCareStaff({
        userId: form.userId,
        employeeNo: form.employeeNo.trim(),
        name: form.name.trim(),
        gender: form.gender ?? undefined,
        phone: form.phone?.trim() || undefined,
        position: form.position?.trim() || undefined,
        remark: form.remark?.trim() || undefined,
      })
    }
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    ElMessage.success(isEdit.value ? '保存成功' : '新增成功')
    emit('success', res.data)
    close()
  } catch (e) {
    const msg = e.message || '保存失败'
    if (msg.includes('工号')) {
      ElMessage.error(msg.includes('重复') ? '该工号已存在，请使用其他工号' : msg)
    } else {
      ElMessage.error(msg)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-drawer v-model="visible" :title="title" size="480px" destroy-on-close @close="resetForm">
    <div v-loading="loading" class="drawer-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <template v-if="!isEdit">
          <el-form-item label="关联账号" prop="userId">
            <el-select
              v-model="form.userId"
              filterable
              clearable
              :loading="accountLoading"
              placeholder="选择 CARE_STAFF 账号"
              style="width: 100%"
            >
              <el-option
                v-for="o in accountOptions"
                :key="o.id"
                :label="o.label"
                :value="o.id"
                :disabled="o.disabled"
              >
                <span>{{ o.label }}</span>
                <span v-if="o.disabled" class="opt-disabled">已绑定</span>
              </el-option>
            </el-select>
            <p v-if="!accountLoading && availableCount === 0" class="hint warn">
              暂无可关联的护理员账号。请先前往
              <el-button type="primary" link @click="goUsers">账号管理</el-button>
              创建 CARE_STAFF 账号。
            </p>
          </el-form-item>
          <el-form-item label="工号" prop="employeeNo">
            <el-input v-model="form.employeeNo" maxlength="32" placeholder="必填，不可与已有工号重复" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="关联账号">
            <el-input :model-value="form.username ? `${form.username}` : '未关联系统账号'" disabled />
          </el-form-item>
          <el-form-item label="工号">
            <el-input :model-value="form.employeeNo || '-'" disabled />
          </el-form-item>
        </template>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="20" placeholder="选填" />
        </el-form-item>
        <el-form-item label="岗位" prop="position">
          <el-input v-model="form.position" maxlength="64" placeholder="选填，如护理员" />
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
  min-height: 160px;
}

.hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--ec-text-secondary, #6b7280);
}

.hint.warn {
  color: #b45309;
}

.opt-disabled {
  float: right;
  color: #9ca3af;
  font-size: 12px;
}
</style>
