<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { toastIfNeeded } from '@/api/request'
import { adminDietaryNotes, cancelMealAdjustment, getElderMenu, saveMealAdjustment } from '@/api/weeklyMenu'
import { formatDate, mealLabel, nextWeekMonday, shiftWeek } from '@/utils/week'

const loading = ref(false)
const notes = ref([])
const elderId = ref(null)
const weekStart = ref(formatDate(nextWeekMonday()))
const view = ref(null)
const drafts = ref({})

const weekLabel = computed(() => {
  if (!view.value?.weekStartDate) return weekStart.value
  return `${view.value.weekStartDate} 至 ${view.value.weekEndDate}`
})

const grouped = computed(() => {
  const map = new Map()
  ;(view.value?.items || []).forEach((item) => {
    if (!map.has(item.menuDate)) map.set(item.menuDate, [])
    map.get(item.menuDate).push(item)
  })
  return [...map.entries()]
})

async function loadNotes() {
  const res = await adminDietaryNotes()
  if (res?.code !== 200) throw new Error(res?.message || '加载失败')
  notes.value = res.data || []
  if (!elderId.value && notes.value.length) {
    const withNote = notes.value.find((row) => row.note)
    elderId.value = (withNote || notes.value[0]).elderId
  }
}

async function loadView() {
  if (!elderId.value) {
    view.value = null
    return
  }
  loading.value = true
  try {
    const res = await getElderMenu(elderId.value, weekStart.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    view.value = res.data
    const next = {}
    ;(res.data?.items || []).forEach((item) => {
      next[item.id] = item.adjusted ? item.displayDishName : ''
    })
    drafts.value = next
  } catch (e) {
    toastIfNeeded(e, '加载老人菜单失败')
  } finally {
    loading.value = false
  }
}

async function reload() {
  loading.value = true
  try {
    await loadNotes()
    await loadView()
  } catch (e) {
    toastIfNeeded(e, '加载失败')
  } finally {
    loading.value = false
  }
}

async function saveItem(item) {
  const content = (drafts.value[item.id] || '').trim()
  if (!content) {
    ElMessage.warning('请填写该老人这一餐的替换菜品')
    return
  }
  try {
    const res = await saveMealAdjustment({
      elderId: elderId.value,
      menuDate: item.menuDate,
      mealType: item.mealType,
      adjustedContent: content,
      reason: view.value?.dietaryNote || '',
    })
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    ElMessage.success('已保存该老人的专属菜品')
    await loadView()
  } catch (e) {
    toastIfNeeded(e, '保存失败')
  }
}

async function cancelItem(item) {
  if (!item.adjustmentId) return
  try {
    const res = await cancelMealAdjustment(item.adjustmentId)
    if (res?.code !== 200) throw new Error(res?.message || '取消失败')
    ElMessage.success('已恢复公共菜单')
    await loadView()
  } catch (e) {
    toastIfNeeded(e, '取消失败')
  }
}

function moveWeek(delta) {
  weekStart.value = shiftWeek(weekStart.value, delta)
  loadView()
}

onMounted(reload)
</script>

<template>
  <div class="diet-page" v-loading="loading">
    <header class="page-head">
      <div>
        <h1>老人饮食管理</h1>
        <p>先看家属备注，再为单个老人调整某一餐。公共菜单不会被改动。</p>
      </div>
    </header>
    <section class="panel">
        <el-table :data="notes" stripe empty-text="暂无家属提交的饮食备注" height="280" @row-click="(row) => { elderId = row.elderId; loadView() }">
        <el-table-column prop="elderName" label="老人" width="120" />
        <el-table-column label="饮食备注" min-width="220">
          <template #default="{ row }">{{ row.note || '无特殊备注' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">{{ row.updatedAt || '-' }}</template>
        </el-table-column>
      </el-table>
    </section>
    <section class="panel">
      <div class="toolbar">
        <el-select v-model="elderId" filterable placeholder="选择老人" style="width: 180px" @change="loadView">
          <el-option v-for="row in notes" :key="row.elderId" :label="row.elderName" :value="row.elderId" />
        </el-select>
        <el-button @click="moveWeek(-1)">上一周</el-button>
        <strong>{{ weekLabel }}</strong>
        <el-button @click="moveWeek(1)">下一周</el-button>
      </div>
      <el-alert
        v-if="view?.dietaryNote"
        type="warning"
        :closable="false"
        show-icon
        :title="`饮食备注：${view.dietaryNote}`"
        class="hint"
      />
      <el-empty v-if="view && !view.items?.length" description="这一周还没有公共菜单" />
      <div v-for="[date, items] in grouped" :key="date" class="day">
        <h3>{{ date }}</h3>
        <div v-for="item in items" :key="item.id" class="meal">
          <div class="meal-head">
            <strong>{{ mealLabel(item.mealType) }}</strong>
            <span>公共菜单：{{ item.publicDishName }}</span>
            <el-tag v-if="item.adjusted" type="success" size="small">已按老人饮食需求调整</el-tag>
          </div>
          <div class="meal-edit">
            <el-input v-model="drafts[item.id]" placeholder="仅填写该老人的替换菜品" />
            <el-button type="primary" @click="saveItem(item)">保存调整</el-button>
            <el-button v-if="item.adjustmentId" @click="cancelItem(item)">取消调整</el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-head h1 { margin: 0 0 6px; font-size: 22px; }
.page-head p { margin: 0 0 12px; color: #667085; }
.panel { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.hint { margin-bottom: 12px; }
.day { margin-bottom: 16px; }
.day h3 { margin: 0 0 8px; font-size: 15px; }
.meal { border-top: 1px solid #eef2f6; padding: 10px 0; }
.meal-head { display: flex; gap: 10px; align-items: center; margin-bottom: 8px; }
.meal-edit { display: flex; gap: 8px; }
</style>
