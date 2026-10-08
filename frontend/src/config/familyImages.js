/**
 * 家属端图片配置
 * 首页轮播（carouselSlides）本阶段禁止改动其展示逻辑；仅子页 Banner 可调整资源分配。
 *
 * 资源盘点（assets/images）：
 * 1–4 风景装饰 | 5–12 主题图 | banner1/2/3 摄影
 * family-*-banner-fit：健康/照护/订单用画布图（标准母版，本阶段不改）
 * 我的老人 → banner1-family（contain + 柔和扩展填充）
 * 服务评价 → 5.jpg（contain + 柔和扩展填充）
 */
import img1 from '@/assets/images/1.jpg'
import img2 from '@/assets/images/2.jpg'
import img3 from '@/assets/images/3.jpg'
import img4 from '@/assets/images/4.jpg'
import img5 from '@/assets/images/5.jpg'
import bannerFamily from '@/assets/images/banner1-family.jpg'
import bannerCompanion from '@/assets/images/banner2-health.jpg'
import bannerCare from '@/assets/images/banner3-care.jpg'
import healthBannerFit from '@/assets/images/family-health-banner-fit.jpg'
import careBannerFit from '@/assets/images/family-care-banner-fit.jpg'
import ordersBannerFit from '@/assets/images/family-orders-banner-fit.jpg'

export const pageBackgroundImage = img4
export const sidebarPlantImage = img2
export const cornerPlantLeft = img1
export const cornerPlantRight = img4
export const footerDecorImage = img3

/**
 * 家属端子页 Banner 外框标准（以健康管理 / 照护服务 / 我的订单为准）
 * 宽 100%；高 clamp(200px, 宽/3.85, 240px)；圆角 16px；padding 0
 */
export const FAMILY_BANNER_FRAME = {
  objectFit: 'cover',
  heightMode: 'ratio',
  bannerRatio: 3.85,
  minHeight: 200,
  maxHeight: 240,
}

/** 个人中心轻量装饰条 */
export const profilePageDecor = {
  image: img1,
  objectPosition: 'center 40%',
}

export const heroBanner = {
  image: bannerFamily,
  title: '智慧养老',
  subtitle: '让关爱没有距离',
  desc: '关注老人生活 · 守护健康安全 · 享受品质晚年',
  action: '查看我的老人 →',
  path: '/family/elders',
  textAlign: 'left',
  objectPosition: '62% 38%',
}

/**
 * 我的老人：沿用 banner1-family.jpg；外框同三标准页
 * contain 完整显示 + softFill 柔和扩展（不裁剪顶部文字/人物）
 */
export const eldersPageBanner = {
  image: bannerFamily,
  title: '我的老人',
  subtitle: '关注老人近况，守护健康生活',
  objectPosition: 'center top',
  textAlign: 'left',
  bakedText: false,
  caption: 'bottom',
  showMask: false,
  softFill: true,
  ...FAMILY_BANNER_FRAME,
  objectFit: 'contain',
}

/** 健康管理（标准页之一，本阶段不改） */
export const healthPageBanner = {
  image: healthBannerFit,
  title: '健康管理',
  subtitle: '关注老人健康变化，\n让关爱更及时。',
  objectPosition: 'center center',
  textAlign: 'left',
  bakedText: false,
  showMask: true,
  ...FAMILY_BANNER_FRAME,
}

/** 照护服务（标准页之一，本阶段不改） */
export const carePageBanner = {
  image: careBannerFit,
  title: '专业照护',
  subtitle: '让陪伴更有温度',
  desc: '从日常生活照料到专业护理，\n为老人提供贴心服务。',
  action: '查看服务项目',
  objectPosition: 'center center',
  textAlign: 'right',
  bakedText: true,
  showMask: false,
  ...FAMILY_BANNER_FRAME,
}

/** 我的订单（标准页之一，本阶段不改） */
export const ordersPageBanner = {
  image: ordersBannerFit,
  title: '我的订单',
  subtitle: '掌握每一次照护安排',
  desc: '查看预约进度，及时了解服务状态。',
  objectPosition: 'center center',
  textAlign: 'right',
  bakedText: false,
  showMask: true,
  ...FAMILY_BANNER_FRAME,
}

/**
 * 服务评价：5.jpg；外框同三标准页
 * contain 完整显示人物场景 + softFill 柔和扩展（不裁剪）
 */
export const evaluationsPageBanner = {
  image: img5,
  title: '服务评价',
  subtitle: '记录服务体验，共同提升照护质量',
  objectPosition: 'center center',
  textAlign: 'left',
  bakedText: false,
  caption: 'bottom',
  showMask: false,
  softFill: true,
  ...FAMILY_BANNER_FRAME,
  objectFit: 'contain',
}

/** 首页轮播 —— 禁止改动本数组的展示职责（HeroCarousel 专用） */
export const carouselSlides = [
  { key: 'family', ...heroBanner, tone: 'warm' },
  {
    key: 'health',
    image: bannerCompanion,
    title: '健康监测',
    subtitle: '守护每一天',
    desc: '及时关注健康变化，异常情况及时预警。',
    action: '查看健康情况 →',
    path: '/family/health',
    textAlign: 'left',
    objectPosition: '50% 36%',
    tone: 'warm',
  },
  {
    key: 'care',
    image: bannerCare,
    title: '专业照护',
    subtitle: '让生活更安心',
    desc: '在线预约专业照护服务，实时查看服务进度。',
    action: '立即预约服务 →',
    path: '/family/care-services',
    textAlign: 'right',
    objectPosition: '58% 32%',
    tone: 'cool',
  },
]
