import request from './request'

/** GET /api/family/evaluations — 当前家属绑定老人范围内的评价列表 */
export function listMyEvaluations() {
  return request.get('/family/evaluations')
}

/** GET /api/family/service-orders/{orderId}/evaluation */
export function getOrderEvaluation(orderId) {
  return request.get(`/family/service-orders/${orderId}/evaluation`)
}

/**
 * POST /api/family/service-orders/{orderId}/evaluation
 * Body: { score: 1-5, content?: max 500 }
 */
export function createOrderEvaluation(orderId, data) {
  return request.post(`/family/service-orders/${orderId}/evaluation`, data)
}

export function getEvaluationList() {
  return listMyEvaluations()
}

export function getEvaluationByOrder(orderId) {
  return getOrderEvaluation(orderId)
}

export function submitEvaluation(orderId, data) {
  return createOrderEvaluation(orderId, data)
}
