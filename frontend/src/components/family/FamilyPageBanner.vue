<script setup>
/**
 * 家属端子页顶部 Banner（不含首页轮播）
 * 按 banner 配置区分：全宽铺满 / 收窄居中 / 自然高度等
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'

const props = defineProps({
  banner: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['action'])

const DEFAULT_RATIO = 1.5
const DEFAULT_MIN = 200
const DEFAULT_MAX = 280

const rootRef = ref(null)
const heightPx = ref(220)

const objectFit = computed(() => props.banner.objectFit || 'cover')
const heightMode = computed(() => props.banner.heightMode || 'ratio')
const isNatural = computed(() => heightMode.value === 'auto' || objectFit.value === 'natural')
const bannerRatio = computed(() => Number(props.banner.bannerRatio) || DEFAULT_RATIO)
const minHeight = computed(() => Number(props.banner.minHeight) || DEFAULT_MIN)
const maxHeight = computed(() => Number(props.banner.maxHeight) || DEFAULT_MAX)
const showMask = computed(() => props.banner.showMask !== false)
const layoutWidth = computed(() => props.banner.layoutWidth || '100%')
const isCompact = computed(() => layoutWidth.value !== '100%')
const naturalMaxHeight = computed(() => {
  const n = Number(props.banner.naturalMaxHeight)
  return Number.isFinite(n) && n > 0 ? `${n}px` : '560px'
})

const heightStyle = computed(() => {
  if (isNatural.value) return 'auto'
  return `${heightPx.value}px`
})

const rootStyle = computed(() => {
  const style = {
    height: heightStyle.value,
    width: layoutWidth.value,
  }
  if (isNatural.value) {
    style['--banner-natural-max'] = naturalMaxHeight.value
  }
  return style
})

const imgObjectFit = computed(() => {
  if (isNatural.value) return props.banner.objectFit || 'contain'
  return objectFit.value
})

const textAlign = computed(() => props.banner.textAlign || 'left')
const bakedText = computed(() => Boolean(props.banner.bakedText))
const captionBottom = computed(() => props.banner.caption === 'bottom')
const softFill = computed(() => Boolean(props.banner.softFill))
const showTitles = computed(() => !bakedText.value && (props.banner.title || props.banner.subtitle))
const showDesc = computed(() => !bakedText.value && props.banner.desc)
const showAction = computed(() => Boolean(props.banner.action) && !captionBottom.value)
const showCaption = computed(() => showTitles.value || showDesc.value || showAction.value)
/** 侧向遮罩：底部文案模式用底部渐变，不再叠加侧向 mask */
const showSideMask = computed(() => showMask.value && !captionBottom.value)

function syncHeight() {
  if (isNatural.value) return
  const el = rootRef.value
  if (!el) return
  const w = el.clientWidth || 0
  if (!w) return
  heightPx.value = Math.min(
    maxHeight.value,
    Math.max(minHeight.value, Math.round(w / bannerRatio.value)),
  )
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
  <section
    ref="rootRef"
    class="fam-page-banner"
    :class="[
      `fam-page-banner--${textAlign}`,
      {
        'fam-page-banner--natural': isNatural,
        'fam-page-banner--compact': isCompact,
        'fam-page-banner--caption-bottom': captionBottom,
        'fam-page-banner--soft-fill': softFill,
      },
    ]"
    :style="rootStyle"
  >
    <!-- 未铺满区域：与标准页 fit 画布同思路的柔和模糊扩展（仅 softFill 页启用） -->
    <div
      v-if="softFill"
      class="fam-page-banner__soft-fill"
      aria-hidden="true"
      :style="{ backgroundImage: `url(${banner.image})` }"
    />
    <img
      class="fam-page-banner__img"
      :src="banner.image"
      :alt="banner.title || '页面 Banner'"
      :style="{
        objectFit: imgObjectFit,
        objectPosition: banner.objectPosition || 'center center',
      }"
    />
    <div v-if="showSideMask" class="fam-page-banner__mask" aria-hidden="true" />
    <div
      v-if="captionBottom && showCaption"
      class="fam-page-banner__bottom-shade"
      aria-hidden="true"
    />
    <div
      v-if="showCaption"
      class="fam-page-banner__text"
      :class="{ 'fam-page-banner__text--bottom': captionBottom }"
    >
      <template v-if="showTitles">
        <h2 v-if="banner.title">{{ banner.title }}</h2>
        <p v-if="banner.subtitle" class="sub">{{ banner.subtitle }}</p>
      </template>
      <p v-if="showDesc" class="desc">{{ banner.desc }}</p>
      <button
        v-if="showAction"
        type="button"
        class="fam-page-banner__btn"
        @click="emit('action')"
      >
        {{ banner.action }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.fam-page-banner {
  position: relative;
  width: 100%;
  margin: 0;
  padding: 0;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(80, 120, 150, 0.08);
  background: #eaf7fc;
}

.fam-page-banner--compact {
  margin-left: auto;
  margin-right: auto;
}

/* 与健康/照护/订单 fit 画布一致的柔和蓝青扩展底 */
.fam-page-banner__soft-fill {
  position: absolute;
  inset: -12%;
  z-index: 0;
  background-color: #eaf7fc;
  background-size: cover;
  background-position: center center;
  filter: blur(28px) saturate(0.92);
  transform: scale(1.08);
  pointer-events: none;
}

.fam-page-banner__soft-fill::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(234, 247, 252, 0.42) 0%,
    rgba(223, 243, 234, 0.32) 48%,
    rgba(231, 241, 250, 0.38) 100%
  );
}

.fam-page-banner__img {
  position: relative;
  z-index: 1;
  display: block;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  border: 0;
  object-fit: cover;
  transition: none;
}

.fam-page-banner--soft-fill .fam-page-banner__img {
  background: transparent;
}

/* 自然高度：按比例缩放，可配 max-height；用于照护等收窄场景 */
.fam-page-banner--natural {
  height: auto;
}

.fam-page-banner--natural .fam-page-banner__img {
  width: 100%;
  height: auto;
  max-height: var(--banner-natural-max, 560px);
  object-fit: contain;
  object-position: center center;
}

.fam-page-banner__mask {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.fam-page-banner--left .fam-page-banner__mask {
  background: linear-gradient(
    90deg,
    rgba(44, 74, 94, 0.5) 0%,
    rgba(44, 74, 94, 0.22) 42%,
    rgba(44, 74, 94, 0) 72%
  );
}

.fam-page-banner--right .fam-page-banner__mask {
  background: linear-gradient(
    270deg,
    rgba(44, 74, 94, 0.5) 0%,
    rgba(44, 74, 94, 0.22) 42%,
    rgba(44, 74, 94, 0) 72%
  );
}

.fam-page-banner__text {
  position: absolute;
  z-index: 2;
  top: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 48%;
  padding: 0 28px;
  color: #fff;
  pointer-events: none;
}

.fam-page-banner--left .fam-page-banner__text {
  left: 0;
  text-align: left;
}

.fam-page-banner--right .fam-page-banner__text {
  right: 0;
  text-align: right;
  align-items: flex-end;
}

.fam-page-banner__text h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
  white-space: pre-line;
}

.fam-page-banner__text .sub {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.55;
  white-space: pre-line;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.2);
}

.fam-page-banner__text .desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.55;
  opacity: 0.95;
  white-space: pre-line;
}

/* 底部渐变文案（我的老人 / 服务评价） */
.fam-page-banner__bottom-shade {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 52%;
  z-index: 1;
  pointer-events: none;
  background: linear-gradient(
    to top,
    rgba(20, 55, 70, 0.55) 0%,
    rgba(20, 55, 70, 0.22) 42%,
    rgba(20, 55, 70, 0) 100%
  );
}

.fam-page-banner__text--bottom {
  top: auto;
  bottom: 0;
  left: 0;
  right: auto;
  max-width: 72%;
  justify-content: flex-end;
  align-items: flex-start;
  text-align: left;
  padding: 16px 22px 18px;
}

.fam-page-banner--caption-bottom .fam-page-banner__text--bottom h2 {
  font-size: 18px;
  font-weight: 700;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.28);
}

.fam-page-banner--caption-bottom .fam-page-banner__text--bottom .sub {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.22);
}

.fam-page-banner__btn {
  pointer-events: auto;
  margin-top: 12px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: #2c4a5e;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.fam-page-banner__btn:hover {
  background: #fff;
}

@media (max-width: 768px) {
  .fam-page-banner--compact {
    width: 100% !important;
  }

  .fam-page-banner__text {
    max-width: 70%;
    padding: 0 16px;
  }

  .fam-page-banner__text--bottom {
    max-width: 88%;
    padding: 14px 16px 14px;
  }

  .fam-page-banner__text h2 {
    font-size: 18px;
  }

  .fam-page-banner--caption-bottom .fam-page-banner__text--bottom h2 {
    font-size: 16px;
  }

  .fam-page-banner__text .sub,
  .fam-page-banner__text .desc {
    font-size: 12px;
  }
}
</style>
