<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getElderList } from '@/api/elder'
import { unwrap } from '@/utils/familyHome'
import { eldersPageBanner } from '@/config/familyImages'
import ElderCard from '@/components/family/ElderCard.vue'
import FamilyPageBanner from '@/components/family/FamilyPageBanner.vue'

const TONES = ['blue', 'green', 'orange']

const loading = ref(false)
const loadError = ref('')
const elders = ref([])

async function loadList() {
  loading.value = true
  loadError.value = ''
  try {
    const data = unwrap(await getElderList())
    elders.value = Array.isArray(data) ? data : []
  } catch (e) {
    loadError.value = e.message || '加载失败'
    elders.value = []
    if (e.code !== 401 && e.code !== 403) {
      ElMessage.error(loadError.value)
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<template>
  <div class="elders-page">
    <header class="elders-page__head">
      <div>
        <h1 class="elders-page__title">我的老人</h1>
        <p class="elders-page__sub">关注老人近况，守护健康生活</p>
      </div>
      <el-button @click="loadList" :loading="loading">刷新</el-button>
    </header>

    <FamilyPageBanner :banner="eldersPageBanner" />

    <el-skeleton v-if="loading" class="elders-skel" :rows="5" animated />

    <el-alert
      v-else-if="loadError"
      type="error"
      :title="loadError"
      show-icon
      :closable="false"
      class="elders-alert"
    />

    <div v-else-if="!elders.length" class="elders-empty">
      <el-empty :image-size="96">
        <template #description>
          <p class="elders-empty__title">暂无绑定老人</p>
          <p class="elders-empty__desc">
            您当前还没有绑定老人档案，<br />如需帮助请联系管理员。
          </p>
        </template>
      </el-empty>
    </div>

    <div v-else class="elders-grid">
      <ElderCard
        v-for="(elder, index) in elders"
        :key="elder.id"
        :elder="elder"
        :tone="TONES[index % TONES.length]"
      />
    </div>
  </div>
</template>

<style scoped>
.elders-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.elders-page__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.elders-page__title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #2c4a5e;
}

.elders-page__sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: #718096;
}

.elders-skel,
.elders-alert {
  background: rgba(255, 255, 255, 0.72);
  border-radius: 16px;
  padding: 16px;
}

.elders-empty {
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
}

.elders-empty__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #4a6072;
}

.elders-empty__desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #8aa0b5;
}

.elders-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1100px) {
  .elders-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .elders-grid {
    grid-template-columns: 1fr;
  }
}
</style>
