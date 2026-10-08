<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, UserFilled } from '@element-plus/icons-vue'
import { getCareStaff } from '@/api/careStaff'
import { getSystemUser } from '@/api/systemUser'
import { genderText } from '@/utils/familyHome'
import { maskPhone } from '@/utils/sensitive'
import CareStaffFormDrawer from '@/components/admin/CareStaffFormDrawer.vue'

const route = useRoute()
const router = useRouter()

const staffId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const loading = ref(false)
const loadError = ref('')
const notFound = ref(false)
const staff = ref(null)
const account = ref(null)
const accountLoading = ref(false)
const editVisible = ref(false)

function staffStatusText(status) {
  if (status === 1) return '在职'
  if (status === 0) return '停用'
  return '未知'
}

function userStatusText(status) {
  if (status === 1) return '启用'
  if (status === 0) return '停用'
  return '未知'
}

function mcpText(v) {
  if (v === true) return '需要修改密码'
  if (v === false) return '已修改密码'
  return '-'
}

function roleTagType(code) {
  if (code === 'ADMIN') return 'danger'
  if (code === 'FAMILY') return 'success'
  if (code === 'CARE_STAFF') return 'warning'
  return 'info'
}

async function loadStaff() {
  if (!staffId.value) {
    loadError.value = '无效的护理员 ID'
    staff.value = null
    notFound.value = true
    return
  }
  loading.value = true
  loadError.value = ''
  notFound.value = false
  account.value = null
  try {
    const res = await getCareStaff(staffId.value)
    if (res?.code === 404 || res?.code === 40401) {
      notFound.value = true
      loadError.value = '护理员不存在或已删除'
      staff.value = null
      return
    }
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    staff.value = res.data
    if (staff.value?.userId) {
      loadAccount(staff.value.userId)
    }
  } catch (e) {
    staff.value = null
    const msg = e.message || '加载失败'
    if (msg.includes('不存在') || e?.code === 404) {
      notFound.value = true
      loadError.value = '护理员不存在或已删除'
    } else {
      loadError.value = msg
    }
  } finally {
    loading.value = false
  }
}

async function loadAccount(userId) {
  accountLoading.value = true
  try {
    const res = await getSystemUser(userId)
    if (res?.code !== 200) {
      account.value = null
      return
    }
    account.value = res.data
  } catch {
    account.value = null
  } finally {
    accountLoading.value = false
  }
}

function goBack() {
  router.push('/admin/staff')
}

function openEdit() {
  editVisible.value = true
}

watch(
  () => route.params.id,
  () => loadStaff(),
)

onMounted(loadStaff)
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <div class="page-head">
      <div class="page-head__left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回列表</el-button>
        <div class="title-row">
          <div class="avatar"><el-icon :size="28"><UserFilled /></el-icon></div>
          <div>
            <h2>{{ staff?.name || '护理员详情' }}</h2>
            <p v-if="staff">
              工号 {{ staff.employeeNo || '-' }} ·
              <el-tag :type="staff.status === 1 ? 'success' : 'info'" size="small">
                {{ staffStatusText(staff.status) }}
              </el-tag>
            </p>
          </div>
        </div>
      </div>
      <el-button v-if="staff" type="primary" @click="openEdit">编辑档案</el-button>
    </div>

    <el-alert
      v-if="loadError"
      :type="notFound ? 'warning' : 'error'"
      :closable="false"
      show-icon
      :title="loadError"
      class="err"
    >
      <template #default>
        <el-button v-if="notFound" type="primary" link @click="goBack">返回列表</el-button>
        <el-button v-else type="primary" link @click="loadStaff">重新加载</el-button>
      </template>
    </el-alert>

    <div v-if="staff" class="panel">
      <el-tabs>
        <el-tab-pane label="基本资料">
          <div class="info-grid">
            <div class="info-item"><span>姓名</span><strong>{{ staff.name }}</strong></div>
            <div class="info-item"><span>工号</span>{{ staff.employeeNo || '-' }}</div>
            <div class="info-item"><span>性别</span>{{ genderText(staff.gender) }}</div>
            <div class="info-item"><span>手机</span>{{ maskPhone(staff.phone) }}</div>
            <div class="info-item"><span>岗位</span>{{ staff.position || '-' }}</div>
            <div class="info-item">
              <span>护理员状态</span>
              <el-tag :type="staff.status === 1 ? 'success' : 'info'" size="small">
                {{ staffStatusText(staff.status) }}
              </el-tag>
            </div>
            <div class="info-item"><span>创建时间</span>{{ staff.createdAt || '-' }}</div>
            <div class="info-item"><span>更新时间</span>{{ staff.updatedAt || '-' }}</div>
            <div class="info-item wide"><span>备注</span>{{ staff.remark || '-' }}</div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="账号信息">
          <div v-loading="accountLoading" class="account-block">
            <template v-if="staff.userId">
              <div class="info-grid">
                <div class="info-item">
                  <span>用户名</span>
                  <code>{{ account?.username || staff.username || '-' }}</code>
                </div>
                <div class="info-item">
                  <span>账号姓名</span>{{ account?.realName || staff.realName || '-' }}
                </div>
                <div class="info-item">
                  <span>账号状态</span>
                  <template v-if="account">
                    <el-tag :type="account.status === 1 ? 'success' : 'info'" size="small">
                      {{ userStatusText(account.status) }}
                    </el-tag>
                  </template>
                  <span v-else>-</span>
                </div>
                <div class="info-item">
                  <span>首次改密</span>{{ mcpText(account?.mustChangePassword) }}
                </div>
                <div class="info-item wide">
                  <span>角色</span>
                  <template v-if="account?.roles?.length">
                    <el-tag
                      v-for="r in account.roles"
                      :key="r"
                      size="small"
                      :type="roleTagType(r)"
                      class="role-tag"
                    >
                      {{ r }}
                    </el-tag>
                  </template>
                  <span v-else>CARE_STAFF（以绑定校验为准）</span>
                </div>
                <div class="info-item">
                  <span>账号手机</span>{{ maskPhone(account?.phone) }}
                </div>
                <div class="info-item"><span>用户 ID</span>{{ staff.userId }}</div>
              </div>
            </template>
            <p v-else class="muted">未关联系统账号</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <CareStaffFormDrawer
      v-if="staffId"
      v-model="editVisible"
      :staff-id="staffId"
      @success="loadStaff"
    />
  </div>
</template>

<style scoped>
.detail-page {
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

.page-head__left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(105, 185, 140, 0.18);
  color: #3d8f63;
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
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.err {
  border-radius: 12px;
}

.panel {
  padding: 8px 18px 18px;
  border-radius: var(--ec-radius-sm);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--ec-shadow);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
  padding: 8px 0 4px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
  color: var(--ec-text);
}

.info-item span {
  font-size: 12px;
  color: var(--ec-text-secondary);
}

.info-item.wide {
  grid-column: 1 / -1;
}

.account-block {
  min-height: 80px;
}

.muted {
  color: var(--ec-text-secondary);
  padding: 12px 0;
}

.role-tag {
  margin-right: 6px;
}

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
