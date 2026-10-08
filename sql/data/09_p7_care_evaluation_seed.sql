-- P7 服务评价权限
SET NAMES utf8mb4;
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'care:evaluation:list' AS perm_code, '服务评价列表(管理端)' AS perm_name, 'care' AS module UNION ALL
  SELECT 'care:evaluation:view', '服务评价详情(管理端)', 'care' UNION ALL
  SELECT 'family:evaluation:list', '家属端评价列表', 'family' UNION ALL
  SELECT 'family:evaluation:view', '家属端评价查看', 'family' UNION ALL
  SELECT 'family:evaluation:add', '家属端提交评价', 'family'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

-- ADMIN 拥有全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- FAMILY：家属端评价
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, p.id FROM sys_permission p
WHERE p.perm_code IN (
  'family:evaluation:list','family:evaluation:view','family:evaluation:add'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 3 AND rp.permission_id = p.id);
