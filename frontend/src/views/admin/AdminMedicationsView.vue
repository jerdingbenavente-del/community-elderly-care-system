<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { pageElders } from '@/api/adminElder'
import {
  createMedication,
  deleteMedication,
  disableMedication,
  enableMedication,
  pageAdminMedications,
  updateMedication,
} from '@/api/elderMedication'
import { toastIfNeeded } from '@/api/request'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const elders = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)

const query = reactive({
  elderId: null,
  medicineName: '',
  status: '',
  page: 1,
  size: 10,
})

const form = reactive({
  elderId: null,
  medicineName: '',
  dosage: '',
  dosageUnit: '片',
  usageMethod: '口服',
  doseTimes: ['08:00:00'],
  startDate: '',
  endDate: '',
  remark: '',
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
]

function statusMeta(s) {
  if (s === 'ACTIVE') return { label: '启用', type: 'success' }
  if (s === 'INACTIVE') return { label: '停用', type: 'info' }
  return { label: s || '-', type: 'info' }
}

function fmtTimes(list) {
  if (!list?.length) return '-'
  return list.map((t) => String(t).slice(0, 5)).join(' / ')
}

async function loadElders() {
  try {
    const res = await pageElders({ page: 1, size: 100, status: 1 })
    elders.value = res?.data?.records || []
  } catch {
    elders.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await pageAdminMedications({
      page: query.page,
      size: query.size,
      elderId: query.elderId || undefined,
      medicineName: query.medicineName || undefined,
      status: query.status || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    toastIfNeeded(e, '加载用药列表失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.elderId = null
  form.medicineName = ''
  form.dosage = ''
  form.dosageUnit = '片'
  form.usageMethod = '口服'
  form.doseTimes = ['08:00:00']
  form.startDate = ''
  form.endDate = ''
  form.remark = ''
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.elderId = row.elderId
  form.medicineName = row.medicineName
  form.dosage = row.dosage
  form.dosageUnit = row.dosageUnit
  form.usageMethod = row.usageMethod
  form.doseTimes = (row.doseTimes || ['08:00:00']).map((t) => String(t).length === 5 ? `${t}:00` : String(t))
  form.startDate = row.startDate
  form.endDate = row.endDate
  form.remark = row.remark || ''
  dialogVisible.value = true
}

function addTime() {
  form.doseTimes.push('12:00:00')
}

function removeTime(index) {
  form.doseTimes.splice(index, 1)
}

async function submit() {
  saving.value = true
  try {
    const payload = {
      elderId: form.elderId,
      medicineName: form.medicineName,
      dosage: form.dosage,
      dosageUnit: form.dosageUnit,
      usageMethod: form.usageMethod,
      startDate: form.startDate,
      endDate: form.endDate,
      remark: form.remark || undefined,
      doseTimes: form.doseTimes.filter(Boolean).map((t) => String(t).slice(0, 5)),
    }
    const res = editingId.value
      ? await updateMedication(editingId.value, payload)
      : await createMedication(payload)
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    ElMessage.success(editingId.value ? '已保存' : '已新增')
    dialogVisible.value = false
    await loadList()
  } catch (e) {
    toastIfNeeded(e, '保存失败')
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  try {
    const res = row.status === 'ACTIVE' ? await disableMedication(row.id) : await enableMedication(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '操作失败')
    ElMessage.success(row.status === 'ACTIVE' ? '已停用' : '已启用')
    await loadList()
  } catch (e) {
    toastIfNeeded(e, '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.medicineName}」？`, '删除用药', { type: 'warning' })
  } catch {
    return
  }
  try {
    const res = await deleteMedication(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '删除失败')
    ElMessage.success('已删除')
    await loadList()
  } catch (e) {
    toastIfNeeded(e, '删除失败')
  }
}

onMounted(async () => {
  await loadElders()
  await loadList()
})
</script>

<template>
  <div class="admin-med-page">
    <header class="page-head">
      <div>
        <h1>老人用药管理</h1>
        <p>维护老人用药计划。停用后不再产生当日提醒。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增用药</el-button>
    </header>
    <section class="panel">
      <div class="toolbar">
        <el-select v-model="query.elderId" clearable filterable placeholder="老人" style="width: 180px">
          <el-option v-for="e in elders" :key="e.id" :label="e.name" :value="e.id" />
        </el-select>
        <el-input v-model="query.medicineName" clearable placeholder="药品名称" style="width: 180px" />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 120px">
          <el-option v-for="o in statusOptions" :key="o.value || 'all'" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="() => { query.page = 1; loadList() }">查询</el-button>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无用药计划">
        <el-table-column label="老人" min-width="100">
          <template #default="{ row }">{{ row.elderName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="medicineName" label="药品名称" min-width="120" />
        <el-table-column label="剂量" width="100">
          <template #default="{ row }">{{ row.dosage }}{{ row.dosageUnit }}</template>
        </el-table-column>
        <el-table-column prop="usageMethod" label="服用方式" width="100" />
        <el-table-column label="服药时间" min-width="140">
          <template #default="{ row }">{{ fmtTimes(row.doseTimes) }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="toggle(row)">{{ row.status === 'ACTIVE' ? '停用' : '启用' }}</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑用药' : '新增用药'" width="520px">
      <el-form label-width="90px">
        <el-form-item label="老人">
          <el-select v-model="form.elderId" filterable placeholder="选择老人" style="width: 100%">
            <el-option v-for="e in elders" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="药品名称"><el-input v-model="form.medicineName" /></el-form-item>
        <el-form-item label="剂量">
          <div class="dose-row">
            <el-input v-model="form.dosage" placeholder="数量" />
            <el-input v-model="form.dosageUnit" placeholder="单位" style="width: 100px" />
          </div>
        </el-form-item>
        <el-form-item label="服用方式"><el-input v-model="form.usageMethod" /></el-form-item>
        <el-form-item label="服药时间">
          <div v-for="(t, i) in form.doseTimes" :key="i" class="time-row">
            <el-time-picker v-model="form.doseTimes[i]" value-format="HH:mm:ss" placeholder="时间" />
            <el-button v-if="form.doseTimes.length > 1" link type="danger" @click="removeTime(i)">移除</el-button>
          </div>
          <el-button link type="primary" @click="addTime">添加时间</el-button>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-med-page { display: flex; flex-direction: column; gap: 16px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.page-head h1 { margin: 0; font-size: 22px; color: #23415f; }
.page-head p { margin: 6px 0 0; color: #7a94a8; font-size: 13px; }
.panel { background: #fff; border-radius: 12px; padding: 16px; box-shadow: 0 6px 18px rgba(36, 66, 92, 0.06); }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.dose-row, .time-row { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
</style>
