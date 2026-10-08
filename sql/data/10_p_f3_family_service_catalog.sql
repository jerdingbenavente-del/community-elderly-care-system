-- F3: 可选补充家属服务目录权限（接口当前也可使用 family:order:add）
INSERT INTO sys_permission (perm_code, perm_name, module)
SELECT 'family:service:list', '家属端服务项目目录', 'family'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission WHERE perm_code = 'family:service:list'
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.perm_code = 'family:service:list'
WHERE r.role_code = 'FAMILY'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
