<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { cancelLeave, createLeave, pageMyLeaves } from '@/api/careStaffLeave'
import { toastIfNeeded } from '@/api/request'

const loading = ref(false)
const submitting = ref(false)
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)

const query = reactive({
  status: '',
  page: 1,
  size: 10,
})

const form = reactive({
  range: [],
  reason: '',
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
    const res = await pageMyLeaves({
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
    toastIfNeeded(e, '加载请假列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.range = []
  form.reason = ''
  dialogVisible.value = true
}

async function submitCreate() {
  if (!form.range || form.range.length !== 2) {
    ElMessage.warning('请选择开始与结束时间')
    return
  }
  if (!form.reason?.trim()) {
    ElMessage.warning('请填写原因')
    return
  }
  submitting.value = true
  try {
    const res = await createLeave({
      startTime: form.range[0],
      endTime: form.range[1],
      reason: form.reason.trim(),
    })
    if (res?.code !== 200) throw new Error(res?.message || '提交失败')
    ElMessage.success('申请已提交，等待管理员审批')
    dialogVisible.value = false
    query.page = 1
    await loadList()
  } catch (e) {
    toastIfNeeded(e, '提交失败')
  } finally {
    submitting.value = false
  }
}

async function onCancel(row) {
  try {
    await ElMessageBox.confirm('确定撤销该待审批申请？', '撤销确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await cancelLeave(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '撤销失败')
    ElMessage.success('已撤销')
    await loadList()
  } catch (e) {
    toastIfNeeded(e, '撤销失败')
  }
}

onMounted(loadList)
</script>

<template>
  <div class="staff-leave-page">
    <header class="page-head">
      <div>
        <h1>临时请假</h1>
        <p>临时无法值班时提交申请，经管理员审批通过后，相关未来任务将退回待重新安排。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建申请</el-button>
    </header>

    <section class="panel">
      <div class="toolbar">
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
          <el-option v-for="o in statusOptions" :key="o.value || 'all'" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="() => { query.page = 1; loadList() }">查询</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无请假申请">
        <el-table-column label="开始时间" min-width="160">
          <template #default="{ row }">{{ fmt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="160">
          <template #default="{ row }">{{ fmt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="审批时间" min-width="160">
          <template #default="{ row }">{{ fmt(row.reviewedAt) }}</template>
        </el-table-column>
        <el-table-column prop="reviewRemark" label="审批备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              link
              type="danger"
              @click="onCancel(row)"
            >
              撤销
            </el-button>
            <span v-else class="muted">—</span>
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

    <el-dialog v-model="dialogVisible" title="新建临时请假" width="520px" destroy-on-close>
      <el-form label-width="88px">
        <el-form-item label="请假时段" required>
          <el-date-picker
            v-model="form.range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请说明临时无法值班的原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.staff-leave-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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
  background: rgba(255, 255, 255, 0.88);
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 8px 24px rgba(80, 120, 150, 0.06);
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
</style>
