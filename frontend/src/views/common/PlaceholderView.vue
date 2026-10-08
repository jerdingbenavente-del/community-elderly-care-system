<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta?.title || '功能占位')
</script>

<template>
  <el-card class="page-card" shadow="never">
    <h2 class="page-title">{{ title }}</h2>
    <p class="page-subtitle">该功能将在下一阶段完成</p>
    <p class="placeholder-tip">当前仅为家属端菜单入口占位，路径：{{ route.path }}</p>
  </el-card>
</template>
