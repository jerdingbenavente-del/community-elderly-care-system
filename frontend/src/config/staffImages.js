/**
 * 护理员端图片分配（复用项目已有养老图片）
 */
import img2 from '@/assets/images/2.jpg'
import img3 from '@/assets/images/3.jpg'
import img4 from '@/assets/images/4.jpg'
import bannerStaff from '@/assets/images/12.jpg'

/**
 * 工作台中央顶部 Banner 卡片（非整页背景）
 * 家属端 HeroCarousel 同构：固定卡片高度 + cover
 */
export const staffHeroBanner = {
  image: bannerStaff,
  fit: 'cover',
  objectPosition: 'center 42%',
}

export const staffSidebarPlant = img2
export const staffPageBackground = img4
export const staffFooterDecor = img3
