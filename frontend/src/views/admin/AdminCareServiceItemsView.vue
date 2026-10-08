<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  pageCareServiceItems,
  updateCareServiceItem,
  deleteCareServiceItem,
} from '@/api/careServiceItem'
import { formatPrice, serviceTypeLabel } from '@/utils/familyHome'
import CareServiceItemFormDrawer from '@/components/admin/CareServiceItemFormDrawer.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)
const hasSearched = ref(false)
const statusLoadingId = ref(null)
const deleteLoadingId = ref(null)

const canDelete = () =>
  (userStore.userInfo?.permissions || []).includes('care:service:delete')

const query = reactive({
  serviceName: '',
  serviceCode: '',
  status: '',
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const editId = ref(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
]

function statusText(status) {
  if (status === 'ENABLED') return '启用'
  if (status === 'DISABLED') return '停用'
  return status || '-'
}

function statusTagType(status) {
  if (status === 'ENABLED') return 'success'
  if (status === 'DISABLED') return 'info'
  return 'info'
}

function durationText(minutes) {
  if (minutes == null) return '-'
  return `${minutes} 分钟`
}

async function loadList() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await pageCareServiceItems({
      page: query.page,
      size: query.size,
      serviceName: query.serviceName.trim() || undefined,
      serviceCode: query.serviceCode.trim() || undefined,
      status: query.status || undefined,
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
  query.serviceName = ''
  query.serviceCode = ''
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
  router.push(`/admin/services/${row.id}`)
}

function emptyText() {
  if (hasSearched.value) return '未找到符合条件的服务项目'
  return '暂无照护服务项目'
}

async function toggleStatus(row) {
  const next = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  if (next === 'DISABLED') {
    try {
      await ElMessageBox.confirm(
        '停用后可能无法继续用于新的服务预约，是否确认停用？',
        '停用服务项目',
        { type: 'warning', confirmButtonText: '确认停用', cancelButtonText: '取消' },
      )
    } catch {
      return
    }
  }
  statusLoadingId.value = row.id
  try {
    const res = await updateCareServiceItem(row.id, {
      serviceName: row.serviceName,
      serviceType: row.serviceType || undefined,
      description: row.description || undefined,
      durationMinutes: row.durationMinutes,
      price: row.price != null ? Number(row.price) : null,
      status: next,
    })
    if (res?.code !== 200) throw new Error(res?.message || '状态更新失败')
    ElMessage.success(next === 'ENABLED' ? '已启用' : '已停用')
    await loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '状态更新失败')
  } finally {
    statusLoadingId.value = null
  }
}

async function onDelete(row) {
  if (!canDelete()) {
    ElMessage.warning('您没有删除服务项目的权限')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认删除服务项目「${row.serviceName}」吗？\n删除后该项目将不再作为可用服务展示。\n历史订单不会被删除。`,
      '删除服务项目',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        distinguishCancelAndClose: true,
      },
    )
  } catch {
    return
  }

  deleteLoadingId.value = row.id
  try {
    const res = await deleteCareServiceItem(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '删除失败')
    ElMessage.success('删除成功')
    if (rows.value.length <= 1 && query.page > 1) {
      query.page -= 1
    }
    await loadList()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  } finally {
    deleteLoadingId.value = null
  }
}

onMounted(loadList)
</script>

<template>
  <div class="svc-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>照护服务管理</h2>
        <p>管理养老服务中心提供的照护服务项目。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增服务项目</el-button>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="服务名称">
          <el-input
            v-model="query.serviceName"
            clearable
            placeholder="模糊搜索"
            @keyup.enter="onSearch"
          />
        </el-form-item>
        <el-form-item label="服务编码">
          <el-input
            v-model="query.serviceCode"
            clearable
            placeholder="精确匹配"
            @keyup.enter="onSearch"
          />
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
        <el-table-column prop="serviceCode" label="编码" min-width="110" />
        <el-table-column prop="serviceName" label="服务名称" min-width="130" />
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ serviceTypeLabel(row.serviceType) }}</template>
        </el-table-column>
        <el-table-column label="服务说明" min-width="180">
          <template #default="{ row }">
            <el-tooltip
              v-if="row.description"
              :content="row.description"
              placement="top"
              :show-after="400"
            >
              <span class="desc-ellipsis">{{ row.description }}</span>
            </el-tooltip>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="100">
          <template #default="{ row }">{{ durationText(row.durationMinutes) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="100">
          <template #default="{ row }">{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 'ENABLED' ? 'warning' : 'success'"
              :loading="statusLoadingId === row.id"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button
              v-if="canDelete()"
              link
              type="danger"
              :loading="deleteLoadingId === row.id"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!rows.length && !loadError && !loading" class="empty-action">
        <el-button type="primary" :icon="Plus" @click="openCreate">新增服务项目</el-button>
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

    <CareServiceItemFormDrawer v-model="formVisible" :item-id="editId" @success="loadList" />
  </div>
</template>

<style scoped>
.svc-page {
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

.desc-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.muted {
  color: var(--ec-text-secondary);
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
