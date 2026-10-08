<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { toastIfNeeded } from '@/api/request'
import { createWeeklyMenu, deleteWeeklyMenu, getWeeklyMenu, updateWeeklyMenu } from '@/api/weeklyMenu'
import { MEAL_TYPES, formatDate, nextWeekMonday, shiftWeek, weekDays } from '@/utils/week'

const loading = ref(false)
const saving = ref(false)
const weekStart = ref(formatDate(nextWeekMonday()))
const menuId = ref(null)
const dishes = reactive({})

const days = computed(() => weekDays(weekStart.value))
const isSunday = computed(() => new Date().getDay() === 0)
const weekLabel = computed(() => {
  const list = days.value
  if (!list.length) return ''
  return `${list[0].date} 至 ${list[6].date}`
})

function dishKey(date, meal) {
  return `${date}|${meal}`
}

function resetDishes() {
  Object.keys(dishes).forEach((key) => {
    delete dishes[key]
  })
  days.value.forEach((day) => {
    MEAL_TYPES.forEach((meal) => {
      dishes[dishKey(day.date, meal.value)] = ''
    })
  })
}

async function loadMenu() {
  loading.value = true
  try {
    const res = await getWeeklyMenu(weekStart.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    resetDishes()
    const menu = res.data
    menuId.value = menu?.id || null
    ;(menu?.items || []).forEach((item) => {
      dishes[dishKey(item.menuDate, item.mealType)] = item.dishName || ''
    })
  } catch (e) {
    toastIfNeeded(e, '加载周菜单失败')
  } finally {
    loading.value = false
  }
}

function buildItems() {
  const items = []
  days.value.forEach((day) => {
    MEAL_TYPES.forEach((meal) => {
      items.push({
        menuDate: day.date,
        mealType: meal.value,
        dishName: (dishes[dishKey(day.date, meal.value)] || '').trim(),
      })
    })
  })
  return items
}

async function save() {
  const items = buildItems()
  if (items.some((item) => !item.dishName)) {
    ElMessage.warning('请填写整周每一餐的菜品')
    return
  }
  saving.value = true
  try {
    const res = menuId.value
      ? await updateWeeklyMenu(menuId.value, { items })
      : await createWeeklyMenu({ items })
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    ElMessage.success('菜单保存成功')
    await loadMenu()
  } catch (e) {
    toastIfNeeded(e, '保存失败')
  } finally {
    saving.value = false
  }
}

async function removeMenu() {
  if (!menuId.value) return
  try {
    await ElMessageBox.confirm('删除后本周公共菜单不再展示，历史已结束的周不会被删除。', '删除本周菜单', {
      type: 'warning',
    })
    const res = await deleteWeeklyMenu(menuId.value)
    if (res?.code !== 200) throw new Error(res?.message || '删除失败')
    ElMessage.success('已删除')
    await loadMenu()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    toastIfNeeded(e, '删除失败')
  }
}

function goNextWeek() {
  weekStart.value = formatDate(nextWeekMonday())
  loadMenu()
}

function moveWeek(delta) {
  weekStart.value = shiftWeek(weekStart.value, delta)
  loadMenu()
}

onMounted(loadMenu)
</script>

<template>
  <div class="admin-menu-page" v-loading="loading">
    <header class="page-head">
      <div>
        <h1>每周膳食菜单</h1>
        <p>公共菜单按周录入。建议周日录入下一周，该周结束前仍可修改。个性化调整不在这里改。</p>
      </div>
      <div class="actions">
        <el-button @click="goNextWeek">定位下一周</el-button>
        <el-button type="primary" :loading="saving" :disabled="saving" @click="save">保存菜单</el-button>
      </div>
    </header>
    <el-alert
      v-if="!isSunday"
      type="info"
      :closable="false"
      show-icon
      title="今天不是周日。可以补录尚未结束的周菜单，已结束的周不能再改。"
      class="hint"
    />
    <section class="panel">
      <div class="toolbar">
        <el-button @click="moveWeek(-1)">上一周</el-button>
        <strong>{{ weekLabel }}</strong>
        <el-button @click="moveWeek(1)">下一周</el-button>
        <el-tag v-if="menuId" type="success">已有公共菜单</el-tag>
        <el-tag v-else type="info">尚未录入</el-tag>
        <el-button v-if="menuId" type="danger" link @click="removeMenu">删除本周</el-button>
      </div>
      <div class="grid">
        <div v-for="day in days" :key="day.date" class="day-card">
          <h3>{{ day.label }} <span>{{ day.date }}</span></h3>
          <label v-for="meal in MEAL_TYPES" :key="meal.value">
            <span>{{ meal.label }}</span>
            <el-input v-model="dishes[dishKey(day.date, meal.value)]" :placeholder="`${meal.label}菜品`" />
          </label>
        </div>
      </div>
      <div class="save-bar">
        <span class="save-bar__tip">{{ menuId ? '已有公共菜单，修改后请保存。' : '填写完整 7 天 × 3 餐后保存。' }}</span>
        <el-button type="primary" size="large" :loading="saving" :disabled="saving" @click="save">保存菜单</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-head { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 12px; flex-wrap: wrap; }
.page-head h1 { margin: 0 0 6px; font-size: 22px; }
.page-head p { margin: 0; color: #667085; }
.actions { display: flex; gap: 8px; flex-shrink: 0; }
.hint { margin-bottom: 12px; }
.panel { background: #fff; border-radius: 12px; padding: 16px; }
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 12px; }
.day-card { border: 1px solid #e7ebf3; border-radius: 10px; padding: 12px; }
.day-card h3 { margin: 0 0 10px; font-size: 15px; }
.day-card h3 span { color: #98a2b3; font-weight: 400; font-size: 12px; }
.day-card label { display: block; margin-bottom: 8px; }
.day-card label span { display: block; margin-bottom: 4px; color: #475467; font-size: 13px; }
.save-bar {
  position: sticky;
  bottom: 0;
  margin-top: 16px;
  padding: 12px 0 4px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  background: linear-gradient(180deg, rgba(255,255,255,0) 0%, #fff 28%);
  border-top: 1px solid #eef2f6;
  z-index: 2;
}
.save-bar__tip { color: #667085; font-size: 13px; margin-right: auto; }
</style>
