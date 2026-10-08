import request from './request'

/**
 * GET /api/admin/dashboard/statistics
 * 管理端 Dashboard 汇总（仅 ADMIN）
 */
export function getDashboardStatistics() {
  return request.get('/admin/dashboard/statistics')
}
