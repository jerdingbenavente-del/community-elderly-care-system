import request from './request'

/**
 * GET /api/admin/statistics/overview
 * @param {{ dateFrom?: string, dateTo?: string }} params
 */
export function getOperationsOverview(params) {
  return request.get('/admin/statistics/overview', { params })
}
