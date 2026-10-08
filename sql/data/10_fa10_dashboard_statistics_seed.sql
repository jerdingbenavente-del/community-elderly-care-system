-- FA10 管理端 Dashboard 统计权限（可选；接口另以 ADMIN 角色校验兜底）
SET NAMES utf8mb4;
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'dashboard:statistics:view' AS perm_code, '工作台统计查看' AS perm_name, 'dashboard' AS module
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

-- 仅 ADMIN（role_id=1）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.perm_code = 'dashboard:statistics:view'
  AND p.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 1 AND rp.permission_id = p.id
  );
