-- G5 护理员请假权限
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'care:leave:list' AS perm_code, '请假申请列表(本人)' AS perm_name, 'care' AS module UNION ALL
  SELECT 'care:leave:view', '请假申请详情', 'care' UNION ALL
  SELECT 'care:leave:add', '提交请假申请', 'care' UNION ALL
  SELECT 'care:leave:cancel', '撤销待审批请假', 'care' UNION ALL
  SELECT 'care:leave:admin:list', '管理员请假列表', 'care' UNION ALL
  SELECT 'care:leave:approve', '审批请假申请', 'care'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

-- ADMIN：全部 leave 权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code LIKE 'care:leave:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- CARE_STAFF：仅本人申请相关（不含 admin:list / approve）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code IN ('care:leave:list', 'care:leave:view', 'care:leave:add', 'care:leave:cancel')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);
