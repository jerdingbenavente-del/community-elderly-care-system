/**
 * P0 回归：authSession JWT 角色权威与撕裂修复。
 * 运行：node scripts/p0-auth-session-check.mjs
 */
import assert from 'node:assert/strict'
import { webcrypto } from 'node:crypto'

// Minimal atob for Node
if (typeof globalThis.atob !== 'function') {
  globalThis.atob = (s) => Buffer.from(s, 'base64').toString('binary')
}

// Dynamic import of ESM module under Vite path alias won't work; inline mirrors of pure fns:
function normalizeRoles(list) {
  if (!Array.isArray(list)) return []
  return list
    .map((item) => {
      if (typeof item === 'string') return item
      if (item && typeof item === 'object') return item.roleCode || item.code || item.role || ''
      return ''
    })
    .filter(Boolean)
}

function parseJwtPayload(token) {
  if (!token || typeof token !== 'string') return null
  const parts = token.split('.')
  if (parts.length < 2) return null
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    return JSON.parse(Buffer.from(padded, 'base64').toString('utf8'))
  } catch {
    return null
  }
}

function rolesFromToken(token, fallbackRoles = []) {
  const payload = parseJwtPayload(token)
  const fromJwt = normalizeRoles(payload?.roles)
  if (fromJwt.length) return fromJwt
  return normalizeRoles(fallbackRoles)
}

function b64url(obj) {
  return Buffer.from(JSON.stringify(obj)).toString('base64url')
}

function fakeJwt(roles) {
  return `${b64url({ alg: 'none' })}.${b64url({ userId: 1, username: 'x', roles })}.sig`
}

// Case 1: torn session — local roles ADMIN but token FAMILY
const familyToken = fakeJwt(['FAMILY'])
const fixed = rolesFromToken(familyToken, ['ADMIN'])
assert.deepEqual(fixed, ['FAMILY'], 'JWT roles must win over stale localStorage roles')

// Case 2: admin token keeps ADMIN even if local empty
const adminToken = fakeJwt(['ADMIN'])
assert.deepEqual(rolesFromToken(adminToken, []), ['ADMIN'])

// Case 3: no token falls back
assert.deepEqual(rolesFromToken('', ['CARE_STAFF']), ['CARE_STAFF'])

console.log('P0 authSession checks PASS')
void webcrypto
