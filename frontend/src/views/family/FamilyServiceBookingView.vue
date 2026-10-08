<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { listMyElders } from '@/api/elder'
import { getCareServiceDetail } from '@/api/careService'
import {
  createServiceOrder,
  getServiceOrder,
  formatOrderDateTime,
  calcScheduledEnd,
} from '@/api/serviceOrder'
import { unwrap } from '@/utils/familyHome'
import ElderSelectCard from '@/components/family/care/ElderSelectCard.vue'
import BookingSummary from '@/components/family/care/BookingSummary.vue'
import BookingSuccess from '@/components/family/care/BookingSuccess.vue'

const route = useRoute()
const router = useRouter()

const serviceId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const loading = ref(false)
const submitting = ref(false)
const step = ref(0)
const success = ref(false)

const item = ref(null)
const elders = ref([])
const order = ref(null)
const loadError = ref('')

const form = reactive({
  elderId: null,
  scheduledStartTime: '',
  remark: '',
})

const formRef = ref(null)

const selectedElder = computed(() => elders.value.find((e) => e.id === form.elderId) || null)

const scheduledEndTime = computed(() => {
  if (!form.scheduledStartTime || !item.value?.durationMinutes) return ''
  return calcScheduledEnd(form.scheduledStartTime, item.value.durationMinutes)
})

const durationMinutes = computed(() => item.value?.durationMinutes ?? '-')

const rules = {
  elderId: [{ required: true, message: '请选择老人', trigger: 'change' }],
  scheduledStartTime: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
}

function disabledDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function disabledHours() {
  if (!form.scheduledStartTime) return []
  const selected = new Date(String(form.scheduledStartTime).replace(' ', 'T'))
  const now = new Date()
  if (
    selected.getFullYear() !== now.getFullYear() ||
    selected.getMonth() !== now.getMonth() ||
    selected.getDate() !== now.getDate()
  ) {
    return []
  }
  const hours = []
  for (let h = 0; h < now.getHours(); h += 1) hours.push(h)
  return hours
}

function disabledMinutes(hour) {
  if (!form.scheduledStartTime) return []
  const selected = new Date(String(form.scheduledStartTime).replace(' ', 'T'))
  const now = new Date()
  if (
    selected.getFullYear() !== now.getFullYear() ||
    selected.getMonth() !== now.getMonth() ||
    selected.getDate() !== now.getDate()
  ) {
    return []
  }
  if (hour !== now.getHours()) return []
  const minutes = []
  for (let m = 0; m <= now.getMinutes(); m += 1) minutes.push(m)
  return minutes
}

function isSameDay(startStr, endStr) {
  if (!startStr || !endStr) return true
  return String(startStr).slice(0, 10) === String(endStr).slice(0, 10)
}

function isPast(startStr) {
  const start = new Date(String(startStr).replace(' ', 'T'))
  return Number.isNaN(start.getTime()) || start.getTime() <= Date.now()
}

async function loadBase() {
  if (!serviceId.value) {
    loadError.value = '服务项目不存在。'
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const [itemData, elderData] = await Promise.all([
      getCareServiceDetail(serviceId.value).then(unwrap),
      listMyElders().then(unwrap),
    ])
    item.value = itemData
    elders.value = Array.isArray(elderData) ? elderData : []
    if (elders.value.length === 1) {
      form.elderId = elders.value[0].id
    }
    if (item.value?.status && item.value.status !== 'ENABLED') {
      loadError.value = '该服务项目当前不可预约。'
    }
  } catch (e) {
    item.value = null
    elders.value = []
    if (e.code === 403) {
      loadError.value = '您没有权限执行此操作。'
    } else if (e.code === 404) {
      loadError.value = '服务项目不存在。'
    } else if (e.code === 500) {
      loadError.value = '系统服务异常，请稍后再试。'
    } else {
      loadError.value = e.message || '加载失败'
    }
  } finally {
    loading.value = false
  }
}

function goBackDetail() {
  router.push(`/family/care-services/${serviceId.value}`)
}

function goElders() {
  router.push('/family/elders')
}

async function nextStep() {
  if (step.value === 0) {
    if (!elders.value.length) {
      ElMessage.warning('暂无可预约服务的老人')
      return
    }
    if (!form.elderId) {
      ElMessage.warning('请选择老人')
      return
    }
    step.value = 1
    return
  }
  if (step.value === 1) {
    if (!form.scheduledStartTime) {
      ElMessage.warning('请选择预约时间')
      return
    }
    if (isPast(form.scheduledStartTime)) {
      ElMessage.warning('不能选择过去的时间')
      return
    }
    if (!scheduledEndTime.value) {
      ElMessage.warning('无法计算结束时间，请检查服务时长')
      return
    }
    if (!isSameDay(form.scheduledStartTime, scheduledEndTime.value)) {
      ElMessage.warning('单次预约暂不支持跨日，请重新选择开始时间')
      return
    }
    step.value = 2
  }
}

function prevStep() {
  if (step.value > 0) step.value -= 1
}

function mapSubmitError(e) {
  if (e.code === 403) {
    return '您没有权限为该老人预约服务。'
  }
  if (e.code === 404) {
    return '服务项目不存在。'
  }
  if (e.code === 409) {
    return e.message || '当前预约时间不可用，请重新选择。'
  }
  if (e.code === 400) {
    return e.message || '请检查预约信息。'
  }
  if (e.code === 500) {
    return '系统服务异常，请稍后再试。'
  }
  return e.message || '预约失败'
}

async function submit() {
  if (!item.value || submitting.value) return
  if (!form.elderId || !form.scheduledStartTime || !scheduledEndTime.value) {
    ElMessage.warning('请完善预约信息')
    return
  }
  if (isPast(form.scheduledStartTime)) {
    ElMessage.warning('不能选择过去的时间')
    return
  }
  if (!isSameDay(form.scheduledStartTime, scheduledEndTime.value)) {
    ElMessage.warning('单次预约暂不支持跨日，请重新选择开始时间')
    return
  }

  submitting.value = true
  try {
    const payload = {
      elderId: form.elderId,
      serviceItemId: item.value.id,
      scheduledStartTime: formatOrderDateTime(form.scheduledStartTime),
      scheduledEndTime: scheduledEndTime.value,
    }
    if (form.remark?.trim()) {
      payload.remark = form.remark.trim().slice(0, 500)
    }
    // 不传 careStaffId：CreateDTO 不含该字段，后端创建时强制 null

    const orderId = unwrap(await createServiceOrder(payload))
    ElMessage.success('预约成功')

    try {
      order.value = unwrap(await getServiceOrder(orderId))
    } catch {
      order.value = {
        id: orderId,
        status: 'PENDING',
        elderName: selectedElder.value?.name,
        serviceName: item.value.serviceName,
        scheduledStartTime: payload.scheduledStartTime,
        durationMinutes: item.value.durationMinutes,
        remark: payload.remark,
      }
    }
    success.value = true
    step.value = 3
  } catch (e) {
    if (e.code !== 401) {
      ElMessage.error(mapSubmitError(e))
    }
  } finally {
    submitting.value = false
  }
}

watch(serviceId, () => {
  success.value = false
  step.value = 0
  form.elderId = null
  form.scheduledStartTime = ''
  form.remark = ''
  order.value = null
  loadBase()
})

onMounted(() => {
  loadBase()
})
</script>

<template>
  <div class="book-page">
    <button type="button" class="back" @click="goBackDetail">
      <el-icon><ArrowLeft /></el-icon>
      返回服务详情
    </button>

    <header class="book-page__head">
      <h1>服务预约</h1>
      <p>为老人预约贴心的照护服务。</p>
    </header>

    <el-skeleton v-if="loading" class="panel" :rows="8" animated />

    <div v-else-if="loadError" class="panel">
      <el-result icon="warning" title="无法预约" :sub-title="loadError">
        <template #extra>
          <el-button type="primary" @click="goBackDetail">返回服务详情</el-button>
          <el-button v-if="!elders.length" @click="goElders">返回我的老人</el-button>
        </template>
      </el-result>
    </div>

    <BookingSuccess
      v-else-if="success"
      :elder-name="order?.elderName || selectedElder?.name"
      :service-name="order?.serviceName || item?.serviceName"
      :scheduled-start-time="order?.scheduledStartTime || form.scheduledStartTime"
      :duration-minutes="order?.durationMinutes || item?.durationMinutes"
      :price="order?.amount ?? item?.price"
      :remark="order?.remark || form.remark"
      :status="order?.status || 'PENDING'"
      :order-id="order?.id"
      :payment-status="order?.paymentStatus || 'UNPAID'"
    />

    <template v-else-if="item">
      <section class="panel">
        <el-steps :active="step" finish-status="success" align-center>
          <el-step title="选择老人" />
          <el-step title="预约时间" />
          <el-step title="确认提交" />
          <el-step title="预约成功" />
        </el-steps>
      </section>

      <section class="panel">
        <div class="service-brief">
          <strong>{{ item.serviceName }}</strong>
          <span>预计时长 {{ durationMinutes }} 分钟 · 不支持跨日预约</span>
        </div>

        <!-- 步骤1：选择绑定老人（无护理员选择） -->
        <div v-show="step === 0">
          <h3 class="block-title">① 选择老人</h3>
          <ElderSelectCard v-model="form.elderId" :elders="elders" />
          <div v-if="!elders.length" class="actions">
            <el-button type="primary" @click="goElders">返回我的老人</el-button>
          </div>
          <div v-else class="actions">
            <el-button type="primary" @click="nextStep">下一步</el-button>
          </div>
        </div>

        <!-- 步骤2：预约时间（结束时间由项目时长自动计算） -->
        <div v-show="step === 1">
          <h3 class="block-title">② 选择预约时间</h3>
          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-form-item label="预约开始时间" prop="scheduledStartTime" required>
              <el-date-picker
                v-model="form.scheduledStartTime"
                type="datetime"
                placeholder="选择日期时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                format="YYYY-MM-DD HH:mm"
                :disabled-date="disabledDate"
                :disabled-hours="disabledHours"
                :disabled-minutes="disabledMinutes"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="预计结束时间（自动计算）">
              <el-input :model-value="scheduledEndTime || '-'" disabled />
            </el-form-item>
            <el-form-item label="预计服务时长">
              <el-input :model-value="`${durationMinutes} 分钟`" disabled />
            </el-form-item>
          </el-form>
          <div class="actions">
            <el-button @click="prevStep">上一步</el-button>
            <el-button type="primary" @click="nextStep">下一步</el-button>
          </div>
        </div>

        <!-- 步骤3：确认信息并提交（不含护理员） -->
        <div v-show="step === 2">
          <h3 class="block-title">③ 确认服务信息</h3>
          <BookingSummary
            :elder-name="selectedElder?.name"
            :service-name="item.serviceName"
            :scheduled-start-time="form.scheduledStartTime"
            :duration-minutes="item.durationMinutes"
            :price="item.price"
            :remark="form.remark"
          />
          <el-form class="remark-form" label-position="top">
            <el-form-item label="备注（选填）">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="3"
                maxlength="500"
                show-word-limit
                placeholder="请输入特殊照护需求"
              />
            </el-form-item>
          </el-form>
          <p class="hint">提交后订单状态为「待确认」，护理员将由管理员分配，家属无需选择护理员。</p>
          <div class="actions">
            <el-button @click="prevStep" :disabled="submitting">上一步</el-button>
            <el-button type="primary" :loading="submitting" @click="submit">
              {{ submitting ? '提交中...' : '提交预约' }}
            </el-button>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.book-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.back {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #4a90c2;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.book-page__head h1 {
  margin: 0;
  font-size: 22px;
  color: #2c4a5e;
}

.book-page__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.panel {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 20px;
}

.service-brief {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 18px;
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(234, 244, 251, 0.7), rgba(238, 247, 243, 0.65));
}

.service-brief strong {
  font-size: 16px;
  color: #2c4a5e;
}

.service-brief span {
  font-size: 12px;
  color: #718096;
}

.block-title {
  margin: 0 0 14px;
  font-size: 15px;
  color: #2c4a5e;
}

.actions {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.remark-form {
  margin-top: 16px;
}

.hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #8aa0b5;
  line-height: 1.5;
}
</style>
