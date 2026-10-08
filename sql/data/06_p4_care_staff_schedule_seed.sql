-- P4 排班权限 + 示例排班 + 第二护理员(测试用)
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'care:schedule:list' AS perm_code, '排班列表' AS perm_name, 'care' AS module UNION ALL
  SELECT 'care:schedule:view', '排班详情', 'care' UNION ALL
  SELECT 'care:schedule:add', '排班新增', 'care' UNION ALL
  SELECT 'care:schedule:update', '排班修改', 'care' UNION ALL
  SELECT 'care:schedule:delete', '排班删除', 'care'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND p.perm_code LIKE 'care:schedule:%'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id FROM sys_permission p
WHERE p.perm_code IN ('care:schedule:list', 'care:schedule:view')
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);

-- 可选第二护理员账号（用于越权执行测试），密码 Care@123
INSERT INTO sys_user (username, password_hash, real_name, phone, status, deleted)
SELECT 'care02', '$2a$10$cUVG6rpPS2y4bzmaQBQ2FuthfUy1c8UmRNvTEBJnC0AU04JW5FMw2', '照护员二号', '13800000004', 1, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.username = 'care02' AND u.deleted = 0);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, 2 FROM sys_user u
WHERE u.username = 'care02' AND u.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = 2);

INSERT INTO care_staff (user_id, name, phone, position, status, deleted)
SELECT u.id, '照护员二号', '13800000004', '护理员', 1, 0
FROM sys_user u
WHERE u.username = 'care02' AND u.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM care_staff cs WHERE cs.user_id = u.id AND cs.deleted = 0);

-- 示例排班：care_staff_id=1，未来第3天上午/下午（便于联调）
INSERT INTO care_staff_schedule (care_staff_id, schedule_date, start_time, end_time, status, remark, deleted)
SELECT 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '09:00:00', '12:00:00', 'AVAILABLE', 'P4示例上午班', 0
FROM DUAL
WHERE EXISTS (SELECT 1 FROM care_staff WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (
    SELECT 1 FROM care_staff_schedule s
    WHERE s.care_staff_id = 1
      AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY)
      AND s.start_time = '09:00:00'
      AND s.deleted = 0
  );

INSERT INTO care_staff_schedule (care_staff_id, schedule_date, start_time, end_time, status, remark, deleted)
SELECT 1, DATE_ADD(CURDATE(), INTERVAL 3 DAY), '13:30:00', '17:30:00', 'AVAILABLE', 'P4示例下午班', 0
FROM DUAL
WHERE EXISTS (SELECT 1 FROM care_staff WHERE id = 1 AND deleted = 0)
  AND NOT EXISTS (
    SELECT 1 FROM care_staff_schedule s
    WHERE s.care_staff_id = 1
      AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY)
      AND s.start_time = '13:30:00'
      AND s.deleted = 0
  );
