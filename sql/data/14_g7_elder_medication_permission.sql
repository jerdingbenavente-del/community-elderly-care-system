-- G7 用药权限。种子里的 medicine:list 没有对应接口，本模块按 care:* 风格新增。
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'care:medication:list' AS perm_code, '用药列表' AS perm_name, 'care' AS module UNION ALL
  SELECT 'care:medication:view', '用药详情', 'care' UNION ALL
  SELECT 'care:medication:add', '新增用药', 'care' UNION ALL
  SELECT 'care:medication:update', '修改用药', 'care' UNION ALL
  SELECT 'care:medication:delete', '删除用药', 'care' UNION ALL
  SELECT 'care:medication:admin:list', '管理员用药列表', 'care' UNION ALL
  SELECT 'care:medication:remind', '今日用药提醒', 'care'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code LIKE 'care:medication:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code IN ('care:medication:list', 'care:medication:view', 'care:medication:remind')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code IN ('care:medication:list', 'care:medication:view')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 3 AND rp.permission_id = p.id);
