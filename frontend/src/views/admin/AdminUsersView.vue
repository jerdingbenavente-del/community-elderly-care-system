<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  deleteSystemUser,
  disableSystemUser,
  enableSystemUser,
  getSystemUser,
  pageSystemUsers,
} from '@/api/systemUser'
import { toastIfNeeded } from '@/api/request'
import { listElderFamilies } from '@/api/elderFamily'
import { pageCareStaff } from '@/api/careStaff'
import { useUserStore } from '@/stores/user'
import CreateAccountDrawer from '@/components/admin/CreateAccountDrawer.vue'

const userStore = useUserStore()

const canDelete = () =>
  (userStore.userInfo?.permissions || []).includes('system:user:delete')

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)

const query = reactive({
  username: '',
  realName: '',
  roleCode: '',
  status: '',
  page: 1,
  size: 10,
})

const createVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const detailBindings = ref([])
const detailStaff = ref(null)
const actionLoadingId = ref(null)

const roleOptions = [
  { label: '全部角色', value: '' },
  { label: 'ADMIN', value: 'ADMIN' },
  { label: 'FAMILY', value: 'FAMILY' },
  { label: 'CARE_STAFF', value: 'CARE_STAFF' },
]

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

const isSelf = (id) => userStore.userInfo?.userId === id

async function onDelete(row) {
  if (isSelf(row.id)) {
    ElMessage.warning('不能删除当前登录账号')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除账号「${row.username}」吗？\n删除后该账号将无法登录。\n如果账号存在业务绑定，系统将拒绝删除。\n是否继续？`,
      '删除账号',
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

  actionLoadingId.value = row.id
  try {
    await deleteSystemUser(row.id)
    ElMessage.success('删除成功')
    if (rows.value.length <= 1 && query.page > 1) {
      query.page -= 1
    }
    await loadUsers()
  } catch (e) {
    toastIfNeeded(e, '删除失败')
  } finally {
    actionLoadingId.value = null
  }
}

function roleTagType(code) {
  if (code === 'ADMIN') return 'danger'
  if (code === 'FAMILY') return 'success'
  if (code === 'CARE_STAFF') return 'warning'
  return 'info'
}

function statusText(status) {
  if (status === 1) return '启用'
  if (status === 0) return '停用'
  return '未知'
}

function mcpText(v) {
  return v ? '需要修改' : '已修改'
}

async function loadUsers() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await pageSystemUsers({
      page: query.page,
      size: query.size,
      username: query.username.trim() || undefined,
      realName: query.realName.trim() || undefined,
      roleCode: query.roleCode || undefined,
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
  query.page = 1
  loadUsers()
}

function onReset() {
  query.username = ''
  query.realName = ''
  query.roleCode = ''
  query.status = ''
  query.page = 1
  loadUsers()
}

function onPageChange(p) {
  query.page = p
  loadUsers()
}

function onSizeChange(s) {
  query.size = s
  query.page = 1
  loadUsers()
}

async function openDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  detailBindings.value = []
  detailStaff.value = null
  try {
    const res = await getSystemUser(row.id)
    if (res?.code !== 200) throw new Error(res?.message || '加载详情失败')
    detail.value = res.data
    const roles = res.data?.roles || []
    if (roles.includes('FAMILY')) {
      const bindRes = await listElderFamilies({ familyUserId: row.id })
      if (bindRes?.code === 200) {
        detailBindings.value = bindRes.data || []
      }
    }
    if (roles.includes('CARE_STAFF')) {
      const staffRes = await pageCareStaff({ userId: row.id, page: 1, size: 1 })
      if (staffRes?.code === 200) {
        detailStaff.value = staffRes.data?.records?.[0] || null
      }
    }
  } catch (e) {
    ElMessage.error(e.message || '加载详情失败')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function toggleStatus(row) {
  if (isSelf(row.id)) {
    ElMessage.warning('不能停用当前登录账号')
    return
  }
  const enabling = row.status === 0
  const action = enabling ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${action}账号「${row.username}」？`, '提示', {
      type: 'warning',
      confirmButtonText: action,
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  actionLoadingId.value = row.id
  try {
    await (enabling ? enableSystemUser(row.id) : disableSystemUser(row.id))
    ElMessage.success(`${action}成功`)
    await loadUsers()
  } catch (e) {
    toastIfNeeded(e, `${action}失败`)
  } finally {
    actionLoadingId.value = null
  }
}

onMounted(loadUsers)
</script>

<template>
  <div class="users-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>系统账号管理</h2>
        <p>创建家属 / 护理员业务账号，查看状态与首次改密标记。管理员账号不通过此入口创建。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="createVisible = true">新建账号</el-button>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="query.username" clearable placeholder="模糊搜索" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="query.realName" clearable placeholder="模糊搜索" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleCode" style="width: 140px">
            <el-option v-for="o in roleOptions" :key="String(o.value)" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width: 120px">
            <el-option v-for="o in statusOptions" :key="String(o.value)" :label="o.label" :value="o.value" />
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
        <el-button type="primary" link @click="loadUsers">重新加载</el-button>
      </template>
    </el-alert>

    <div class="table-card">
      <el-table :data="rows" stripe empty-text="暂无账号">
        <el-table-column prop="username" label="用户名" min-width="140">
          <template #default="{ row }">
            <code class="uname">{{ row.username }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <el-tag
              v-for="r in row.roles || []"
              :key="r"
              size="small"
              :type="roleTagType(r)"
              class="role-tag"
            >
              {{ r }}
            </el-tag>
            <span v-if="!(row.roles || []).length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="首次改密" width="110">
          <template #default="{ row }">
            <el-tag :type="row.mustChangePassword ? 'warning' : 'success'" size="small">
              {{ mcpText(row.mustChangePassword) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              :loading="actionLoadingId === row.id"
              :disabled="isSelf(row.id)"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button
              v-if="canDelete()"
              link
              type="danger"
              :loading="actionLoadingId === row.id"
              :disabled="isSelf(row.id)"
              @click="onDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <CreateAccountDrawer v-model="createVisible" @success="loadUsers" />

    <el-drawer v-model="detailVisible" title="账号详情" size="420px">
      <div v-loading="detailLoading" class="detail">
        <template v-if="detail">
          <div class="detail__row"><span>用户名</span><code>{{ detail.username }}</code></div>
          <div class="detail__row"><span>姓名</span><strong>{{ detail.realName || '-' }}</strong></div>
          <div class="detail__row">
            <span>角色</span>
            <div>
              <el-tag
                v-for="r in detail.roles || []"
                :key="r"
                size="small"
                :type="roleTagType(r)"
                class="role-tag"
              >
                {{ r }}
              </el-tag>
            </div>
          </div>
          <div class="detail__row"><span>状态</span>{{ statusText(detail.status) }}</div>
          <div class="detail__row"><span>首次改密</span>{{ mcpText(detail.mustChangePassword) }}</div>
          <div class="detail__row"><span>手机</span>{{ detail.phone || '-' }}</div>
          <div class="detail__row"><span>创建时间</span>{{ detail.createdAt || '-' }}</div>

          <template v-if="(detail.roles || []).includes('FAMILY')">
            <h4>已绑定老人</h4>
            <ul v-if="detailBindings.length" class="bind-list">
              <li v-for="b in detailBindings" :key="b.id">
                {{ b.elderName || `老人#${b.elderId}` }}
                <em>{{ b.relationship || '家属' }}</em>
              </li>
            </ul>
            <p v-else class="muted">暂无绑定</p>
          </template>

          <template v-if="(detail.roles || []).includes('CARE_STAFF')">
            <h4>护理员档案</h4>
            <template v-if="detailStaff">
              <div class="detail__row"><span>工号</span>{{ detailStaff.employeeNo || '-' }}</div>
              <div class="detail__row"><span>姓名</span>{{ detailStaff.name || '-' }}</div>
              <div class="detail__row"><span>岗位</span>{{ detailStaff.position || '-' }}</div>
            </template>
            <p v-else class="muted">暂无关联护理员档案</p>
          </template>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.users-page {
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

.role-tag {
  margin-right: 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.detail {
  min-height: 200px;
}

.detail__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(74, 144, 194, 0.1);
  font-size: 13px;
  color: var(--ec-text);
}

.detail__row span {
  color: var(--ec-text-muted);
  flex-shrink: 0;
}

.detail h4 {
  margin: 18px 0 8px;
  font-size: 14px;
  color: var(--ec-text);
}

.bind-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.bind-list li {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed rgba(74, 144, 194, 0.12);
}

.bind-list em {
  font-style: normal;
  color: var(--ec-text-muted);
  font-size: 12px;
}

.muted {
  color: var(--ec-text-muted);
  font-size: 13px;
}

@media (max-width: 900px) {
  .page-head {
    flex-direction: column;
  }
}
</style>
