<script setup>
import { computed, ref } from 'vue'
import {
  formatDateTime,
  indicatorLabel,
  directionLabel,
  warningLevelMeta,
  warningStatusMeta,
} from '@/utils/familyHome'
import { getHealthAlertDetail } from '@/api/health'
import { unwrap } from '@/utils/familyHome'
import { ElMessage } from 'element-plus'

const props = defineProps({
  loading: Boolean,
  warnings: { type: Array, default: () => [] },
  elderName: { type: String, default: '' },
})

const statusFilter = defineModel('statusFilter', { type: String, default: 'ALL' })

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const filtered = computed(() => {
  const list = Array.isArray(props.warnings) ? props.warnings : []
  if (statusFilter.value === 'ALL') return list
  return list.filter((w) => w.status === statusFilter.value)
})

function alertTitle(item) {
  const name = indicatorLabel(item.indicator)
  const dir = directionLabel(item.direction)
  return dir ? `${name}异常（${dir}）` : `${name}异常`
}

function alertMessage(item) {
  const parts = [
    indicatorLabel(item.indicator),
    item.actualValue != null ? `实测 ${item.actualValue}` : '',
    item.thresholdDesc || '',
  ].filter(Boolean)
  return parts.join('，') + '。请及时关注老人健康状况。'
}

async function openDetail(item) {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = item
  try {
    const data = unwrap(await getHealthAlertDetail(item.id))
    detail.value = data
  } catch (e) {
    if (e.code === 403) {
      ElMessage.warning('您没有权限查看该老人的健康数据。')
    } else if (e.code !== 401) {
      ElMessage.error(e.message || '加载预警详情失败')
    }
  } finally {
    detailLoading.value = false
  }
}
</script>

<template>
  <section class="alerts">
    <div class="alerts__head">
      <div>
        <h3>健康预警</h3>
        <p>及时关注老人异常健康指标</p>
      </div>
      <el-radio-group v-model="statusFilter" size="small">
        <el-radio-button value="ALL">全部</el-radio-button>
        <el-radio-button value="UNHANDLED">待处理</el-radio-button>
        <el-radio-button value="HANDLED">已处理</el-radio-button>
      </el-radio-group>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />
    <div v-else-if="!filtered.length" class="alerts-empty" :class="{ 'is-ok': !warnings.length }">
      <template v-if="!warnings.length">
        <div class="alerts-empty__emoji">🎉</div>
        <p class="empty-title">暂无健康异常预警</p>
        <p class="empty-desc">老人目前健康状况良好。</p>
      </template>
      <template v-else>
        <p class="empty-title">该筛选条件下暂无预警</p>
        <p class="empty-desc">可切换查看全部、待处理或已处理记录。</p>
      </template>
    </div>
    <div v-else class="alerts__list">
      <article
        v-for="item in filtered"
        :key="item.id"
        class="alert-card"
        :class="`alert-card--${warningLevelMeta(item.warningLevel).tone}`"
      >
        <div class="alert-card__top">
          <div>
            <h4>⚠️ {{ alertTitle(item) }}</h4>
            <span class="level-tag" :class="`is-${warningLevelMeta(item.warningLevel).tone}`">
              {{ warningLevelMeta(item.warningLevel).label }}
            </span>
          </div>
          <el-tag
            size="small"
            :type="warningStatusMeta(item.status).type"
            effect="light"
          >
            {{ warningStatusMeta(item.status).label }}
          </el-tag>
        </div>
        <p class="alert-card__time">{{ formatDateTime(item.generatedAt) }}</p>
        <p class="alert-card__msg">{{ alertMessage(item) }}</p>
        <div class="alert-card__foot">
          <span>状态：{{ warningStatusMeta(item.status).label }}</span>
          <el-button link type="primary" @click="openDetail(item)">查看详情</el-button>
        </div>
      </article>
    </div>

    <el-drawer v-model="drawerVisible" title="预警详情" size="420px" destroy-on-close>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <template v-else-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="老人">
            {{ detail.elderName || elderName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="预警类型">
            {{ alertTitle(detail) }}
          </el-descriptions-item>
          <el-descriptions-item label="预警等级">
            {{ warningLevelMeta(detail.warningLevel).label }}
          </el-descriptions-item>
          <el-descriptions-item label="实测值">
            {{ detail.actualValue ?? '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="阈值说明">
            {{ detail.thresholdDesc || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="预警内容">
            {{ alertMessage(detail) }}
          </el-descriptions-item>
          <el-descriptions-item label="触发时间">
            {{ formatDateTime(detail.generatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理状态">
            {{ warningStatusMeta(detail.status).label }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">
            {{ formatDateTime(detail.handledAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理人">
            {{ detail.handledByName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="处理结果">
            {{ detail.handlingResult || '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <p class="drawer-note">家属端仅支持查看预警，处理操作由照护人员或管理员完成。</p>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.alerts {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 18px 20px;
}

.alerts__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.alerts__head h3 {
  margin: 0;
  font-size: 16px;
  color: #2c4a5e;
}

.alerts__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.alerts__list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.alert-card {
  border-radius: 14px;
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
}

.alert-card--low {
  background: linear-gradient(160deg, rgba(255, 249, 230, 0.9), rgba(255, 252, 245, 0.95));
}
.alert-card--mid {
  background: linear-gradient(160deg, rgba(255, 243, 230, 0.92), rgba(255, 250, 245, 0.95));
}
.alert-card--high {
  background: linear-gradient(160deg, rgba(255, 236, 236, 0.9), rgba(255, 248, 248, 0.95));
}

.alert-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.alert-card__top h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #2c4a5e;
}

.level-tag {
  display: inline-flex;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.level-tag.is-low {
  background: rgba(230, 195, 92, 0.2);
  color: #b8922c;
}
.level-tag.is-mid {
  background: rgba(242, 166, 90, 0.2);
  color: #c8782a;
}
.level-tag.is-high {
  background: rgba(245, 108, 108, 0.16);
  color: #d45454;
}

.alert-card__time {
  margin: 8px 0 0;
  font-size: 12px;
  color: #8aa0b5;
}

.alert-card__msg {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.55;
  color: #4a6072;
}

.alert-card__foot {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #718096;
}

.alerts-empty {
  min-height: 160px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: rgba(234, 244, 251, 0.45);
  padding: 24px;
}

.alerts-empty.is-ok {
  background: linear-gradient(160deg, rgba(232, 246, 239, 0.85), rgba(245, 252, 248, 0.95));
}

.alerts-empty__emoji {
  font-size: 28px;
  margin-bottom: 8px;
}

.empty-title {
  margin: 0;
  color: #4a6072;
  font-size: 14px;
  font-weight: 600;
}

.empty-desc {
  margin: 6px 0 0;
  color: #8aa0b5;
  font-size: 12px;
}

.drawer-note {
  margin: 16px 0 0;
  font-size: 12px;
  color: #8aa0b5;
  line-height: 1.5;
}

@media (max-width: 900px) {
  .alerts__list {
    grid-template-columns: 1fr;
  }
}
</style>
