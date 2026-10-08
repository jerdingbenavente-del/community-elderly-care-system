import request from './request'

/** GET /api/family/service-items — 启用中的服务项目（家属预约目录） */
export function listFamilyServiceItems() {
  return request.get('/family/service-items')
}

/**
 * GET /api/care-service-items/{id}
 * FAMILY 拥有 care:service:view，可用于详情
 */
export function getCareServiceItem(id) {
  return request.get(`/care-service-items/${id}`)
}

/** 别名 */
export function getCareServiceList() {
  return listFamilyServiceItems()
}

export function getCareServiceDetail(id) {
  return getCareServiceItem(id)
}
