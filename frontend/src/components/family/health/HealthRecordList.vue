<script setup>
import { computed } from 'vue'
import { formatDateTime } from '@/utils/familyHome'

const props = defineProps({
  loading: Boolean,
  records: { type: Array, default: () => [] },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  dateRange: { type: Array, default: null },
})

const emit = defineEmits(['update:page', 'update:pageSize', 'update:dateRange', 'search', 'reset'])

const total = computed(() => props.records.length)

const paged = computed(() => {
  const start = (props.page - 1) * props.pageSize
  return props.records.slice(start, start + props.pageSize)
})

function onPageChange(p) {
  emit('update:page', p)
}

function onSizeChange(s) {
  emit('update:pageSize', s)
  emit('update:page', 1)
}
</script>

<template>
  <section class="records">
    <div class="records__head">
      <div>
        <h3>健康记录</h3>
        <p>按时间查看真实测量记录</p>
      </div>
      <div class="records__filters">
        <el-date-picker
          :model-value="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          :clearable="true"
          @update:model-value="emit('update:dateRange', $event)"
        />
        <el-button type="primary" @click="emit('search')">查询</el-button>
        <el-button @click="emit('reset')">重置</el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />
    <el-empty
      v-else-if="!records.length"
      description="暂无健康数据"
      :image-size="72"
    >
      <template #description>
        <p class="empty-title">暂无健康数据</p>
        <p class="empty-desc">暂时还没有该老人的健康测量记录。</p>
      </template>
    </el-empty>
    <template v-else>
      <div class="records__table-wrap">
        <el-table :data="paged" class="records-table" stripe>
          <el-table-column label="测量时间" min-width="168">
            <template #default="{ row }">{{ formatDateTime(row.measuredAt) }}</template>
          </el-table-column>
          <el-table-column label="体温(℃)" width="100">
            <template #default="{ row }">{{ row.bodyTemperature ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="血压(mmHg)" min-width="120">
            <template #default="{ row }">
              {{ row.systolicPressure ?? '-' }}/{{ row.diastolicPressure ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="心率" width="90">
            <template #default="{ row }">{{ row.heartRate ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="血糖" width="100">
            <template #default="{ row }">{{ row.bloodGlucose ?? '-' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        </el-table>
      </div>
      <div class="records__pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :current-page="page"
          :page-size="pageSize"
          :page-sizes="[5, 10, 20]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
      <p class="records__note">
        家属端接口按时间筛选后返回列表（最多 100 条），分页在前端完成。
      </p>
    </template>
  </section>
</template>

<style scoped>
.records {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 18px 20px;
}

.records__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.records__head h3 {
  margin: 0;
  font-size: 16px;
  color: #2c4a5e;
}

.records__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.records__filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.records__table-wrap {
  border-radius: 14px;
  overflow: hidden;
  background: rgba(248, 252, 255, 0.65);
}

.records-table {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(234, 244, 251, 0.75);
  --el-table-row-hover-bg-color: rgba(238, 247, 243, 0.55);
  --el-table-text-color: #2c4a5e;
  --el-table-header-text-color: #4a6072;
}

.records__pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.records__note {
  margin: 10px 0 0;
  font-size: 12px;
  color: #8aa0b5;
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

@media (max-width: 720px) {
  .records__filters {
    width: 100%;
  }
}
</style>
