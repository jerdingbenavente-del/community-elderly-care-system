import request from './request'

/**
 * GET /api/system/operation-logs
 * @param {{ username?: string, module?: string, dateFrom?: string, dateTo?: string, page?: number, size?: number }} params
 */
export function pageOperationLogs(params) {
  return request.get('/system/operation-logs', { params })
}
