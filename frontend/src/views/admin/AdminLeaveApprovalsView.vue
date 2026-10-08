<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { approveLeave, pageAdminLeaves, rejectLeave } from '@/api/careStaffLeave'
import { toastIfNeeded } from '@/api/request'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const total = ref(0)
const reviewVisible = ref(false)
const reviewing = ref(false)
const reviewAction = ref('approve')
const current = ref(null)

const query = reactive({
  status: 'PENDING',
  page: 1,
  size: 10,
})

const reviewForm = reactive({
  reviewRemark: '',
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待审批', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
]

function statusMeta(s) {
  if (s === 'PENDING') return { label: '待审批', type: 'warning' }
  if (s === 'APPROVED') return { label: '已通过', type: 'success' }
  if (s === 'REJECTED') return { label: '已驳回', type: 'info' }
  return { label: s || '-', type: 'info' }
}

function fmt(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function loadList() {
  loading.value = true
  try {
    const res = await pageAdminLeaves({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    toastIfNeeded(e, '加载请假审批列表失败')
  } finally {
    loading.value = false
  }
}

function openReview(row, action) {
  current.value = row
  reviewAction.value = action
  reviewForm.reviewRemark = ''
  reviewVisible.value = true
}

async function submitReview() {
  if (!current.value) return
  reviewing.value = true
  try {
    const fn = reviewAction.value === 'approve' ? approveLeave : rejectLeave
    const res = await fn(current.value.id, { reviewRemark: reviewForm.reviewRemark || undefined })
    if (res?.code !== 200) throw new Error(res?.message || '审批失败')
    ElMessage.success(reviewAction.value === 'approve' ? '已通过，受影响未来任务已退回待重新安排' : '已驳回')
    reviewVisible.value = false
    await loadList()
    if (reviewAction.value === 'approve') {
      try {
        await ElMessageBox.confirm(
          '可通过服务订单列表筛选「待确认」状态，为已解除护理员的订单重新分配。是否前往？',
          '重新安排提示',
          { type: 'info', confirmButtonText: '去订单列表', cancelButtonText: '稍后' },
        )
        router.push({ path: '/admin/orders', query: { status: 'PENDING' } })
      } catch {
        /* cancel */
      }
    }
  } catch (e) {
    toastIfNeeded(e, '审批失败')
  } finally {
    reviewing.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="admin-leave-page">
    <header class="page-head">
      <div>
        <h1>护理员请假审批</h1>
        <p>审批通过后，请假时段内已确认的未来任务将解除原护理员并回到「待确认/待重新安排」。</p>
      </div>
    </header>

    <section class="panel">
      <div class="toolbar">
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
          <el-option v-for="o in statusOptions" :key="o.value || 'all'" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="() => { query.page = 1; loadList() }">查询</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无申请">
        <el-table-column label="护理员" min-width="120">
          <template #default="{ row }">{{ row.careStaffName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="employeeNo" label="工号" width="100">
          <template #default="{ row }">{{ row.employeeNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="160">
          <template #default="{ row }">{{ fmt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="160">
          <template #default="{ row }">{{ fmt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="审批人" width="110">
          <template #default="{ row }">{{ row.reviewedByName || '-' }}</template>
        </el-table-column>
        <el-table-column label="审批时间" min-width="160">
          <template #default="{ row }">{{ fmt(row.reviewedAt) }}</template>
        </el-table-column>
        <el-table-column prop="reviewRemark" label="审批备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="受影响订单" width="110">
          <template #default="{ row }">
            {{ row.status === 'APPROVED' ? (row.affectedOrderCount ?? 0) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="primary" @click="openReview(row, 'approve')">通过</el-button>
              <el-button link type="danger" @click="openReview(row, 'reject')">驳回</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="loadList"
        />
      </div>
    </section>

    <el-dialog
      v-model="reviewVisible"
      :title="reviewAction === 'approve' ? '审批通过' : '驳回申请'"
      width="480px"
      destroy-on-close
    >
      <p v-if="current" class="review-summary">
        {{ current.careStaffName }} · {{ fmt(current.startTime) }} ~ {{ fmt(current.endTime) }}
      </p>
      <el-input
        v-model="reviewForm.reviewRemark"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="审批备注（可选）"
      />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button
          :type="reviewAction === 'approve' ? 'primary' : 'danger'"
          :loading="reviewing"
          @click="submitReview"
        >
          确认{{ reviewAction === 'approve' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-leave-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-head h1 {
  margin: 0;
  font-size: 22px;
  color: #23415f;
}
.page-head p {
  margin: 6px 0 0;
  color: #7a94a8;
  font-size: 13px;
}
.panel {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 6px 18px rgba(36, 66, 92, 0.06);
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.muted {
  color: #9aa8b5;
}
.review-summary {
  margin: 0 0 12px;
  color: #4a6278;
  font-size: 13px;
}
</style>
