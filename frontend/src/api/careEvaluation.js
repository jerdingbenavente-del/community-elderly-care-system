import request from './request'

/**
 * GET /api/admin/evaluations
 * Query: elderId?, serviceOrderId?, score?, page, size
 */
export function pageAdminEvaluations(params = {}) {
  return request.get('/admin/evaluations', { params })
}

/** GET /api/admin/evaluations/{id} */
export function getAdminEvaluation(id) {
  return request.get(`/admin/evaluations/${id}`)
}
