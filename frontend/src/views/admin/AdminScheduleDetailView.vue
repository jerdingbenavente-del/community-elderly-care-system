<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Calendar } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getCareStaffSchedule,
  updateCareStaffSchedule,
} from '@/api/careStaffSchedule'
import { getCareStaff } from '@/api/careStaff'
import ScheduleFormDrawer from '@/components/admin/ScheduleFormDrawer.vue'

const route = useRoute()
const router = useRouter()

const scheduleId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const loading = ref(false)
const loadError = ref('')
const notFound = ref(false)
const schedule = ref(null)
const staffExtra = ref(null)
const cancelLoading = ref(false)
const editVisible = ref(false)

const canCancel = computed(() => schedule.value?.status === 'AVAILABLE')

function statusText(status) {
  if (status === 'AVAILABLE') return '可用'
  if (status === 'CANCELLED') return '已取消'
  return status || '-'
}

async function loadDetail() {
  if (!scheduleId.value) {
    loadError.value = '无效的排班 ID'
    schedule.value = null
    notFound.value = true
    return
  }
  loading.value = true
  loadError.value = ''
  notFound.value = false
  staffExtra.value = null
  try {
    const res = await getCareStaffSchedule(scheduleId.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    schedule.value = res.data
    if (schedule.value?.careStaffId) {
      loadStaff(schedule.value.careStaffId)
    }
  } catch (e) {
    schedule.value = null
    const msg = e.message || '加载失败'
    if (e?.code === 404 || msg.includes('不存在')) {
      notFound.value = true
      loadError.value = '排班不存在或已删除'
    } else {
      loadError.value = msg
    }
  } finally {
    loading.value = false
  }
}

async function loadStaff(id) {
  try {
    const res = await getCareStaff(id)
    if (res?.code === 200) staffExtra.value = res.data
  } catch {
    staffExtra.value = null
  }
}

function goBack() {
  router.push('/admin/schedules')
}

function openEdit() {
  editVisible.value = true
}

async function onCancel() {
  if (!schedule.value) return
  try {
    await ElMessageBox.confirm(
      '取消后该时段将无法用于订单确认分配，是否确认取消？',
      '取消排班',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '返回' },
    )
  } catch {
    return
  }
  cancelLoading.value = true
  try {
    const res = await updateCareStaffSchedule(schedule.value.id, {
      scheduleDate: schedule.value.scheduleDate,
      startTime: schedule.value.startTime,
      endTime: schedule.value.endTime,
      status: 'CANCELLED',
      remark: schedule.value.remark || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '取消失败')
    ElMessage.success('排班已取消')
    await loadDetail()
  } catch (e) {
    if (!e?.toastShown) ElMessage.error(e.message || '取消失败')
  } finally {
    cancelLoading.value = false
  }
}

watch(
  () => route.params.id,
  () => loadDetail(),
)

onMounted(loadDetail)
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <div class="page-head">
      <div class="page-head__left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回列表</el-button>
        <div class="title-row">
          <div class="avatar"><el-icon :size="28"><Calendar /></el-icon></div>
          <div>
            <h2>{{ schedule?.careStaffName || '排班详情' }}</h2>
            <p v-if="schedule">
              {{ schedule.scheduleDate }} · {{ schedule.startTime }} ~ {{ schedule.endTime }} ·
              <el-tag :type="schedule.status === 'AVAILABLE' ? 'success' : 'info'" size="small">
                {{ statusText(schedule.status) }}
              </el-tag>
            </p>
          </div>
        </div>
      </div>
      <div v-if="schedule" class="page-head__actions">
        <el-button
          v-if="canCancel"
          type="warning"
          :loading="cancelLoading"
          @click="onCancel"
        >
          取消排班
        </el-button>
        <el-button type="primary" @click="openEdit">编辑</el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      :type="notFound ? 'warning' : 'error'"
      :closable="false"
      show-icon
      :title="loadError"
      class="err"
    >
      <template #default>
        <el-button v-if="notFound" type="primary" link @click="goBack">返回列表</el-button>
        <el-button v-else type="primary" link @click="loadDetail">重新加载</el-button>
      </template>
    </el-alert>

    <div v-if="schedule" class="panel">
      <div class="info-grid">
        <div class="info-item">
          <span>护理员</span>
          <strong>{{ schedule.careStaffName || staffExtra?.name || '-' }}</strong>
        </div>
        <div class="info-item"><span>工号</span>{{ staffExtra?.employeeNo || '-' }}</div>
        <div class="info-item"><span>排班日期</span>{{ schedule.scheduleDate || '-' }}</div>
        <div class="info-item">
          <span>时段</span>{{ schedule.startTime || '-' }} ~ {{ schedule.endTime || '-' }}
        </div>
        <div class="info-item">
          <span>状态</span>
          <el-tag :type="schedule.status === 'AVAILABLE' ? 'success' : 'info'" size="small">
            {{ statusText(schedule.status) }}
          </el-tag>
        </div>
        <div class="info-item"><span>护理员 ID</span>{{ schedule.careStaffId || '-' }}</div>
        <div class="info-item"><span>创建时间</span>{{ schedule.createdAt || '-' }}</div>
        <div class="info-item"><span>更新时间</span>{{ schedule.updatedAt || '-' }}</div>
        <div class="info-item wide"><span>备注</span>{{ schedule.remark || '-' }}</div>
      </div>
      <p class="note">
        订单确认分配护理员时，后端会校验预约时段是否被「可用」排班覆盖，以及是否与已确认/服务中订单冲突。
      </p>
    </div>

    <ScheduleFormDrawer
      v-if="scheduleId"
      v-model="editVisible"
      :schedule-id="scheduleId"
      @success="loadDetail"
    />
  </div>
</template>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.page-head__left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-head__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(74, 144, 194, 0.16);
  color: #3a7a9c;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--ec-text);
}

.page-head p {
  margin: 0;
  color: var(--ec-text-secondary);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.err {
  border-radius: 12px;
}

.panel {
  padding: 18px;
  border-radius: var(--ec-radius-sm);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--ec-shadow);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  color: var(--ec-text);
}

.info-item span {
  font-size: 12px;
  color: var(--ec-text-secondary);
}

.info-item.wide {
  grid-column: 1 / -1;
}

.note {
  margin: 16px 0 0;
  font-size: 12px;
  color: var(--ec-text-secondary);
  line-height: 1.6;
}

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
