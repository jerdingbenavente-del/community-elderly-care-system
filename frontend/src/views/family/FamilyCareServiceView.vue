<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getCareServiceList } from '@/api/careService'
import { unwrap, serviceTypeLabel } from '@/utils/familyHome'
import { carePageBanner } from '@/config/familyImages'
import FamilyPageBanner from '@/components/family/FamilyPageBanner.vue'
import CareServiceCard from '@/components/family/care/CareServiceCard.vue'

const loading = ref(false)
const items = ref([])
const typeFilter = ref('ALL')

const types = computed(() => {
  const set = new Set()
  items.value.forEach((i) => {
    if (i.serviceType) set.add(i.serviceType)
  })
  return Array.from(set)
})

const filtered = computed(() => {
  if (typeFilter.value === 'ALL') return items.value
  return items.value.filter((i) => i.serviceType === typeFilter.value)
})

function scrollToList() {
  document.getElementById('care-service-list')?.scrollIntoView({ behavior: 'smooth' })
}

async function load() {
  loading.value = true
  try {
    const data = unwrap(await getCareServiceList())
    items.value = Array.isArray(data) ? data : []
  } catch (e) {
    items.value = []
    if (e.code !== 401 && e.code !== 403) {
      ElMessage.error(e.message || '加载服务项目失败')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<template>
  <div class="care-page">
    <header class="care-page__head">
      <div>
        <h1>照护服务</h1>
        <p>专业照护，温暖陪伴，让老人享受安心的晚年生活。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </header>

    <FamilyPageBanner :banner="carePageBanner" @action="scrollToList" />

    <section id="care-service-list" class="care-list-panel">
      <div class="care-list-panel__head">
        <div>
          <h3>服务项目</h3>
          <p>选择适合老人的照护服务</p>
        </div>
        <el-radio-group v-if="types.length" v-model="typeFilter" size="small">
          <el-radio-button value="ALL">全部</el-radio-button>
          <el-radio-button v-for="t in types" :key="t" :value="t">
            {{ serviceTypeLabel(t) }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <el-skeleton v-if="loading" :rows="6" animated />
      <div v-else-if="!filtered.length" class="care-empty">
        <el-empty :image-size="88">
          <template #description>
            <p class="empty-title">暂无可预约服务</p>
            <p class="empty-desc">当前暂时没有开放的照护服务，请稍后再试。</p>
          </template>
        </el-empty>
      </div>
      <div v-else class="care-grid">
        <CareServiceCard
          v-for="(item, index) in filtered"
          :key="item.id"
          :item="item"
          :tone-index="index"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.care-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.care-page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.care-page__head h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #2c4a5e;
}

.care-page__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.care-list-panel {
  background: rgba(255, 255, 255, 0.55);
  border-radius: 16px;
  padding: 4px 2px 8px;
}

.care-list-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
  padding: 0 4px;
}

.care-list-panel__head h3 {
  margin: 0;
  font-size: 16px;
  color: #2c4a5e;
}

.care-list-panel__head p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.care-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.care-empty {
  min-height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
}

.empty-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #4a6072;
}

.empty-desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: #8aa0b5;
}

@media (max-width: 1100px) {
  .care-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .care-grid {
    grid-template-columns: 1fr;
  }
}
</style>
