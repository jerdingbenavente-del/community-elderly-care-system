<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { confirmServiceOrder } from '@/api/serviceOrder'
import { pageCareStaff } from '@/api/careStaff'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  order: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const loading = ref(false)
const submitting = ref(false)
const staffOptions = ref([])
const form = reactive({
  careStaffId: null,
})

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      form.careStaffId = null
      staffOptions.value = []
      return
    }
    form.careStaffId = null
    loadStaff()
  },
)

async function loadStaff() {
  loading.value = true
  try {
    const res = await pageCareStaff({ page: 1, size: 100, status: 1 })
    if (res?.code !== 200) throw new Error(res?.message || '加载护理员失败')
    staffOptions.value = (res.data?.records || []).map((s) => ({
      id: s.id,
      label: `${s.name || '-'}${s.employeeNo ? `（${s.employeeNo}）` : ''}`,
      disabled: s.status !== 1,
    }))
  } catch (e) {
    staffOptions.value = []
    if (!e?.toastShown) ElMessage.error(e.message || '加载护理员失败')
  } finally {
    loading.value = false
  }
}

function close() {
  visible.value = false
}

async function submit() {
  if (!props.order?.id) return
  if (!form.careStaffId) {
    ElMessage.warning('请选择护理员')
    return
  }
  submitting.value = true
  try {
    const res = await confirmServiceOrder(props.order.id, {
      careStaffId: form.careStaffId,
    })
    if (res?.code !== 200) throw new Error(res?.message || '确认失败')
    ElMessage.success('订单已确认并分配护理员')
    emit('success')
    close()
  } catch (e) {
    if (!e?.toastShown) ElMessage.error(e.message || '确认失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-drawer v-model="visible" title="确认订单并分配护理员" size="440px" destroy-on-close>
    <div v-loading="loading" class="body">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="tip"
        title="确认后订单变为「已确认」。护理员须在预约时段有有效排班，且无订单时间冲突（由后端校验）。"
      />

      <div v-if="order" class="summary">
        <div><span>订单号</span><code>{{ order.orderNo }}</code></div>
        <div><span>老人</span>{{ order.elderName || '-' }}</div>
        <div><span>服务</span>{{ order.serviceName || '-' }}</div>
        <div>
          <span>预约</span>
          {{ order.scheduledStartTime || '-' }} ~ {{ order.scheduledEndTime || '-' }}
        </div>
      </div>

      <el-form label-width="90px">
        <el-form-item label="护理员" required>
          <el-select
            v-model="form.careStaffId"
            filterable
            clearable
            placeholder="选择在职护理员"
            style="width: 100%"
          >
            <el-option
              v-for="o in staffOptions"
              :key="o.id"
              :label="o.label"
              :value="o.id"
              :disabled="o.disabled"
            />
          </el-select>
          <p v-if="!loading && !staffOptions.length" class="hint">
            暂无在职护理员，请先在护理员管理中维护档案。
          </p>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">确认并分配</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 160px;
}

.tip {
  border-radius: 10px;
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(91, 184, 176, 0.08);
  font-size: 13px;
}

.summary span {
  display: inline-block;
  width: 56px;
  color: var(--ec-text-secondary, #6b7280);
}

.hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #b45309;
}
</style>
