import { pinyin } from 'pinyin-pro'

/**
 * 仅用于开户表单「账号预览」展示；真实用户名由 P8 后端生成。
 */
export function buildNamePrefix(realName) {
  if (!realName || !String(realName).trim()) return ''
  const cleaned = String(realName).trim()
  let prefix = ''
  for (const ch of cleaned) {
    if (/\s/.test(ch)) continue
    if (/[A-Za-z0-9]/.test(ch)) {
      prefix += ch.toLowerCase()
      continue
    }
    if (/[\u4e00-\u9fff]/.test(ch)) {
      const py = pinyin(ch, { toneType: 'none', type: 'array' })
      if (py && py[0]) prefix += String(py[0]).charAt(0).toLowerCase()
    }
  }
  return prefix
}

export function randomFiveDigits() {
  return String(Math.floor(Math.random() * 100000)).padStart(5, '0')
}

/** 预览账号：prefix@xxxxx（非最终入库值） */
export function previewUsername(realName, digits) {
  const prefix = buildNamePrefix(realName)
  if (!prefix) return ''
  const five = digits || randomFiveDigits()
  return `${prefix}@${five}`
}
