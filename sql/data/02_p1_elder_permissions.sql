-- P1 老人档案相关权限增量（可在已有库上重复执行需先清理同编码）
USE elderly_care;

-- 新增权限码（若不存在）
INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'elder:family:list' AS perm_code, '家属绑定列表' AS perm_name, 'elder' AS module UNION ALL
  SELECT 'elder:family:add', '家属绑定新增', 'elder' UNION ALL
  SELECT 'elder:family:delete', '家属绑定解除', 'elder' UNION ALL
  SELECT 'elder:emergency-contact:list', '紧急联系人列表', 'elder' UNION ALL
  SELECT 'elder:emergency-contact:view', '紧急联系人详情', 'elder' UNION ALL
  SELECT 'elder:emergency-contact:add', '紧急联系人新增', 'elder' UNION ALL
  SELECT 'elder:emergency-contact:update', '紧急联系人修改', 'elder' UNION ALL
  SELECT 'elder:emergency-contact:delete', '紧急联系人删除', 'elder' UNION ALL
  SELECT 'family:elder:list', '家属端老人列表', 'family' UNION ALL
  SELECT 'family:elder:view', '家属端老人详情', 'family'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

-- ADMIN：补全全部新权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id
FROM sys_permission p
WHERE p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 1 AND rp.permission_id = p.id
  );

-- CARE_STAFF：老人档案相关（不含 elder:delete）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id
FROM sys_permission p
WHERE p.perm_code IN (
  'elder:list', 'elder:view', 'elder:add', 'elder:update',
  'elder:family:list', 'elder:family:add', 'elder:family:delete',
  'elder:emergency-contact:list', 'elder:emergency-contact:view',
  'elder:emergency-contact:add', 'elder:emergency-contact:update',
  'elder:emergency-contact:delete'
)
AND NOT EXISTS (
  SELECT 1 FROM sys_role_permission rp
  WHERE rp.role_id = 2 AND rp.permission_id = p.id
);

-- FAMILY：仅家属端权限；移除管理端 elder:view，防止绕过 /api/elders
DELETE rp FROM sys_role_permission rp
INNER JOIN sys_permission p ON p.id = rp.permission_id
WHERE rp.role_id = 3 AND p.perm_code = 'elder:view';

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, p.id
FROM sys_permission p
WHERE p.perm_code IN ('family:elder:list', 'family:elder:view')
AND NOT EXISTS (
  SELECT 1 FROM sys_role_permission rp
  WHERE rp.role_id = 3 AND rp.permission_id = p.id
);
