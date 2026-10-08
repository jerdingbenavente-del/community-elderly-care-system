<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { pagePublicActivities } from '@/api/activity'
import { toastIfNeeded } from '@/api/request'

const loading = ref(false)
const rows = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  status: '',
  dateFrom: '',
  dateTo: '',
  page: 1,
  size: 10,
})

const upcoming = computed(() => rows.value.filter((r) => r.status === 'PUBLISHED'))
const history = computed(() => rows.value.filter((r) => r.status === 'COMPLETED' || r.status === 'CANCELLED'))

function statusMeta(s) {
  if (s === 'PUBLISHED') return { label: '已发布', type: 'success' }
  if (s === 'CANCELLED') return { label: '已取消', type: 'warning' }
  if (s === 'COMPLETED') return { label: '已结束', type: 'info' }
  return { label: s || '-', type: 'info' }
}

function fmtTime(v) {
  if (!v) return '-'
  return String(v).slice(0, 5)
}

async function loadList() {
  loading.value = true
  try {
    const res = await pagePublicActivities({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      dateFrom: query.dateFrom || undefined,
      dateTo: query.dateTo || undefined,
    })
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    rows.value = []
    total.value = 0
    toastIfNeeded(e, '加载活动失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <div class="family-activity-page" v-loading="loading">
    <header class="page-head">
      <h1>社区活动</h1>
      <p>查看已发布的社区活动（只读，不含报名）。</p>
    </header>

    <section class="panel filters">
      <el-input v-model="query.keyword" clearable placeholder="名称/地点" style="width: 180px" />
      <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px">
        <el-option label="全部" value="" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="已结束" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-date-picker v-model="query.dateFrom" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
      <el-date-picker v-model="query.dateTo" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
      <el-button type="primary" :icon="Search" @click="() => { query.page = 1; loadList() }">查询</el-button>
    </section>

    <el-empty v-if="!loading && !rows.length" description="暂无已发布活动" />

    <template v-else>
      <h2 v-if="!query.status || query.status === 'PUBLISHED'" class="section-title">即将举行</h2>
      <el-empty v-if="(!query.status || query.status === 'PUBLISHED') && !upcoming.length" description="暂无即将举行的活动" />
      <article v-for="row in upcoming" :key="'u-' + row.id" class="card">
        <div class="card-head">
          <h3>{{ row.activityName }}</h3>
          <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
        </div>
        <p>日期：{{ row.activityDate }}　时间：{{ fmtTime(row.startTime) }}～{{ fmtTime(row.endTime) }}</p>
        <p>地点：{{ row.location }}</p>
        <p v-if="row.description" class="desc">{{ row.description }}</p>
      </article>

      <h2 v-if="!query.status || query.status === 'COMPLETED' || query.status === 'CANCELLED'" class="section-title">历史活动</h2>
      <el-empty v-if="(!query.status || query.status === 'COMPLETED' || query.status === 'CANCELLED') && !history.length" description="暂无历史活动" />
      <article v-for="row in history" :key="'h-' + row.id" class="card muted">
        <div class="card-head">
          <h3>{{ row.activityName }}</h3>
          <el-tag :type="statusMeta(row.status).type" size="small">{{ statusMeta(row.status).label }}</el-tag>
        </div>
        <p>日期：{{ row.activityDate }}　时间：{{ fmtTime(row.startTime) }}～{{ fmtTime(row.endTime) }}</p>
        <p>地点：{{ row.location }}</p>
        <p v-if="row.description" class="desc">{{ row.description }}</p>
      </article>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadList"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.family-activity-page { max-width: 880px; }
.page-head h1 { margin: 0 0 6px; font-size: 22px; }
.page-head p { margin: 0 0 14px; color: #667085; }
.filters { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; background: #fff; padding: 14px; border-radius: 12px; }
.section-title { margin: 18px 0 10px; font-size: 16px; }
.card { background: #fff; border-radius: 12px; padding: 14px 16px; margin-bottom: 10px; }
.card.muted { opacity: 0.92; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.card h3 { margin: 0; font-size: 17px; }
.card p { margin: 8px 0 0; color: #475467; }
.desc { color: #667085 !important; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
