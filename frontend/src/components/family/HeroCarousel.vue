<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { carouselSlides } from '@/config/familyImages'

/**
 * 三张 banner 均为 1536×1024 = 3:2
 * 高度按宽度推算（上限内尽量贴近原图比例），图片用 cover 铺满去留白
 */
const BANNER_RATIO = 1536 / 1024
const MIN_HEIGHT = 280
const MAX_HEIGHT = 440

const router = useRouter()
const rootRef = ref(null)
const carouselHeightPx = ref(340)

const carouselHeight = computed(() => `${carouselHeightPx.value}px`)

function syncHeight() {
  const el = rootRef.value
  if (!el) return
  const w = el.clientWidth || 0
  if (!w) return
  const h = Math.round(w / BANNER_RATIO)
  carouselHeightPx.value = Math.min(MAX_HEIGHT, Math.max(MIN_HEIGHT, h))
}

function go(path) {
  router.push(path)
}

let ro
onMounted(() => {
  syncHeight()
  if (typeof ResizeObserver !== 'undefined' && rootRef.value) {
    ro = new ResizeObserver(() => syncHeight())
    ro.observe(rootRef.value)
  } else {
    window.addEventListener('resize', syncHeight)
  }
})

onUnmounted(() => {
  if (ro) ro.disconnect()
  else window.removeEventListener('resize', syncHeight)
})
</script>

<template>
  <div ref="rootRef" class="hero-carousel-wrap">
    <el-carousel
      class="hero-carousel"
      :height="carouselHeight"
      :interval="4000"
      :autoplay="true"
      :loop="true"
      :pause-on-hover="true"
      arrow="hover"
      indicator-position="outside"
    >
      <el-carousel-item v-for="item in carouselSlides" :key="item.key">
        <div class="hero-slide" :class="`hero-slide--${item.textAlign}`">
          <img
            class="hero-slide__img"
            :src="item.image"
            :alt="`${item.title} ${item.subtitle}`"
            :style="{ objectPosition: item.objectPosition || 'center center' }"
          />
          <div class="hero-slide__mask" aria-hidden="true" />
          <div class="hero-slide__content">
            <h2 class="hero-slide__title">
              {{ item.title }}
              <span>{{ item.subtitle }}</span>
            </h2>
            <p class="hero-slide__desc">{{ item.desc }}</p>
            <button type="button" class="hero-slide__btn" @click="go(item.path)">
              {{ item.action }}
            </button>
          </div>
        </div>
      </el-carousel-item>
    </el-carousel>
  </div>
</template>

<style scoped>
.hero-carousel-wrap {
  width: 100%;
  padding: 0;
  margin: 0;
}

.hero-carousel {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--ec-shadow);
  padding: 0;
  margin: 0;
  background: transparent;
}

.hero-carousel :deep(.el-carousel__container) {
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  padding: 0;
  margin: 0;
}

.hero-carousel :deep(.el-carousel__item) {
  width: 100%;
  height: 100%;
  padding: 0;
  margin: 0;
  overflow: hidden;
}

.hero-carousel :deep(.el-carousel__indicators--outside) {
  margin-top: 10px;
}

.hero-carousel :deep(.el-carousel__button) {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4a90c2;
  opacity: 0.35;
}

.hero-carousel :deep(.is-active .el-carousel__button) {
  width: 16px;
  border-radius: 8px;
  opacity: 1;
}

.hero-slide {
  position: relative;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
  background: transparent;
}

.hero-slide__img {
  display: block;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  border: 0;
  object-fit: cover;
  object-position: center center;
}

.hero-slide__mask {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.hero-slide--left .hero-slide__mask {
  background: linear-gradient(
    90deg,
    rgba(30, 70, 90, 0.45) 0%,
    rgba(30, 70, 90, 0.14) 40%,
    rgba(30, 70, 90, 0) 70%
  );
}

.hero-slide--right .hero-slide__mask {
  background: linear-gradient(
    270deg,
    rgba(30, 70, 90, 0.45) 0%,
    rgba(30, 70, 90, 0.14) 40%,
    rgba(30, 70, 90, 0) 70%
  );
}

.hero-slide__content {
  position: absolute;
  z-index: 2;
  max-width: 420px;
  color: #fff;
  padding: 28px 32px;
  pointer-events: none;
}

.hero-slide__content .hero-slide__btn {
  pointer-events: auto;
}

.hero-slide--left .hero-slide__content {
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  text-align: left;
}

.hero-slide--right .hero-slide__content {
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  text-align: right;
}

.hero-slide__title {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.35;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.22);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hero-slide--right .hero-slide__title {
  align-items: flex-end;
}

.hero-slide__title span {
  font-size: 22px;
  font-weight: 600;
}

.hero-slide__desc {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.7;
  opacity: 0.95;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.18);
}

.hero-slide__btn {
  border: 1px solid rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.9);
  color: #2c4a5e;
  border-radius: 999px;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.hero-slide__btn:hover {
  background: #fff;
}

@media (max-width: 768px) {
  .hero-slide__content {
    padding: 18px 16px;
    max-width: 88%;
  }

  .hero-slide__title {
    font-size: 20px;
  }

  .hero-slide__title span {
    font-size: 17px;
  }

  .hero-slide__desc {
    font-size: 12px;
    margin-bottom: 12px;
  }
}
</style>
