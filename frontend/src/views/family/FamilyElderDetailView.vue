<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Female,
  FirstAidKit,
  Male,
  Monitor,
  Bell,
  User,
} from '@element-plus/icons-vue'
import { getElderDetail, getEmergencyContacts } from '@/api/elder'
import { pageFamilyMedications } from '@/api/elderMedication'
import { unwrap, calcAge, genderText } from '@/utils/familyHome'
import { elderStatusText, maskPhone } from '@/utils/sensitive'
import ElderProfile from '@/components/family/ElderProfile.vue'
import EmergencyContactCard from '@/components/family/EmergencyContactCard.vue'
import FamilyRelationCard from '@/components/family/FamilyRelationCard.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const contactsLoading = ref(false)
const errorCode = ref(null)
const errorMessage = ref('')
const elder = ref(null)
const contacts = ref([])
const medications = ref([])
const medicationLoading = ref(false)
const medicationError = ref('')

const elderId = computed(() => {
  const raw = route.params.id
  const n = Number(raw)
  return Number.isFinite(n) ? n : null
})

const age = computed(() => calcAge(elder.value?.birthDate))
const gender = computed(() => genderText(elder.value?.gender))
const statusLabel = computed(() => elderStatusText(elder.value?.status))
const statusOk = computed(() => elder.value?.status === 1)

const avatarIcon = computed(() => {
  if (elder.value?.gender === 1) return Male
  if (elder.value?.gender === 2) return Female
  return User
})

const forbidden = computed(() => errorCode.value === 403)
const notFound = computed(() => errorCode.value === 404)

function mapError(e) {
  const code = e?.code
  errorCode.value = code ?? null
  if (code === 403) {
    errorMessage.value = '您没有权限查看该老人档案。'
  } else if (code === 404) {
    errorMessage.value = '老人档案不存在。'
  } else if (code === 500) {
    errorMessage.value = '系统服务异常，请稍后重试。'
  } else {
    errorMessage.value = e?.message || '加载失败'
  }
}

async function loadDetail() {
  if (!elderId.value) {
    errorCode.value = 404
    errorMessage.value = '老人档案不存在。'
    elder.value = null
    contacts.value = []
    medications.value = []
    return
  }

  loading.value = true
  contactsLoading.value = true
  errorCode.value = null
  errorMessage.value = ''
  elder.value = null
  contacts.value = []
  medications.value = []

  try {
    const data = unwrap(await getElderDetail(elderId.value))
    elder.value = data
    const embedded = Array.isArray(data?.emergencyContacts)
      ? data.emergencyContacts
      : null

    try {
      const list = unwrap(await getEmergencyContacts(elderId.value))
      contacts.value = Array.isArray(list) ? list : embedded || []
    } catch {
      contacts.value = embedded || []
    }
    await loadMedications(elderId.value)
  } catch (e) {
    mapError(e)
    elder.value = null
    contacts.value = []
    medications.value = []
  } finally {
    loading.value = false
    contactsLoading.value = false
  }
}

function medStatusText(status) {
  if (status === 'ACTIVE') return '启用'
  if (status === 'INACTIVE') return '停用'
  return status || '-'
}

function fmtDoseTimes(list) {
  if (!list?.length) return '-'
  return list.map((t) => String(t).slice(0, 5)).join(' / ')
}

async function loadMedications(id) {
  medicationLoading.value = true
  medicationError.value = ''
  medications.value = []
  try {
    const data = unwrap(await pageFamilyMedications({ elderId: id, page: 1, size: 50 }))
    medications.value = data?.records || []
  } catch (e) {
    medicationError.value = e?.message || '用药信息加载失败'
  } finally {
    medicationLoading.value = false
  }
}

function goBack() {
  router.push('/family/elders')
}

function onQuickEntry(kind) {
  if (kind === 'health' || kind === 'warning') {
    router.push('/family/health')
    return
  }
  if (kind === 'care') {
    router.push('/family/care-services')
    return
  }
  ElMessage.info('该功能将在下一阶段开放。')
}

watch(elderId, () => {
  loadDetail()
})

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div class="detail-page">
    <button type="button" class="detail-back" @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      返回我的老人
    </button>

    <el-skeleton v-if="loading" class="detail-skel" :rows="8" animated />

    <div v-else-if="forbidden || notFound || errorMessage" class="detail-error">
      <el-result
        :icon="forbidden ? 'warning' : notFound ? 'info' : 'error'"
        :title="forbidden ? '无权查看该老人档案' : notFound ? '老人档案不存在' : '加载失败'"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button type="primary" @click="goBack">返回我的老人</el-button>
          <el-button v-if="!forbidden && !notFound" @click="loadDetail">重试</el-button>
        </template>
      </el-result>
    </div>

    <template v-else-if="elder">
      <section class="hero-card">
        <div class="hero-card__left">
          <div class="hero-card__avatar">
            <el-icon :size="36"><component :is="avatarIcon" /></el-icon>
          </div>
          <div class="hero-card__meta">
            <h1>{{ elder.name }}</h1>
            <p>
              {{ gender }}
              <template v-if="age != null"> · {{ age }}岁</template>
              <template v-if="elder.careLevel"> · {{ elder.careLevel }}</template>
            </p>
            <p class="hero-card__phone">📞 {{ maskPhone(elder.phone) }}</p>
            <span class="hero-card__status" :class="statusOk ? 'is-ok' : 'is-off'">
              {{ statusLabel }}
            </span>
          </div>
        </div>
        <div class="hero-card__actions">
          <button type="button" class="quick-btn is-green" @click="onQuickEntry('health')">
            <el-icon><Monitor /></el-icon>
            健康记录
          </button>
          <button type="button" class="quick-btn is-orange" @click="onQuickEntry('warning')">
            <el-icon><Bell /></el-icon>
            健康预警
          </button>
          <button type="button" class="quick-btn is-purple" @click="onQuickEntry('care')">
            <el-icon><FirstAidKit /></el-icon>
            照护服务
          </button>
        </div>
      </section>

      <div class="detail-grid">
        <ElderProfile :elder="elder" />
        <div class="detail-side">
          <EmergencyContactCard :contacts="contacts" :loading="contactsLoading" />
          <FamilyRelationCard :elder-name="elder.name" />
        </div>
      </div>

      <section class="med-section">
        <h3>用药管理</h3>
        <p class="med-section__hint">仅展示该老人的用药计划，家属不可修改。</p>
        <el-skeleton v-if="medicationLoading" :rows="3" animated />
        <el-alert
          v-else-if="medicationError"
          type="error"
          :closable="false"
          show-icon
          :title="medicationError"
        />
        <el-table v-else :data="medications" stripe empty-text="暂无用药计划">
          <el-table-column prop="medicineName" label="药品名称" min-width="120" />
          <el-table-column label="剂量" width="100">
            <template #default="{ row }">{{ row.dosage }}{{ row.dosageUnit }}</template>
          </el-table-column>
          <el-table-column prop="usageMethod" label="服用方式" width="100" />
          <el-table-column label="服药时间" min-width="140">
            <template #default="{ row }">{{ fmtDoseTimes(row.doseTimes) }}</template>
          </el-table-column>
          <el-table-column prop="startDate" label="开始日期" width="120" />
          <el-table-column prop="endDate" label="结束日期" width="120" />
          <el-table-column label="当前状态" width="100">
            <template #default="{ row }">
              <span class="med-status" :class="row.status === 'ACTIVE' ? 'is-on' : 'is-off'">
                {{ medStatusText(row.status) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="quick-section">
        <h3>快捷服务</h3>
        <div class="quick-section__grid">
          <button type="button" class="service-card" @click="onQuickEntry('health')">
            <span class="service-card__icon is-green"><el-icon :size="20"><Monitor /></el-icon></span>
            <span class="service-card__title">健康记录</span>
            <span class="service-card__desc">后续阶段开放</span>
          </button>
          <button type="button" class="service-card" @click="onQuickEntry('warning')">
            <span class="service-card__icon is-orange"><el-icon :size="20"><Bell /></el-icon></span>
            <span class="service-card__title">健康预警</span>
            <span class="service-card__desc">后续阶段开放</span>
          </button>
          <button type="button" class="service-card" @click="onQuickEntry('care')">
            <span class="service-card__icon is-purple"><el-icon :size="20"><FirstAidKit /></el-icon></span>
            <span class="service-card__title">照护服务</span>
            <span class="service-card__desc">后续阶段开放</span>
          </button>
        </div>
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

.detail-back {
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

.detail-back:hover {
  color: #3a78a8;
}

.detail-skel,
.detail-error {
  background: rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 20px;
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px;
  border-radius: 16px;
  background: linear-gradient(120deg, rgba(234, 244, 251, 0.95), rgba(238, 247, 243, 0.9));
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
}

.hero-card__left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.hero-card__avatar {
  width: 78px;
  height: 78px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4a90c2;
  background: linear-gradient(135deg, #d7ebf8, #e8f4fc);
}

.hero-card__meta h1 {
  margin: 0;
  font-size: 22px;
  color: #2c4a5e;
}

.hero-card__meta p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.hero-card__phone {
  color: #4a6072 !important;
}

.hero-card__status {
  display: inline-flex;
  margin-top: 10px;
  padding: 3px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.hero-card__status.is-ok {
  background: rgba(105, 185, 140, 0.16);
  color: #4f9a72;
}

.hero-card__status.is-off {
  background: rgba(138, 160, 181, 0.18);
  color: #718096;
}

.hero-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  flex-shrink: 0;
}

.quick-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 38px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.78);
  color: #2c4a5e;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s ease;
}

.quick-btn:hover {
  transform: translateY(-2px);
}

.quick-btn.is-green {
  color: #4f9a72;
}
.quick-btn.is-orange {
  color: #c8782a;
}
.quick-btn.is-purple {
  color: #7a63b8;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.9fr);
  gap: 16px;
  align-items: start;
}

.detail-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.med-section {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 18px 20px;
}

.med-section h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #2c4a5e;
}

.med-section__hint {
  margin: 6px 0 12px;
  font-size: 12px;
  color: #8aa0b5;
}

.med-status {
  display: inline-flex;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.med-status.is-on {
  background: rgba(105, 185, 140, 0.16);
  color: #4f9a72;
}

.med-status.is-off {
  background: rgba(138, 160, 181, 0.18);
  color: #718096;
}

.quick-section {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 18px 20px;
}

.quick-section h3 {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: #2c4a5e;
}

.quick-section__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.service-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 16px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  background: linear-gradient(160deg, rgba(234, 244, 251, 0.65), rgba(255, 255, 255, 0.8));
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.service-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 28px rgba(74, 144, 194, 0.12);
}

.service-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.service-card__icon.is-green {
  background: linear-gradient(135deg, #69b98c, #8fcea8);
}
.service-card__icon.is-orange {
  background: linear-gradient(135deg, #f2a65a, #f6c28a);
}
.service-card__icon.is-purple {
  background: linear-gradient(135deg, #9b7ed9, #b9a0e8);
}

.service-card__title {
  font-size: 14px;
  font-weight: 700;
  color: #2c4a5e;
}

.service-card__desc {
  font-size: 12px;
  color: #8aa0b5;
}

@media (max-width: 960px) {
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .quick-section__grid {
    grid-template-columns: 1fr;
  }
}
</style>
