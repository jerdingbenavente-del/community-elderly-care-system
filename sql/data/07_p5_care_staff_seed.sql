-- P5 护理员管理权限 + 工号回填
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'care_staff:add' AS perm_code, '照护人员新增' AS perm_name, 'care_staff' AS module UNION ALL
  SELECT 'care_staff:update', '照护人员修改', 'care_staff' UNION ALL
  SELECT 'care_staff:delete', '照护人员删除', 'care_staff' UNION ALL
  SELECT 'care_staff:enable', '照护人员启用', 'care_staff' UNION ALL
  SELECT 'care_staff:disable', '照护人员停用', 'care_staff'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code IN (
    'care_staff:list','care_staff:view','care_staff:add','care_staff:update',
    'care_staff:delete','care_staff:enable','care_staff:disable'
  )
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- CARE_STAFF 仅保留 list/view（查自己由 Service 限制）；/me 使用 view
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id FROM sys_permission p
WHERE p.perm_code IN ('care_staff:list', 'care_staff:view')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);

-- 回填已有护理员工号（不覆盖已有值）
UPDATE care_staff
SET employee_no = CONCAT('CS', LPAD(id, 3, '0'))
WHERE deleted = 0 AND (employee_no IS NULL OR employee_no = '');
