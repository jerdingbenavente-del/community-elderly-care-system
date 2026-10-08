<script setup>
import { promoCards } from '@/config/familyImages'
</script>

<template>
  <aside class="promo-rail" aria-label="养老主题宣传">
    <article
      v-for="item in promoCards"
      :key="item.key"
      class="promo-card"
      :class="`promo-card--${item.tone}`"
    >
      <div
        class="promo-card__media"
        :style="{
          backgroundImage: `url(${item.image})`,
          backgroundPosition: item.objectPosition,
        }"
      />
    </article>
  </aside>
</template>

<style scoped>
.promo-rail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
}

.promo-card {
  position: relative;
  flex: 1;
  min-height: 160px;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 8px 20px rgba(47, 79, 111, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.65);
}

.promo-card__media {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-repeat: no-repeat;
}

.promo-card--warm {
  background: #fff4e8;
}
.promo-card--blue {
  background: #eaf4ff;
}
.promo-card--teal {
  background: #e8faf8;
}

@media (max-width: 1100px) {
  .promo-rail {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .promo-card {
    min-height: 220px;
  }
}

@media (max-width: 720px) {
  .promo-rail {
    grid-template-columns: 1fr;
  }

  .promo-card {
    min-height: 180px;
  }
}
</style>
