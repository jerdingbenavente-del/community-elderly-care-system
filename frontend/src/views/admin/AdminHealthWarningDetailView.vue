<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Warning } from '@element-plus/icons-vue'
import { getHealthWarning } from '@/api/healthWarning'
import {
  directionLabel,
  formatDateTime,
  indicatorLabel,
  warningLevelMeta,
  warningStatusMeta,
} from '@/utils/familyHome'
import HandleWarningDrawer from '@/components/admin/HandleWarningDrawer.vue'

const route = useRoute()
const router = useRouter()

const warningId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const loading = ref(false)
const loadError = ref('')
const notFound = ref(false)
const warning = ref(null)
const handleVisible = ref(false)

const canHandle = computed(() => warning.value?.status === 'UNHANDLED')

function alertTitle(item) {
  if (!item) return '-'
  const name = indicatorLabel(item.indicator)
  const dir = directionLabel(item.direction)
  return dir ? `${name}异常（${dir}）` : `${name}异常`
}

function alertMessage(item) {
  if (!item) return '-'
  const parts = [
    indicatorLabel(item.indicator),
    item.actualValue != null ? `实测 ${item.actualValue}` : '',
    item.thresholdDesc || '',
  ].filter(Boolean)
  return `${parts.join('，')}。请及时关注老人健康状况。`
}

function levelTagType(level) {
  if (level === 'CRITICAL') return 'danger'
  if (level === 'WARNING') return 'warning'
  return 'info'
}

async function loadDetail() {
  if (!warningId.value) {
    loadError.value = '无效的预警 ID'
    warning.value = null
    notFound.value = true
    return
  }
  loading.value = true
  loadError.value = ''
  notFound.value = false
  try {
    const res = await getHealthWarning(warningId.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    warning.value = res.data
  } catch (e) {
    warning.value = null
    const msg = e.message || '加载失败'
    if (e?.code === 404 || msg.includes('不存在')) {
      notFound.value = true
      loadError.value = '预警不存在或已删除'
    } else {
      loadError.value = msg
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/admin/warnings')
}

function openHandle() {
  handleVisible.value = true
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
          <div class="avatar"><el-icon :size="28"><Warning /></el-icon></div>
          <div>
            <h2>{{ warning ? alertTitle(warning) : '预警详情' }}</h2>
            <p v-if="warning">
              {{ warning.elderName || '-' }} ·
              <el-tag :type="warningStatusMeta(warning.status).type" size="small">
                {{ warningStatusMeta(warning.status).label }}
              </el-tag>
              <el-tag :type="levelTagType(warning.warningLevel)" size="small">
                {{ warningLevelMeta(warning.warningLevel).label }}
              </el-tag>
            </p>
          </div>
        </div>
      </div>
      <el-button v-if="canHandle" type="warning" @click="openHandle">处理预警</el-button>
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

    <template v-if="warning">
      <div class="panel">
        <h3 class="panel__title">预警信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>预警编号</span>{{ warning.id }}</div>
          <div class="info-item"><span>预警类型</span>{{ alertTitle(warning) }}</div>
          <div class="info-item">
            <span>预警等级</span>
            <el-tag :type="levelTagType(warning.warningLevel)" size="small">
              {{ warningLevelMeta(warning.warningLevel).label }}
            </el-tag>
          </div>
          <div class="info-item">
            <span>状态</span>
            <el-tag :type="warningStatusMeta(warning.status).type" size="small">
              {{ warningStatusMeta(warning.status).label }}
            </el-tag>
          </div>
          <div class="info-item"><span>预警时间</span>{{ formatDateTime(warning.generatedAt) }}</div>
          <div class="info-item wide"><span>预警内容</span>{{ alertMessage(warning) }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">老人信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>老人姓名</span><strong>{{ warning.elderName || '-' }}</strong></div>
          <div class="info-item"><span>老人 ID</span>{{ warning.elderId || '-' }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">异常健康数据</h3>
        <div class="info-grid">
          <div class="info-item"><span>指标</span>{{ indicatorLabel(warning.indicator) }}</div>
          <div class="info-item"><span>方向</span>{{ directionLabel(warning.direction) || '-' }}</div>
          <div class="info-item"><span>实测值</span><strong>{{ warning.actualValue ?? '-' }}</strong></div>
          <div class="info-item"><span>健康记录 ID</span>{{ warning.healthRecordId || '-' }}</div>
          <div class="info-item wide"><span>阈值说明</span>{{ warning.thresholdDesc || '-' }}</div>
        </div>
        <p class="note">本系统仅作健康管理辅助提醒，不构成医疗诊断。</p>
      </div>

      <div class="panel">
        <h3 class="panel__title">处理信息</h3>
        <template v-if="warning.status === 'HANDLED'">
          <div class="info-grid">
            <div class="info-item"><span>处理人</span>{{ warning.handledByName || '-' }}</div>
            <div class="info-item"><span>处理时间</span>{{ formatDateTime(warning.handledAt) }}</div>
            <div class="info-item wide"><span>处理备注</span>{{ warning.handlingResult || '-' }}</div>
          </div>
        </template>
        <p v-else class="muted">尚未处理。可点击「处理预警」填写处理备注。</p>
      </div>
    </template>

    <HandleWarningDrawer
      v-if="warning"
      v-model="handleVisible"
      :warning="warning"
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
  background: rgba(242, 166, 90, 0.18);
  color: #c47a2e;
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
}

.note,
.muted {
  margin: 12px 0 0;
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
