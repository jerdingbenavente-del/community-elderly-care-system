-- G8 周菜单与饮食调整权限
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'care:menu:list' AS perm_code, '周菜单列表' AS perm_name, 'care' AS module UNION ALL
  SELECT 'care:menu:view', '周菜单查看', 'care' UNION ALL
  SELECT 'care:menu:add', '周菜单新增', 'care' UNION ALL
  SELECT 'care:menu:update', '周菜单修改', 'care' UNION ALL
  SELECT 'care:menu:delete', '周菜单删除', 'care' UNION ALL
  SELECT 'care:dietary:view', '饮食备注查看', 'care' UNION ALL
  SELECT 'care:dietary:update', '饮食备注维护', 'care' UNION ALL
  SELECT 'care:dietary:admin:list', '饮食备注管理列表', 'care' UNION ALL
  SELECT 'care:meal-adjust:list', '个性化饮食查看', 'care' UNION ALL
  SELECT 'care:meal-adjust:add', '个性化饮食新增', 'care' UNION ALL
  SELECT 'care:meal-adjust:update', '个性化饮食修改', 'care' UNION ALL
  SELECT 'care:meal-adjust:delete', '个性化饮食取消', 'care'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND (p.perm_code LIKE 'care:menu:%' OR p.perm_code LIKE 'care:dietary:%' OR p.perm_code LIKE 'care:meal-adjust:%')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code IN ('care:menu:list', 'care:menu:view', 'care:dietary:view', 'care:meal-adjust:list')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code IN ('care:menu:list', 'care:menu:view', 'care:dietary:view', 'care:dietary:update', 'care:meal-adjust:list')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 3 AND rp.permission_id = p.id);
