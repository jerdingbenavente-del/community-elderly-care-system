<script setup>
import { onMounted, ref } from 'vue'
import { toastIfNeeded } from '@/api/request'
import { staffTodayMenus } from '@/api/weeklyMenu'
import { mealLabel } from '@/utils/week'

const loading = ref(false)
const rows = ref([])

async function load() {
  loading.value = true
  try {
    const res = await staffTodayMenus()
    if (res?.code !== 200) throw new Error(res?.message || '加载失败')
    rows.value = res.data || []
  } catch (e) {
    toastIfNeeded(e, '加载今日膳食失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <header class="page-head">
      <h1>今日膳食</h1>
      <p>只读查看。有专属调整的老人不要按公共菜单供餐。</p>
    </header>
    <el-empty v-if="!loading && !rows.length" description="当前没有可查看的老人" />
    <section v-for="row in rows" :key="row.elderId" class="card">
      <h2>老人：{{ row.elderName }}</h2>
      <el-alert v-if="row.dietaryNote" type="warning" :closable="false" :title="`饮食备注：${row.dietaryNote}`" />
      <el-empty v-if="!row.items?.length" description="今天还没有公共菜单" />
      <div v-for="item in row.items" :key="item.mealType" class="meal">
        <strong>{{ mealLabel(item.mealType) }}</strong>
        <span v-if="item.adjusted">专属调整：{{ item.displayDishName }}（公共菜单 {{ item.publicDishName }}）</span>
        <span v-else>公共菜单：{{ item.displayDishName }}</span>
        <el-tag v-if="item.adjusted" type="success" size="small">已按老人饮食需求调整</el-tag>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-head h1 { margin: 0 0 6px; font-size: 22px; }
.page-head p { margin: 0 0 12px; color: #667085; }
.card { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.card h2 { margin: 0 0 10px; font-size: 18px; }
.meal { display: flex; gap: 10px; align-items: center; padding: 8px 0; border-top: 1px solid #eef2f6; }
</style>
