import { completeServiceOrder, getServiceOrder, listServiceOrders, startServiceOrder } from './serviceOrder'
import { pageCareStaffSchedules } from './careStaffSchedule'

/**
 * 护理员端：我的服务订单
 * GET /api/service-orders — CARE_STAFF 后端强制仅本人 careStaffId
 */
export function pageMyServiceOrders(params = {}) {
  return listServiceOrders(params)
}

/** GET /api/service-orders/{id} — 后端 assertCanView（仅本人订单） */
export function getMyServiceOrder(id) {
  return getServiceOrder(id)
}

/** POST /api/service-orders/{id}/start */
export function startMyServiceOrder(id) {
  return startServiceOrder(id)
}

/** POST /api/service-orders/{id}/complete */
export function completeMyServiceOrder(id) {
  return completeServiceOrder(id)
}

/**
 * 护理员端：服务记录（已完成历史，只读）
 * GET /api/service-orders?status=COMPLETED
 * 可选 scheduledStartFrom/To（ISO yyyy-MM-ddTHH:mm:ss）
 * 不要传 careStaffId：CARE_STAFF 由后端强制本人
 */
export function pageMyServiceRecords(params = {}) {
  return listServiceOrders({
    ...params,
    status: 'COMPLETED',
  })
}

/**
 * 护理员端：我的排班（只读）
 * GET /api/care-staff-schedules
 * Query: scheduleDate? (yyyy-MM-dd), page, size …
 * 不要传 careStaffId：CARE_STAFF 由后端强制绑定本人；传他人 ID 会 403
 */
export function pageMySchedules(params = {}) {
  return pageCareStaffSchedules(params)
}

/** 本地日历日 yyyy-MM-dd */
export function formatLocalDate(d = new Date()) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 今日 scheduledStart 查询窗口
 * GET 查询绑定 LocalDateTime 需 ISO-8601（yyyy-MM-ddTHH:mm:ss）；
 * 空格格式（yyyy-MM-dd HH:mm:ss）会 400（@JsonFormat 不作用于 query）。
 */
export function todayScheduleWindow(d = new Date()) {
  const day = formatLocalDate(d)
  return {
    date: day,
    scheduledStartFrom: `${day}T00:00:00`,
    scheduledStartTo: `${day}T23:59:59`,
  }
}
