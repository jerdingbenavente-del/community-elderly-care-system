<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, FirstAidKit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCareServiceItem, updateCareServiceItem } from '@/api/careServiceItem'
import { formatPrice, serviceTypeLabel } from '@/utils/familyHome'
import CareServiceItemFormDrawer from '@/components/admin/CareServiceItemFormDrawer.vue'

const route = useRoute()
const router = useRouter()

const itemId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const loading = ref(false)
const loadError = ref('')
const notFound = ref(false)
const item = ref(null)
const editVisible = ref(false)
const statusLoading = ref(false)

function statusText(status) {
  if (status === 'ENABLED') return '启用'
  if (status === 'DISABLED') return '停用'
  return status || '-'
}

function durationText(minutes) {
  if (minutes == null) return '-'
  return `${minutes} 分钟`
}

async function loadDetail() {
  if (!itemId.value) {
    loadError.value = '无效的服务项目 ID'
    item.value = null
    notFound.value = true
    return
  }
  loading.value = true
  loadError.value = ''
  notFound.value = false
  try {
    const res = await getCareServiceItem(itemId.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    item.value = res.data
  } catch (e) {
    item.value = null
    const msg = e.message || '加载失败'
    if (e?.code === 404 || msg.includes('不存在')) {
      notFound.value = true
      loadError.value = '服务项目不存在或已删除'
    } else {
      loadError.value = msg
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/admin/services')
}

function openEdit() {
  editVisible.value = true
}

async function toggleStatus() {
  if (!item.value) return
  const next = item.value.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
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
  statusLoading.value = true
  try {
    const res = await updateCareServiceItem(item.value.id, {
      serviceName: item.value.serviceName,
      serviceType: item.value.serviceType || undefined,
      description: item.value.description || undefined,
      durationMinutes: item.value.durationMinutes,
      price: item.value.price != null ? Number(item.value.price) : null,
      status: next,
    })
    if (res?.code !== 200) throw new Error(res?.message || '状态更新失败')
    ElMessage.success(next === 'ENABLED' ? '已启用' : '已停用')
    await loadDetail()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '状态更新失败')
  } finally {
    statusLoading.value = false
  }
}

watch(
  () => route.params.id,
  () => loadDetail(),
)

onMounted(loadDetail)
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <div class="page-head">
      <div class="page-head__left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回列表</el-button>
        <div class="title-row">
          <div class="avatar"><el-icon :size="28"><FirstAidKit /></el-icon></div>
          <div>
            <h2>{{ item?.serviceName || '服务项目详情' }}</h2>
            <p v-if="item">
              {{ item.serviceCode }} ·
              <el-tag :type="item.status === 'ENABLED' ? 'success' : 'info'" size="small">
                {{ statusText(item.status) }}
              </el-tag>
            </p>
          </div>
        </div>
      </div>
      <div v-if="item" class="page-head__actions">
        <el-button
          :type="item.status === 'ENABLED' ? 'warning' : 'success'"
          :loading="statusLoading"
          @click="toggleStatus"
        >
          {{ item.status === 'ENABLED' ? '停用' : '启用' }}
        </el-button>
        <el-button type="primary" @click="openEdit">编辑</el-button>
      </div>
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
        <el-button v-else type="primary" link @click="loadDetail">重新加载</el-button>
      </template>
    </el-alert>

    <div v-if="item" class="panel">
      <div class="info-grid">
        <div class="info-item"><span>服务名称</span><strong>{{ item.serviceName }}</strong></div>
        <div class="info-item"><span>服务编码</span><code>{{ item.serviceCode }}</code></div>
        <div class="info-item"><span>服务类型</span>{{ serviceTypeLabel(item.serviceType) }}</div>
        <div class="info-item"><span>时长</span>{{ durationText(item.durationMinutes) }}</div>
        <div class="info-item"><span>参考价格</span>{{ formatPrice(item.price) }}</div>
        <div class="info-item">
          <span>状态</span>
          <el-tag :type="item.status === 'ENABLED' ? 'success' : 'info'" size="small">
            {{ statusText(item.status) }}
          </el-tag>
        </div>
        <div class="info-item"><span>创建时间</span>{{ item.createdAt || '-' }}</div>
        <div class="info-item"><span>更新时间</span>{{ item.updatedAt || '-' }}</div>
        <div class="info-item wide"><span>服务说明</span>{{ item.description || '-' }}</div>
      </div>
    </div>

    <CareServiceItemFormDrawer
      v-if="itemId"
      v-model="editVisible"
      :item-id="itemId"
      @success="loadDetail"
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

.page-head__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
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
  background: rgba(91, 184, 176, 0.18);
  color: #3a9a92;
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
  padding: 18px;
  border-radius: var(--ec-radius-sm);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--ec-shadow);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
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

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
