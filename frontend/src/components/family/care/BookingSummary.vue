<script setup>
import { formatPrice, formatDateTime, orderStatusMeta } from '@/utils/familyHome'

defineProps({
  elderName: { type: String, default: '-' },
  serviceName: { type: String, default: '-' },
  scheduledStartTime: { type: String, default: '' },
  durationMinutes: { type: [Number, String], default: '-' },
  price: { type: [Number, String], default: null },
  remark: { type: String, default: '' },
  status: { type: String, default: '' },
})
</script>

<template>
  <dl class="summary">
    <div class="summary__item">
      <dt>老人</dt>
      <dd>{{ elderName || '-' }}</dd>
    </div>
    <div class="summary__item">
      <dt>服务项目</dt>
      <dd>{{ serviceName || '-' }}</dd>
    </div>
    <div class="summary__item">
      <dt>预约时间</dt>
      <dd>{{ formatDateTime(scheduledStartTime) }}</dd>
    </div>
    <div class="summary__item">
      <dt>预计时长</dt>
      <dd>{{ durationMinutes ?? '-' }} 分钟</dd>
    </div>
    <div class="summary__item">
      <dt>价格</dt>
      <dd class="price">{{ formatPrice(price) }}</dd>
    </div>
    <div v-if="remark" class="summary__item summary__item--full">
      <dt>备注</dt>
      <dd>{{ remark }}</dd>
    </div>
    <div v-if="status" class="summary__item">
      <dt>订单状态</dt>
      <dd>
        <el-tag size="small" :type="orderStatusMeta(status).type" effect="light">
          {{ orderStatusMeta(status).label }}
        </el-tag>
      </dd>
    </div>
  </dl>
</template>

<style scoped>
.summary {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.summary__item {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(234, 244, 251, 0.5);
}

.summary__item--full {
  grid-column: 1 / -1;
}

.summary__item dt {
  font-size: 12px;
  color: #8aa0b5;
  margin-bottom: 4px;
}

.summary__item dd {
  margin: 0;
  font-size: 14px;
  color: #2c4a5e;
  word-break: break-word;
}

.summary__item .price {
  color: #c8782a;
  font-weight: 700;
}

@media (max-width: 720px) {
  .summary {
    grid-template-columns: 1fr;
  }
}
</style>
