-- G6 护理员考勤权限
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'care:attendance:today' AS perm_code, '今日考勤' AS perm_name, 'care' AS module UNION ALL
  SELECT 'care:attendance:check-in', '上班签到', 'care' UNION ALL
  SELECT 'care:attendance:check-out', '下班签退', 'care' UNION ALL
  SELECT 'care:attendance:list', '我的考勤', 'care' UNION ALL
  SELECT 'care:attendance:admin:list', '全部考勤查询', 'care'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code LIKE 'care:attendance:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code IN (
    'care:attendance:today',
    'care:attendance:check-in',
    'care:attendance:check-out',
    'care:attendance:list'
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);
