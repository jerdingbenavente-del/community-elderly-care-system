-- G9 社区活动权限
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'care:activity:list' AS perm_code, '活动列表' AS perm_name, 'care' AS module UNION ALL
  SELECT 'care:activity:view', '活动查看', 'care' UNION ALL
  SELECT 'care:activity:add', '活动新增', 'care' UNION ALL
  SELECT 'care:activity:update', '活动修改', 'care' UNION ALL
  SELECT 'care:activity:delete', '活动删除', 'care' UNION ALL
  SELECT 'care:activity:publish', '活动发布', 'care' UNION ALL
  SELECT 'care:activity:cancel', '活动取消', 'care' UNION ALL
  SELECT 'care:activity:admin:list', '活动管理列表', 'care'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

-- ADMIN：全部活动权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code LIKE 'care:activity:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- CARE_STAFF：只读
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code IN ('care:activity:list', 'care:activity:view')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);

-- FAMILY：只读
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code IN ('care:activity:list', 'care:activity:view')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 3 AND rp.permission_id = p.id);
