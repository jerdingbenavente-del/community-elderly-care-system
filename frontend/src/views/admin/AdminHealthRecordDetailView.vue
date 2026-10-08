<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Monitor } from '@element-plus/icons-vue'
import { getHealthRecord } from '@/api/health'
import {
  directionLabel,
  formatDateTime,
  indicatorLabel,
  warningLevelMeta,
  warningStatusMeta,
} from '@/utils/familyHome'
import HealthRecordFormDrawer from '@/components/admin/HealthRecordFormDrawer.vue'

const route = useRoute()
const router = useRouter()

const recordId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const loading = ref(false)
const loadError = ref('')
const notFound = ref(false)
const record = ref(null)
const formVisible = ref(false)

function bpText(row) {
  if (!row) return '-'
  if (row.systolicPressure == null && row.diastolicPressure == null) return '-'
  const s = row.systolicPressure ?? '-'
  const d = row.diastolicPressure ?? '-'
  return `${s} / ${d} mmHg`
}

function alertTitle(item) {
  if (!item) return '-'
  const name = indicatorLabel(item.indicator)
  const dir = directionLabel(item.direction)
  return dir ? `${name}异常（${dir}）` : `${name}异常`
}

function levelTagType(level) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'WARNING') return 'warning'
  return 'info'
}

async function loadDetail() {
  if (!recordId.value) {
    loadError.value = '无效的记录 ID'
    record.value = null
    notFound.value = true
    return
  }
  loading.value = true
  loadError.value = ''
  notFound.value = false
  try {
    const res = await getHealthRecord(recordId.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    record.value = res.data
  } catch (e) {
    record.value = null
    const msg = e.message || '加载失败'
    if (e?.code === 404 || msg.includes('不存在')) {
      notFound.value = true
      loadError.value = '健康记录不存在或已删除'
    } else {
      loadError.value = msg
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/admin/health')
}

function goWarning(row) {
  router.push(`/admin/warnings/${row.id}`)
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
          <div class="avatar"><el-icon :size="28"><Monitor /></el-icon></div>
          <div>
            <h2>{{ record?.elderName || '健康记录详情' }}</h2>
            <p v-if="record">测量时间 {{ formatDateTime(record.measuredAt) }}</p>
          </div>
        </div>
      </div>
      <div class="page-head__right">
        <el-button v-if="record" type="primary" @click="formVisible = true">编辑</el-button>
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

    <template v-if="record">
      <div class="panel">
        <h3 class="panel__title">测量信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>记录 ID</span>{{ record.id }}</div>
          <div class="info-item"><span>老人</span><strong>{{ record.elderName || '-' }}</strong></div>
          <div class="info-item"><span>老人 ID</span>{{ record.elderId || '-' }}</div>
          <div class="info-item"><span>测量时间</span>{{ formatDateTime(record.measuredAt) }}</div>
          <div class="info-item"><span>录入人</span>{{ record.recordedBy || '-' }}</div>
          <div class="info-item"><span>创建时间</span>{{ formatDateTime(record.createdAt) }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">健康指标</h3>
        <div class="info-grid">
          <div class="info-item"><span>血压</span>{{ bpText(record) }}</div>
          <div class="info-item"><span>血糖</span>{{ record.bloodGlucose != null ? `${record.bloodGlucose} mmol/L` : '-' }}</div>
          <div class="info-item"><span>体温</span>{{ record.bodyTemperature != null ? `${record.bodyTemperature} ℃` : '-' }}</div>
          <div class="info-item"><span>心率</span>{{ record.heartRate != null ? `${record.heartRate} 次/分` : '-' }}</div>
          <div class="info-item wide"><span>备注</span>{{ record.remark?.trim() || '（无）' }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">关联预警</h3>
        <el-table
          v-if="record.warnings?.length"
          :data="record.warnings"
          stripe
        >
          <el-table-column label="类型" min-width="160">
            <template #default="{ row }">{{ alertTitle(row) }}</template>
          </el-table-column>
          <el-table-column label="等级" width="110">
            <template #default="{ row }">
              <el-tag :type="levelTagType(row.warningLevel)" size="small">
                {{ warningLevelMeta(row.warningLevel).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="实测" width="90">
            <template #default="{ row }">{{ row.actualValue ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="warningStatusMeta(row.status).type" size="small">
                {{ warningStatusMeta(row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goWarning(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <p v-else class="empty-hint">本次测量未产生预警。</p>
      </div>
    </template>

    <HealthRecordFormDrawer
      v-model="formVisible"
      :record-id="recordId"
      @success="loadDetail"
    />
  </div>
</template>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 960px;
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

.page-head__right {
  display: flex;
  gap: 10px;
  align-items: center;
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
  background: rgba(91, 184, 176, 0.18);
  color: #357a68;
}

.page-head h2 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--ec-text);
}

.page-head p {
  margin: 0;
  color: var(--ec-text-secondary);
  font-size: 13px;
}

.err {
  border-radius: 12px;
}

.panel {
  padding: 16px 18px;
  border-radius: var(--ec-radius-sm);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--ec-shadow);
}

.panel__title {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--ec-text);
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
  white-space: pre-wrap;
}

.empty-hint {
  margin: 0;
  font-size: 13px;
  color: var(--ec-text-secondary);
}

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .page-head {
    flex-direction: column;
  }
}
</style>
