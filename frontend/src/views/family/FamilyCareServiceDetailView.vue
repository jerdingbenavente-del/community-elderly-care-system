<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, FirstAidKit } from '@element-plus/icons-vue'
import { getCareServiceDetail } from '@/api/careService'
import { unwrap, formatPrice, serviceTypeLabel, formatDateTime } from '@/utils/familyHome'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const item = ref(null)
const errorCode = ref(null)
const errorMessage = ref('')

const serviceId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const canBook = computed(() => item.value && item.value.status === 'ENABLED')

async function load() {
  if (!serviceId.value) {
    errorCode.value = 404
    errorMessage.value = '服务项目不存在。'
    item.value = null
    return
  }

  loading.value = true
  errorCode.value = null
  errorMessage.value = ''
  item.value = null

  try {
    item.value = unwrap(await getCareServiceDetail(serviceId.value))
  } catch (e) {
    errorCode.value = e.code ?? null
    if (e.code === 403) {
      errorMessage.value = '您没有权限执行此操作。'
    } else if (e.code === 404) {
      errorMessage.value = '服务项目不存在。'
    } else if (e.code === 500) {
      errorMessage.value = '系统服务异常，请稍后再试。'
    } else {
      errorMessage.value = e.message || '加载失败'
    }
    if (e.code !== 401 && e.code !== 403 && e.code !== 404) {
      ElMessage.error(errorMessage.value)
    }
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/family/care-services')
}

function goBook() {
  if (!canBook.value) return
  router.push(`/family/care-services/${serviceId.value}/book`)
}

watch(serviceId, () => load())

onMounted(() => {
  load()
})
</script>

<template>
  <div class="detail-page">
    <button type="button" class="back" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      返回服务列表
    </button>

    <el-skeleton v-if="loading" class="panel" :rows="8" animated />

    <div v-else-if="errorMessage" class="panel">
      <el-result
        :icon="errorCode === 404 ? 'info' : 'warning'"
        :title="errorCode === 404 ? '服务项目不存在' : '无法查看服务'"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button type="primary" @click="goBack">返回服务列表</el-button>
        </template>
      </el-result>
    </div>

    <template v-else-if="item">
      <section class="hero panel">
        <div class="hero__icon">
          <el-icon :size="36"><FirstAidKit /></el-icon>
        </div>
        <div class="hero__meta">
          <h1>{{ item.serviceName }}</h1>
          <p class="type">{{ serviceTypeLabel(item.serviceType) }}</p>
          <p class="desc">{{ item.description || '暂无项目说明' }}</p>
          <el-tag size="small" :type="canBook ? 'success' : 'info'" effect="light">
            {{ canBook ? '可预约' : item.status || '不可预约' }}
          </el-tag>
        </div>
      </section>

      <section class="panel info">
        <h3>服务说明</h3>
        <dl class="info-grid">
          <div>
            <dt>服务编码</dt>
            <dd>{{ item.serviceCode || '-' }}</dd>
          </div>
          <div>
            <dt>服务类型</dt>
            <dd>{{ serviceTypeLabel(item.serviceType) }}</dd>
          </div>
          <div>
            <dt>服务时长</dt>
            <dd>{{ item.durationMinutes ?? '-' }} 分钟</dd>
          </div>
          <div>
            <dt>服务价格</dt>
            <dd class="price">{{ formatPrice(item.price) }}</dd>
          </div>
          <div>
            <dt>服务状态</dt>
            <dd>{{ item.status === 'ENABLED' ? '启用' : item.status || '-' }}</dd>
          </div>
          <div>
            <dt>更新时间</dt>
            <dd>{{ formatDateTime(item.updatedAt || item.createdAt) }}</dd>
          </div>
        </dl>
        <p class="info-note">
          {{ item.description || '专业护理人员提供贴心照护服务，具体安排以管理员确认为准。' }}
        </p>
      </section>

      <section class="panel action-bar">
        <div>
          <div class="action-bar__price">{{ formatPrice(item.price) }}</div>
          <div class="action-bar__dur">预计时长 {{ item.durationMinutes ?? '-' }} 分钟</div>
        </div>
        <el-button type="primary" size="large" round :disabled="!canBook" @click="goBook">
          预约服务
        </el-button>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.back {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border: none;
  background: transparent;
  color: #4a90c2;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
}

.panel {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 20px;
}

.hero {
  display: flex;
  gap: 18px;
  align-items: flex-start;
  background: linear-gradient(120deg, rgba(234, 244, 251, 0.95), rgba(238, 247, 243, 0.9));
}

.hero__icon {
  width: 78px;
  height: 78px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #4a90c2, #69b98c);
  flex-shrink: 0;
}

.hero__meta h1 {
  margin: 0;
  font-size: 22px;
  color: #2c4a5e;
}

.hero__meta .type {
  margin: 6px 0 0;
  font-size: 13px;
  color: #4a90c2;
  font-weight: 600;
}

.hero__meta .desc {
  margin: 10px 0 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #718096;
}

.info h3 {
  margin: 0 0 14px;
  font-size: 15px;
  color: #2c4a5e;
}

.info-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.info-grid div {
  padding: 12px;
  border-radius: 12px;
  background: rgba(234, 244, 251, 0.45);
}

.info-grid dt {
  font-size: 12px;
  color: #8aa0b5;
  margin-bottom: 4px;
}

.info-grid dd {
  margin: 0;
  font-size: 14px;
  color: #2c4a5e;
}

.info-grid .price {
  color: #c8782a;
  font-weight: 700;
}

.info-note {
  margin: 14px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #718096;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.action-bar__price {
  font-size: 22px;
  font-weight: 700;
  color: #c8782a;
}

.action-bar__dur {
  margin-top: 4px;
  font-size: 13px;
  color: #718096;
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hero {
    flex-direction: column;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
