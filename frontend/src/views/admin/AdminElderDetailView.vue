<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getElder,
  listEmergencyContacts,
} from '@/api/adminElder'
import { listElderFamilies } from '@/api/elderFamily'
import { calcAge, genderText } from '@/utils/familyHome'
import { elderStatusText, maskIdCard, maskPhone } from '@/utils/sensitive'
import ElderFormDrawer from '@/components/admin/ElderFormDrawer.vue'
import EmergencyContactDrawer from '@/components/admin/EmergencyContactDrawer.vue'
import BindFamilyDrawer from '@/components/admin/BindFamilyDrawer.vue'

const route = useRoute()
const router = useRouter()

const elderId = computed(() => {
  const n = Number(route.params.id)
  return Number.isFinite(n) ? n : null
})

const activeTab = ref('basic')
const loading = ref(false)
const loadError = ref('')
const elder = ref(null)

const contactsLoading = ref(false)
const contacts = ref([])
const contactDrawerVisible = ref(false)
const editingContact = ref(null)

const familiesLoading = ref(false)
const families = ref([])
const bindVisible = ref(false)

const editVisible = ref(false)

const ageText = computed(() => {
  const a = calcAge(elder.value?.birthDate)
  return a == null ? '-' : `${a} 岁`
})

async function loadElder() {
  if (!elderId.value) {
    loadError.value = '无效的老人 ID'
    elder.value = null
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const res = await getElder(elderId.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    elder.value = res.data
  } catch (e) {
    elder.value = null
    loadError.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadContacts() {
  if (!elderId.value) return
  contactsLoading.value = true
  try {
    const res = await listEmergencyContacts(elderId.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载联系人失败')
    contacts.value = res.data || []
  } catch (e) {
    contacts.value = []
    ElMessage.error(e.message || '加载联系人失败')
  } finally {
    contactsLoading.value = false
  }
}

async function loadFamilies() {
  if (!elderId.value) return
  familiesLoading.value = true
  try {
    const res = await listElderFamilies({ elderId: elderId.value })
    if (res?.code !== 200) throw new Error(res?.message || '加载家属绑定失败')
    families.value = res.data || []
  } catch (e) {
    families.value = []
    ElMessage.error(e.message || '加载家属绑定失败')
  } finally {
    familiesLoading.value = false
  }
}

function reloadAll() {
  loadElder()
  loadContacts()
  loadFamilies()
}

function goBack() {
  router.push('/admin/elders')
}

function openEdit() {
  editVisible.value = true
}

function openAddContact() {
  editingContact.value = null
  contactDrawerVisible.value = true
}

function openEditContact(row) {
  editingContact.value = row
  contactDrawerVisible.value = true
}

function onContactSaved() {
  loadContacts()
  // 详情 VO 也可能带 contacts，刷新档案
  loadElder()
}

watch(
  () => route.params.id,
  () => {
    activeTab.value = 'basic'
    reloadAll()
  },
)

onMounted(reloadAll)
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <div class="page-head">
      <div class="page-head__left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回列表</el-button>
        <div>
          <h2>{{ elder?.name || '老人详情' }}</h2>
          <p v-if="elder">
            {{ genderText(elder.gender) }} · {{ ageText }} ·
            <el-tag :type="elder.status === 1 ? 'success' : 'info'" size="small">
              {{ elderStatusText(elder.status) }}
            </el-tag>
          </p>
        </div>
      </div>
      <el-button v-if="elder" type="primary" @click="openEdit">编辑档案</el-button>
    </div>

    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      :title="loadError"
      class="err"
    >
      <template #default>
        <el-button type="primary" link @click="loadElder">重新加载</el-button>
      </template>
    </el-alert>

    <div v-if="elder" class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本资料" name="basic">
          <div class="info-grid">
            <div class="info-item"><span>姓名</span><strong>{{ elder.name }}</strong></div>
            <div class="info-item"><span>性别</span>{{ genderText(elder.gender) }}</div>
            <div class="info-item"><span>出生日期</span>{{ elder.birthDate || '-' }}</div>
            <div class="info-item"><span>年龄</span>{{ ageText }}</div>
            <div class="info-item"><span>手机</span>{{ maskPhone(elder.phone) }}</div>
            <div class="info-item"><span>身份证</span>{{ maskIdCard(elder.idCard) }}</div>
            <div class="info-item"><span>护理等级</span>{{ elder.careLevel || '-' }}</div>
            <div class="info-item"><span>建档日期</span>{{ elder.registeredAt || '-' }}</div>
            <div class="info-item wide"><span>住址</span>{{ elder.address || '-' }}</div>
            <div class="info-item wide"><span>病史</span>{{ elder.medicalHistory || '-' }}</div>
            <div class="info-item wide"><span>过敏史</span>{{ elder.allergyHistory || '-' }}</div>
            <div class="info-item wide"><span>特殊照护</span>{{ elder.specialCareRequirement || '-' }}</div>
            <div class="info-item wide"><span>备注</span>{{ elder.remark || '-' }}</div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="紧急联系人" name="contacts">
          <div class="tab-toolbar">
            <el-button type="primary" :icon="Plus" @click="openAddContact">新增联系人</el-button>
          </div>
          <el-table
            v-loading="contactsLoading"
            :data="contacts"
            stripe
            empty-text="暂无紧急联系人"
          >
            <el-table-column prop="name" label="姓名" min-width="100" />
            <el-table-column prop="relationship" label="关系" min-width="90">
              <template #default="{ row }">{{ row.relationship || '-' }}</template>
            </el-table-column>
            <el-table-column label="手机" min-width="120">
              <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="90" />
            <el-table-column prop="remark" label="备注" min-width="140">
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEditContact(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="家属信息" name="families">
          <div class="tab-toolbar">
            <el-button type="primary" :icon="Plus" @click="bindVisible = true">绑定家属</el-button>
          </div>
          <el-table
            v-loading="familiesLoading"
            :data="families"
            stripe
            empty-text="暂无绑定家属"
          >
            <el-table-column prop="familyUsername" label="用户名" min-width="130" />
            <el-table-column prop="familyRealName" label="姓名" min-width="100">
              <template #default="{ row }">{{ row.familyRealName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="relationship" label="关系" min-width="90">
              <template #default="{ row }">{{ row.relationship || '-' }}</template>
            </el-table-column>
            <el-table-column label="主联系人" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.isPrimary === 1" type="success" size="small">是</el-tag>
                <span v-else>否</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="绑定时间" width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.createdAt || '-' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <ElderFormDrawer
      v-if="elderId"
      v-model="editVisible"
      :elder-id="elderId"
      @success="loadElder"
    />
    <EmergencyContactDrawer
      v-if="elderId"
      v-model="contactDrawerVisible"
      :elder-id="elderId"
      :contact="editingContact"
      @success="onContactSaved"
    />
    <BindFamilyDrawer
      v-if="elderId"
      v-model="bindVisible"
      :elder-id="elderId"
      @success="loadFamilies"
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

.tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

@media (max-width: 720px) {
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
