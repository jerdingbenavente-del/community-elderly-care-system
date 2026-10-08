export const MEAL_TYPES = [
  { value: 'BREAKFAST', label: '早餐' },
  { value: 'LUNCH', label: '午餐' },
  { value: 'DINNER', label: '晚餐' },
]

const WEEKDAY = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

export function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function parseDate(text) {
  const [y, m, d] = String(text).split('-').map(Number)
  return new Date(y, m - 1, d)
}

export function mondayOf(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const day = d.getDay()
  const diff = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + diff)
  return d
}

export function nextWeekMonday(today = new Date()) {
  const monday = mondayOf(today)
  monday.setDate(monday.getDate() + 7)
  return monday
}

export function addDays(date, days) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  d.setDate(d.getDate() + days)
  return d
}

export function weekDays(mondayText) {
  const monday = parseDate(mondayText)
  return Array.from({ length: 7 }, (_, index) => {
    const date = addDays(monday, index)
    return { date: formatDate(date), label: WEEKDAY[index] }
  })
}

export function mealLabel(type) {
  return MEAL_TYPES.find((item) => item.value === type)?.label || type
}

export function shiftWeek(mondayText, deltaWeeks) {
  const monday = parseDate(mondayText)
  monday.setDate(monday.getDate() + deltaWeeks * 7)
  return formatDate(monday)
}
