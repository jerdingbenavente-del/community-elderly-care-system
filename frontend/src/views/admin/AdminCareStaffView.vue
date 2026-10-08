<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { pageCareStaff } from '@/api/careStaff'
import { genderText } from '@/utils/familyHome'
import { maskPhone } from '@/utils/sensitive'
import CareStaffFormDrawer from '@/components/admin/CareStaffFormDrawer.vue'

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
const hasSearched = ref(false)

const query = reactive({
  name: '',
  employeeNo: '',
  phone: '',
  status: '',
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const editId = ref(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '在职', value: 1 },
  { label: '停用', value: 0 },
]

function staffStatusText(status) {
  if (status === 1) return '在职'
  if (status === 0) return '停用'
  return '未知'
}

async function loadList() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await pageCareStaff({
      page: query.page,
      size: query.size,
      name: query.name.trim() || undefined,
      employeeNo: query.employeeNo.trim() || undefined,
      phone: query.phone.trim() || undefined,
      status: query.status === '' ? undefined : query.status,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } catch (e) {
    rows.value = []
    total.value = 0
    loadError.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function onSearch() {
  hasSearched.value = true
  query.page = 1
  loadList()
}

function onReset() {
  query.name = ''
  query.employeeNo = ''
  query.phone = ''
  query.status = ''
  query.page = 1
  hasSearched.value = false
  loadList()
}

function onPageChange(p) {
  query.page = p
  loadList()
}

function onSizeChange(s) {
  query.size = s
  query.page = 1
  loadList()
}

function openCreate() {
  editId.value = null
  formVisible.value = true
}

function openEdit(row) {
  editId.value = row.id
  formVisible.value = true
}

function goDetail(row) {
  router.push(`/admin/staff/${row.id}`)
}

function emptyText() {
  if (hasSearched.value) return '未找到符合条件的护理员'
  return '暂无护理员档案'
}

onMounted(loadList)
</script>

<template>
  <div class="staff-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>护理员管理</h2>
        <p>管理养老服务中心护理员档案、账号关联和基础信息。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增护理员</el-button>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="姓名">
          <el-input v-model="query.name" clearable placeholder="模糊搜索" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="工号">
          <el-input v-model="query.employeeNo" clearable placeholder="精确匹配" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="query.phone" clearable placeholder="精确匹配" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width: 120px">
            <el-option
              v-for="o in statusOptions"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
          <el-button :icon="Refresh" @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      class="err"
      :title="loadError"
    >
      <template #default>
        <el-button type="primary" link @click="loadList">重新加载</el-button>
      </template>
    </el-alert>

    <div class="table-card">
      <el-table :data="rows" stripe :empty-text="emptyText()">
        <el-table-column prop="employeeNo" label="工号" min-width="100">
          <template #default="{ row }">{{ row.employeeNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="联系电话" min-width="120">
          <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
        </el-table-column>
        <el-table-column label="关联账号" min-width="140">
          <template #default="{ row }">
            <code v-if="row.username" class="uname">{{ row.username }}</code>
            <span v-else class="muted">未关联账号</span>
          </template>
        </el-table-column>
        <el-table-column prop="position" label="岗位" min-width="100">
          <template #default="{ row }">{{ row.position || '-' }}</template>
        </el-table-column>
        <el-table-column label="护理员状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ staffStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!rows.length && !loadError && !loading" class="empty-action">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增护理员</el-button>
      </div>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-size="query.size"
          :current-page="query.page"
          :page-sizes="[10, 20, 50]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <CareStaffFormDrawer v-model="formVisible" :staff-id="editId" @success="loadList" />
  </div>
</template>

<style scoped>
.staff-page {
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

.page-head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--ec-text);
}

.page-head p {
  margin: 0;
  color: var(--ec-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  max-width: 640px;
}

.filter-card,
.table-card {
  padding: 16px 18px;
  border-radius: var(--ec-radius-sm);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--ec-shadow);
}

.err {
  border-radius: 12px;
}

.uname {
  font-family: ui-monospace, Consolas, monospace;
  color: var(--ec-color-primary-dark);
}

.muted {
  color: var(--ec-text-secondary);
  font-size: 13px;
}

.empty-action {
  display: flex;
  justify-content: center;
  padding: 12px 0 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
