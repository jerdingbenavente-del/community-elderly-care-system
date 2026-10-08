<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  cancelActivity,
  createActivity,
  deleteActivity,
  pageAdminActivities,
  publishActivity,
  updateActivity,
} from '@/api/activity'
import { toastIfNeeded } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref(null)

const query = reactive({
  keyword: '',
  status: '',
  dateFrom: '',
  dateTo: '',
  page: 1,
  size: 10,
})

const form = reactive({
  activityName: '',
  activityDate: '',
  startTime: '',
  endTime: '',
  location: '',
  description: '',
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已结束', value: 'COMPLETED' },
]

function statusMeta(s) {
  if (s === 'DRAFT') return { label: '草稿', type: 'info' }
  if (s === 'PUBLISHED') return { label: '已发布', type: 'success' }
  if (s === 'CANCELLED') return { label: '已取消', type: 'warning' }
  if (s === 'COMPLETED') return { label: '已结束', type: '' }
  return { label: s || '-', type: 'info' }
}

function fmtTime(v) {
  if (!v) return '-'
  return String(v).slice(0, 5)
}

function fmtDt(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

async function loadList() {
  loading.value = true
  try {
    const res = await pageAdminActivities({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      dateFrom: query.dateFrom || undefined,
      dateTo: query.dateTo || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    toastIfNeeded(e, '加载活动列表失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    activityName: '',
    activityDate: '',
    startTime: '09:00',
    endTime: '10:00',
    location: '',
    description: '',
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    activityName: row.activityName,
    activityDate: row.activityDate,
    startTime: fmtTime(row.startTime),
    endTime: fmtTime(row.endTime),
    location: row.location,
    description: row.description || '',
  })
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.activityName?.trim() || !form.activityDate || !form.startTime || !form.endTime || !form.location?.trim()) {
    ElMessage.warning('请完整填写活动信息')
    return
  }
  saving.value = true
  try {
    const payload = {
      activityName: form.activityName.trim(),
      activityDate: form.activityDate,
      startTime: form.startTime.length === 5 ? `${form.startTime}:00` : form.startTime,
      endTime: form.endTime.length === 5 ? `${form.endTime}:00` : form.endTime,
      location: form.location.trim(),
      description: form.description?.trim() || undefined,
    }
    const res = editingId.value
      ? await updateActivity(editingId.value, payload)
      : await createActivity(payload)
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    ElMessage.success(editingId.value ? '已更新' : '已创建为草稿')
    dialogVisible.value = false
    await loadList()
  } catch (e) {
    toastIfNeeded(e, '保存失败')
  } finally {
    saving.value = false
  }
}

async function onPublish(row) {
  try {
    await ElMessageBox.confirm(`确认发布「${row.activityName}」？发布后家属与护理员可见。`, '发布活动', { type: 'warning' })
    const res = await publishActivity(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '发布失败')
    ElMessage.success('已发布')
    await loadList()
  } catch (e) {
    if (e !== 'cancel') toastIfNeeded(e, '发布失败')
  }
}

async function onCancel(row) {
  try {
    await ElMessageBox.confirm(`确认取消「${row.activityName}」？`, '取消活动', { type: 'warning' })
    const res = await cancelActivity(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '取消失败')
    ElMessage.success('已取消')
    await loadList()
  } catch (e) {
    if (e !== 'cancel') toastIfNeeded(e, '取消失败')
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除草稿「${row.activityName}」？`, '删除', { type: 'warning' })
    const res = await deleteActivity(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '删除失败')
    ElMessage.success('已删除')
    await loadList()
  } catch (e) {
    if (e !== 'cancel') toastIfNeeded(e, '删除失败')
  }
}

onMounted(loadList)
</script>

<template>
  <div class="admin-activity-page">
    <header class="page-head">
      <div>
        <h1>社区活动管理</h1>
        <p>创建草稿 → 发布后家属与护理员可见。本阶段不含报名、收费与签到。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增活动</el-button>
    </header>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="名称/地点" style="width: 180px" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px">
          <el-option v-for="o in statusOptions" :key="o.value || 'all'" :label="o.label" :value="o.value" />
        </el-select>
        <el-date-picker v-model="query.dateFrom" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
        <el-date-picker v-model="query.dateTo" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
        <el-button type="primary" :icon="Search" @click="() => { query.page = 1; loadList() }">查询</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无活动">
        <el-table-column prop="activityName" label="活动名称" min-width="140" />
        <el-table-column prop="activityDate" label="日期" width="120" />
        <el-table-column label="时间" width="130">
          <template #default="{ row }">{{ fmtTime(row.startTime) }}～{{ fmtTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="location" label="地点" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="创建人" width="100" />
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ fmtDt(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'PUBLISHED'"
              link type="primary"
              @click="openEdit(row)"
            >编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="success" @click="onPublish(row)">发布</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'PUBLISHED'"
              link type="warning"
              @click="onCancel(row)"
            >取消</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadList"
          @size-change="() => { query.page = 1; loadList() }"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑活动' : '新增活动'" width="520px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="活动名称" required>
          <el-input v-model="form.activityName" maxlength="100" />
        </el-form-item>
        <el-form-item label="活动日期" required>
          <el-date-picker v-model="form.activityDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间" required>
          <el-time-picker v-model="form.startTime" value-format="HH:mm" format="HH:mm" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" required>
          <el-time-picker v-model="form.endTime" value-format="HH:mm" format="HH:mm" style="width: 100%" />
        </el-form-item>
        <el-form-item label="活动地点" required>
          <el-input v-model="form.location" maxlength="200" />
        </el-form-item>
        <el-form-item label="活动简介">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-activity-page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.page-head h1 { margin: 0 0 6px; font-size: 22px; }
.page-head p { margin: 0; color: #667085; }
.panel { background: #fff; border-radius: 12px; padding: 16px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 14px; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
</style>
