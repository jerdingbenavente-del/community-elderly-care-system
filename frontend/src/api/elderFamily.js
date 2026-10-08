import request from './request'

/** GET /api/elder-families?elderId=&familyUserId= */
export function listElderFamilies(params = {}) {
  return request.get('/elder-families', { params })
}

/**
 * POST /api/elder-families
 * Body: { elderId, familyUserId, relationship?, isPrimary? }
 */
export function bindElderFamily(data) {
  return request.post('/elder-families', data)
}
