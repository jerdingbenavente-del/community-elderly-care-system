<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

defineProps({
  elderName: { type: String, default: '' },
})

const userStore = useUserStore()

const familyName = computed(
  () => userStore.userInfo?.realName || userStore.displayName || '-',
)
const username = computed(() => userStore.userInfo?.username || '-')
</script>

<template>
  <section class="relation-card">
    <h3 class="relation-card__title">家属信息</h3>
    <p class="relation-card__hint">
      仅展示当前登录家属与老人的绑定概况。其他家属信息不会展示。
    </p>
    <dl class="relation-list">
      <div class="relation-item">
        <dt>家属姓名</dt>
        <dd>{{ familyName }}</dd>
      </div>
      <div class="relation-item">
        <dt>登录账号</dt>
        <dd>{{ username }}</dd>
      </div>
      <div class="relation-item">
        <dt>绑定老人</dt>
        <dd>{{ elderName || '-' }}</dd>
      </div>
      <div class="relation-item">
        <dt>绑定状态</dt>
        <dd><span class="tag-ok">已绑定</span></dd>
      </div>
      <div class="relation-item relation-item--full">
        <dt>与老人关系</dt>
        <dd class="muted">
          家属端开放接口未返回亲属关系字段；具体关系由中心管理员在绑定档案中维护。
        </dd>
      </div>
    </dl>
  </section>
</template>

<style scoped>
.relation-card {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  box-shadow: 0 8px 25px rgba(80, 120, 150, 0.08);
  padding: 18px 20px;
}

.relation-card__title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: #2c4a5e;
}

.relation-card__hint {
  margin: 0 0 14px;
  font-size: 12px;
  color: #8aa0b5;
  line-height: 1.5;
}

.relation-list {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.relation-item {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(238, 247, 243, 0.55);
}

.relation-item--full {
  grid-column: 1 / -1;
}

.relation-item dt {
  font-size: 12px;
  color: #8aa0b5;
  margin-bottom: 4px;
}

.relation-item dd {
  margin: 0;
  font-size: 14px;
  color: #2c4a5e;
}

.relation-item dd.muted {
  font-size: 13px;
  color: #718096;
  line-height: 1.5;
}

.tag-ok {
  display: inline-flex;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(105, 185, 140, 0.16);
  color: #4f9a72;
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 720px) {
  .relation-list {
    grid-template-columns: 1fr;
  }
}
</style>
