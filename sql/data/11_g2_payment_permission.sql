-- G2：家属模拟支付权限
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT 'family:order:pay', '家属端模拟支付', 'family', 1, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = 'family:order:pay' AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, p.id FROM sys_permission p
WHERE p.perm_code = 'family:order:pay'
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 3 AND rp.permission_id = p.id
  );
