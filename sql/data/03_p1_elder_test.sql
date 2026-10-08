-- P1 测试辅助说明：业务测试数据建议通过 API 创建，避免手工 SQL 与权限绕开。
-- 本文件预留第二家属账号，便于越权对照（可选）。
USE elderly_care;

INSERT INTO sys_user (username, password_hash, real_name, phone, status, deleted)
SELECT 'family02',
       '$2a$10$f2Zuk5HBElH9KKGUK5NVYuHg5WC23BA/afzKJwjfzOGmgEw24T0Ru',
       '家属二号',
       '13800000004',
       1,
       0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'family02' AND deleted = 0);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, 3
FROM sys_user u
WHERE u.username = 'family02'
  AND u.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = 3
  );
