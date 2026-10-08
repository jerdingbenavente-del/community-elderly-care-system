<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Star } from '@element-plus/icons-vue'
import { getAdminEvaluation } from '@/api/careEvaluation'
import { formatDateTime } from '@/utils/familyHome'

const route = useRoute()
const router = useRouter()

const evaluationId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const loading = ref(false)
const loadError = ref('')
const notFound = ref(false)
const evaluation = ref(null)

async function loadDetail() {
  if (!evaluationId.value) {
    loadError.value = '无效的评价 ID'
    evaluation.value = null
    notFound.value = true
    return
  }
  loading.value = true
  loadError.value = ''
  notFound.value = false
  try {
    const res = await getAdminEvaluation(evaluationId.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    evaluation.value = res.data
  } catch (e) {
    evaluation.value = null
    const msg = e.message || '加载失败'
    if (e?.code === 404 || msg.includes('不存在')) {
      notFound.value = true
      loadError.value = '评价不存在或已删除'
    } else {
      loadError.value = msg
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/admin/evaluations')
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
          <div class="avatar"><el-icon :size="28"><Star /></el-icon></div>
          <div>
            <h2>评价详情 #{{ evaluation?.id || '' }}</h2>
            <p v-if="evaluation">
              {{ evaluation.elderName || '-' }} · {{ evaluation.serviceName || '-' }}
            </p>
          </div>
        </div>
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

    <template v-if="evaluation">
      <div class="panel score-panel">
        <h3 class="panel__title">评分信息</h3>
        <div class="score-row">
          <el-rate :model-value="Number(evaluation.score) || 0" disabled :max="5" size="large" />
          <strong>{{ evaluation.score ?? '-' }} / 5</strong>
        </div>
        <p class="note">本系统评价为单一综合评分（1–5），无单独护理员评分字段。</p>
      </div>

      <div class="panel">
        <h3 class="panel__title">评价信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>评价编号</span>{{ evaluation.id }}</div>
          <div class="info-item"><span>评价时间</span>{{ formatDateTime(evaluation.createdAt) }}</div>
          <div class="info-item wide">
            <span>评价内容</span>
            {{ evaluation.content?.trim() || '（未填写评价内容）' }}
          </div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">订单信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>订单编号</span><code>{{ evaluation.orderNo || '-' }}</code></div>
          <div class="info-item"><span>订单 ID</span>{{ evaluation.serviceOrderId || '-' }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">老人信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>老人姓名</span><strong>{{ evaluation.elderName || '-' }}</strong></div>
          <div class="info-item"><span>老人 ID</span>{{ evaluation.elderId || '-' }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">服务信息</h3>
        <div class="info-grid">
          <div class="info-item"><span>服务项目</span><strong>{{ evaluation.serviceName || '-' }}</strong></div>
          <div class="info-item"><span>服务项目 ID</span>{{ evaluation.serviceItemId || '-' }}</div>
        </div>
      </div>

      <div class="panel">
        <h3 class="panel__title">护理员信息</h3>
        <div v-if="evaluation.careStaffId" class="info-grid">
          <div class="info-item">
            <span>护理员</span>{{ evaluation.careStaffName || '-' }}
          </div>
          <div class="info-item"><span>护理员 ID</span>{{ evaluation.careStaffId }}</div>
        </div>
        <p v-else class="muted">未分配护理员</p>
      </div>
    </template>
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
  background: rgba(242, 166, 90, 0.16);
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

.score-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-row strong {
  font-size: 20px;
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
