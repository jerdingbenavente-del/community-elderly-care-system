-- RBAC 种子数据（开发/演示环境）
-- 密码均为 BCrypt 哈希，禁止明文入库
USE elderly_care;

-- 清理关联后重建种子（仅开发环境使用）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE sys_role_permission;
TRUNCATE TABLE sys_user_role;
TRUNCATE TABLE sys_permission;
TRUNCATE TABLE sys_role;
TRUNCATE TABLE sys_user;
TRUNCATE TABLE sys_operation_log;
TRUNCATE TABLE elder_family;
TRUNCATE TABLE emergency_contact;
TRUNCATE TABLE care_staff;
TRUNCATE TABLE elder;
SET FOREIGN_KEY_CHECKS = 1;

-- 角色
INSERT INTO sys_role (id, role_code, role_name, status, deleted, remark) VALUES
(1, 'ADMIN', '管理员', 1, 0, '系统管理员'),
(2, 'CARE_STAFF', '照护人员', 1, 0, '一线照护人员'),
(3, 'FAMILY', '家属', 1, 0, '老人家属');

-- 权限（仅保留有业务意义的基础权限码）
INSERT INTO sys_permission (id, perm_code, perm_name, module, status, deleted) VALUES
(1,  'system:user:list',   '用户列表',     'system', 1, 0),
(2,  'system:user:add',    '用户新增',     'system', 1, 0),
(3,  'system:user:update', '用户修改',     'system', 1, 0),
(4,  'system:user:delete', '用户删除',     'system', 1, 0),
(5,  'system:role:list',   '角色列表',     'system', 1, 0),
(6,  'system:log:list',    '操作日志列表', 'system', 1, 0),
(7,  'elder:list',         '老人列表',     'elder', 1, 0),
(8,  'elder:view',         '老人详情',     'elder', 1, 0),
(9,  'elder:add',          '老人新增',     'elder', 1, 0),
(10, 'elder:update',       '老人修改',     'elder', 1, 0),
(11, 'elder:delete',       '老人删除',     'elder', 1, 0),
(12, 'care_staff:list',    '照护人员列表', 'care_staff', 1, 0),
(13, 'care_staff:view',    '照护人员详情', 'care_staff', 1, 0),
(14, 'health:list',        '健康记录列表', 'health', 1, 0),
(15, 'health:view',        '健康记录详情', 'health', 1, 0),
(16, 'health:add',         '健康记录录入', 'health', 1, 0),
(17, 'warning:list',       '预警列表',     'warning', 1, 0),
(18, 'warning:handle',     '预警处理',     'warning', 1, 0),
(19, 'service:list',       '服务项目列表', 'service', 1, 0),
(20, 'service:create',     '服务预约',     'service', 1, 0),
(21, 'schedule:list',      '排班列表',     'schedule', 1, 0),
(22, 'schedule:update',    '排班调整',     'schedule', 1, 0),
(23, 'meal:list',          '餐饮查看',     'meal', 1, 0),
(24, 'activity:list',      '活动列表',     'activity', 1, 0),
(25, 'medicine:list',      '用药计划列表', 'medicine', 1, 0),
(26, 'report:view',        '统计报表查看', 'report', 1, 0);

-- ADMIN：全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE deleted = 0;

-- CARE_STAFF：业务操作，无系统用户管理权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission
WHERE perm_code IN (
  'elder:list', 'elder:view', 'elder:add', 'elder:update',
  'care_staff:list', 'care_staff:view',
  'health:list', 'health:view', 'health:add',
  'warning:list', 'warning:handle',
  'service:list', 'service:create',
  'schedule:list', 'schedule:update',
  'meal:list', 'activity:list', 'medicine:list'
);

-- FAMILY：只读类权限（数据范围由 elder_family 另行约束）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission
WHERE perm_code IN (
  'elder:view',
  'health:list', 'health:view',
  'warning:list',
  'service:list',
  'meal:list',
  'activity:list',
  'medicine:list'
);

-- 开发环境账号（密码见 README，均为 BCrypt）
-- admin / Admin@123
-- care01 / Care@123
-- family01 / Family@123
INSERT INTO sys_user (id, username, password_hash, real_name, phone, status, deleted) VALUES
(1, 'admin', '$2a$10$ht7UUhPawiGPQRemnDuFHOJ.zZTsCBSzvAY.P8DMoDt2q1iKTPliu', '系统管理员', '13800000001', 1, 0),
(2, 'care01', '$2a$10$cUVG6rpPS2y4bzmaQBQ2FuthfUy1c8UmRNvTEBJnC0AU04JW5FMw2', '照护员一号', '13800000002', 1, 0),
(3, 'family01', '$2a$10$f2Zuk5HBElH9KKGUK5NVYuHg5WC23BA/afzKJwjfzOGmgEw24T0Ru', '家属一号', '13800000003', 1, 0);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2),
(3, 3);

-- 示例照护人员档案（关联 care01，非业务 CRUD）
INSERT INTO care_staff (user_id, name, phone, position, status, deleted) VALUES
(2, '照护员一号', '13800000002', '护理员', 1, 0);
