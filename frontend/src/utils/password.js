/**
 * 与 P8 后端 PasswordRules 一致（前端仅体验校验）。
 */
export const NEW_PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/

export function isValidNewPassword(password) {
  if (!password || typeof password !== 'string') return false
  if (password.includes(' ') || password.includes('\t')) return false
  return NEW_PASSWORD_PATTERN.test(password)
}
