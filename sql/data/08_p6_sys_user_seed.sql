-- P6 系统用户管理权限增量
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'system:user:view' AS perm_code, '用户详情' AS perm_name, 'system' AS module UNION ALL
  SELECT 'system:user:enable', '用户启用', 'system' UNION ALL
  SELECT 'system:user:disable', '用户停用', 'system' UNION ALL
  SELECT 'system:user:role', '用户角色分配', 'system'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

-- ADMIN 拥有全部 system:user 权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code LIKE 'system:user:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);
