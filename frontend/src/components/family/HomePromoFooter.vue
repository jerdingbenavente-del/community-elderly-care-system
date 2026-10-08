<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goServices() {
  router.push('/family/care-services')
}
</script>

<template>
  <section class="promo">
    <div class="promo__content">
      <p class="promo__eyebrow">智慧养老 · 温暖守护</p>
      <h3 class="promo__title">让每一位家人，都被温柔地看见</h3>
      <p class="promo__desc">
        健康监测、照护预约、服务进度与预警提醒，一站式陪伴家庭日常。
      </p>
      <el-button type="primary" round class="promo__btn" @click="goServices">
        立即预约照护服务
      </el-button>
    </div>
  </section>
</template>

<style scoped>
.promo {
  border-radius: 20px;
  padding: 36px 40px;
  background:
    radial-gradient(circle at 85% 20%, rgba(255, 255, 255, 0.28), transparent 40%),
    linear-gradient(120deg, #409eff 0%, #66b1ff 42%, #67c23a 100%);
  color: #fff;
  box-shadow: 0 12px 28px rgba(64, 158, 255, 0.22);
}

.promo__eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  opacity: 0.9;
}

.promo__title {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 700;
}

.promo__desc {
  margin: 0 0 20px;
  max-width: 560px;
  font-size: 14px;
  line-height: 1.7;
  opacity: 0.95;
}

.promo__btn {
  --el-button-bg-color: #fff;
  --el-button-border-color: #fff;
  --el-button-text-color: #337ecc;
  --el-button-hover-bg-color: #f5faff;
  --el-button-hover-border-color: #f5faff;
  --el-button-hover-text-color: #337ecc;
  font-weight: 600;
}

@media (max-width: 640px) {
  .promo {
    padding: 28px 20px;
  }

  .promo__title {
    font-size: 20px;
  }
}
</style>
