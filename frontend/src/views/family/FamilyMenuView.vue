<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { toastIfNeeded } from '@/api/request'
import { listMyElders } from '@/api/elder'
import { getElderMenu, saveDietaryNote } from '@/api/weeklyMenu'
import { formatDate, mealLabel, mondayOf, shiftWeek } from '@/utils/week'

const loading = ref(false)
const saving = ref(false)
const elders = ref([])
const elderId = ref(null)
const weekStart = ref(formatDate(mondayOf(new Date())))
const view = ref(null)
const noteDialog = ref(false)
const noteText = ref('')

const thisWeek = formatDate(mondayOf(new Date()))
const nextWeek = shiftWeek(thisWeek, 1)

const grouped = computed(() => {
  const map = new Map()
  ;(view.value?.items || []).forEach((item) => {
    if (!map.has(item.menuDate)) map.set(item.menuDate, [])
    map.get(item.menuDate).push(item)
  })
  return [...map.entries()]
})

async function loadElders() {
  const res = await listMyElders()
  if (res?.code !== 200) throw new Error(res?.message || '加载老人失败')
  elders.value = res.data || []
  if (!elderId.value && elders.value.length) elderId.value = elders.value[0].id
}

async function loadMenu() {
  if (!elderId.value) return
  loading.value = true
  try {
    const res = await getElderMenu(elderId.value, weekStart.value)
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    view.value = res.data
  } catch (e) {
    toastIfNeeded(e, '加载菜单失败')
  } finally {
    loading.value = false
  }
}

function openNote() {
  noteText.value = view.value?.dietaryNote || ''
  noteDialog.value = true
}

async function submitNote() {
  saving.value = true
  try {
    const res = await saveDietaryNote({ elderId: elderId.value, note: noteText.value })
    if (res?.code !== 200) throw new Error(res?.message || '保存失败')
    ElMessage.success(noteText.value.trim() ? '饮食备注已保存' : '未填写内容，不保留空备注')
    noteDialog.value = false
    await loadMenu()
  } catch (e) {
    toastIfNeeded(e, '保存失败')
  } finally {
    saving.value = false
  }
}

function pickWeek(start) {
  weekStart.value = start
  loadMenu()
}

onMounted(async () => {
  weekStart.value = nextWeek
  try {
    await loadElders()
    await loadMenu()
  } catch (e) {
    toastIfNeeded(e, '加载失败')
  }
})
</script>

<template>
  <div class="family-menu" v-loading="loading">
    <header class="page-head">
      <div>
        <h1>膳食菜单</h1>
        <p>查看养老中心的每周菜单。没有特殊饮食要求时，不用填写任何内容。</p>
      </div>
      <el-button type="primary" :disabled="!elderId" @click="openNote">
        {{ view?.dietaryNote ? '修改饮食备注' : '添加饮食备注' }}
      </el-button>
    </header>
    <div class="toolbar">
      <el-select v-model="elderId" placeholder="选择老人" style="width: 180px" @change="loadMenu">
        <el-option v-for="elder in elders" :key="elder.id" :label="elder.name" :value="elder.id" />
      </el-select>
      <el-button :type="weekStart === thisWeek ? 'primary' : 'default'" @click="pickWeek(thisWeek)">本周</el-button>
      <el-button :type="weekStart === nextWeek ? 'primary' : 'default'" @click="pickWeek(nextWeek)">下一周</el-button>
      <el-button @click="pickWeek(shiftWeek(weekStart, -1))">上一周</el-button>
    </div>
    <el-alert
      v-if="view?.dietaryNote"
      type="warning"
      :closable="false"
      show-icon
      :title="`饮食备注：${view.dietaryNote}`"
      class="hint"
    />
    <el-empty v-if="!elders.length" description="还没有绑定老人" />
    <el-empty v-else-if="view && !view.items?.length" description="这一周还没有菜单" />
    <section v-for="[date, items] in grouped" :key="date" class="day">
      <h3>{{ date }}</h3>
      <div v-for="item in items" :key="`${item.menuDate}-${item.mealType}`" class="meal">
        <strong>{{ mealLabel(item.mealType) }}</strong>
        <div>
          <div v-if="item.adjusted" class="public">原公共菜单：{{ item.publicDishName }}</div>
          <div>当前菜单：{{ item.displayDishName }}</div>
        </div>
        <el-tag v-if="item.adjusted" type="success" size="small">已按老人饮食需求调整</el-tag>
      </div>
    </section>
    <el-dialog v-model="noteDialog" title="饮食备注" width="460px">
      <p class="dialog-tip">只填写不能吃或不适合吃的内容。替代菜品由管理员安排，这里不能直接换菜。</p>
      <el-input v-model="noteText" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="例如：海鲜过敏，不能吃辣。没有特殊要求可留空。" />
      <template #footer>
        <el-button @click="noteDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitNote">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.page-head h1 { margin: 0 0 6px; font-size: 22px; }
.page-head p { margin: 0; color: #667085; }
.toolbar { display: flex; gap: 8px; align-items: center; margin: 16px 0; }
.hint { margin-bottom: 12px; }
.day { background: #fff; border-radius: 12px; padding: 12px 16px; margin-bottom: 12px; }
.day h3 { margin: 0 0 8px; }
.meal { display: flex; gap: 12px; align-items: center; padding: 8px 0; border-top: 1px solid #eef2f6; }
.public { color: #98a2b3; font-size: 13px; }
.dialog-tip { margin-top: 0; color: #667085; }
</style>
