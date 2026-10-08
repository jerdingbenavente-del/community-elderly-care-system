<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { pageElders } from '@/api/adminElder'
import { calcAge, genderText } from '@/utils/familyHome'
import { elderStatusText, maskIdCard, maskPhone } from '@/utils/sensitive'
import ElderFormDrawer from '@/components/admin/ElderFormDrawer.vue'

const router = useRouter()

const loading = ref(false)
const loadError = ref('')
const rows = ref([])
const total = ref(0)

const query = reactive({
  name: '',
  phone: '',
  status: '',
  page: 1,
  size: 10,
})

const formVisible = ref(false)
const editId = ref(null)

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '档案正常', value: 1 },
  { label: '已停用', value: 0 },
]

async function loadList() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await pageElders({
      page: query.page,
      size: query.size,
      name: query.name.trim() || undefined,
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
  query.page = 1
  loadList()
}

function onReset() {
  query.name = ''
  query.phone = ''
  query.status = ''
  query.page = 1
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
  router.push(`/admin/elders/${row.id}`)
}

function ageText(birthDate) {
  const a = calcAge(birthDate)
  return a == null ? '-' : `${a} 岁`
}

onMounted(loadList)
</script>

<template>
  <div class="elders-page" v-loading="loading">
    <div class="page-head">
      <div>
        <h2>老人管理</h2>
        <p>维护在册老人档案；支持查询、新增、编辑与查看详情（紧急联系人、家属绑定）。</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增老人</el-button>
    </div>

    <div class="filter-card">
      <el-form :inline="true" @submit.prevent="onSearch">
        <el-form-item label="姓名">
          <el-input v-model="query.name" clearable placeholder="模糊搜索" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="query.phone" clearable placeholder="模糊搜索" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" style="width: 130px">
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
      <el-table :data="rows" stripe empty-text="暂无老人档案">
        <el-table-column prop="name" label="姓名" min-width="100" />
        <el-table-column label="性别" width="70">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column label="年龄" width="80">
          <template #default="{ row }">{{ ageText(row.birthDate) }}</template>
        </el-table-column>
        <el-table-column label="手机" min-width="120">
          <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
        </el-table-column>
        <el-table-column label="身份证" min-width="160">
          <template #default="{ row }">{{ maskIdCard(row.idCard) }}</template>
        </el-table-column>
        <el-table-column prop="careLevel" label="护理等级" min-width="100">
          <template #default="{ row }">{{ row.careLevel || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ elderStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <ElderFormDrawer v-model="formVisible" :elder-id="editId" @success="loadList" />
  </div>
</template>

<style scoped>
.elders-page {
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

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
