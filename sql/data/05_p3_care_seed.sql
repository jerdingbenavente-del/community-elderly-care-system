-- P3 权限 + 示例服务项目
USE elderly_care;

INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'care:service:list' AS perm_code, '服务项目列表' AS perm_name, 'care' AS module UNION ALL
  SELECT 'care:service:view', '服务项目详情', 'care' UNION ALL
  SELECT 'care:service:add', '服务项目新增', 'care' UNION ALL
  SELECT 'care:service:update', '服务项目修改', 'care' UNION ALL
  SELECT 'care:service:delete', '服务项目删除', 'care' UNION ALL
  SELECT 'care:order:list', '服务订单列表', 'care' UNION ALL
  SELECT 'care:order:view', '服务订单详情', 'care' UNION ALL
  SELECT 'care:order:add', '服务订单创建', 'care' UNION ALL
  SELECT 'care:order:confirm', '服务订单确认', 'care' UNION ALL
  SELECT 'care:order:start', '服务开始', 'care' UNION ALL
  SELECT 'care:order:complete', '服务完成', 'care' UNION ALL
  SELECT 'care:order:cancel', '服务取消', 'care' UNION ALL
  SELECT 'family:order:list', '家属端订单列表', 'family' UNION ALL
  SELECT 'family:order:view', '家属端订单详情', 'family' UNION ALL
  SELECT 'family:order:add', '家属端创建预约', 'family' UNION ALL
  SELECT 'family:order:cancel', '家属端取消预约', 'family'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id FROM sys_permission p
WHERE p.perm_code IN (
  'care:service:list','care:service:view',
  'care:order:list','care:order:view',
  'care:order:start','care:order:complete'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, p.id FROM sys_permission p
WHERE p.perm_code IN (
  'family:order:list','family:order:view','family:order:add','family:order:cancel',
  'care:service:list','care:service:view'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 3 AND rp.permission_id = p.id);

INSERT INTO care_service_item (service_code, service_name, service_type, description, duration_minutes, price, status, deleted)
SELECT * FROM (
  SELECT 'LIFE_CARE' AS service_code, '生活照料' AS service_name, 'DAILY' AS service_type, '日常起居协助' AS description, 60 AS duration_minutes, 0.00 AS price, 'ENABLED' AS status, 0 AS deleted UNION ALL
  SELECT 'WASH_ASSIST', '协助洗漱', 'DAILY', '洗漱清洁协助', 30, 0.00, 'ENABLED', 0 UNION ALL
  SELECT 'MEAL_ASSIST', '协助进食', 'DAILY', '用餐协助', 45, 0.00, 'ENABLED', 0 UNION ALL
  SELECT 'REHAB_ASSIST', '康复辅助', 'REHAB', '简单康复活动辅助', 60, 0.00, 'ENABLED', 0
) v
WHERE NOT EXISTS (
  SELECT 1 FROM care_service_item i WHERE i.service_code = v.service_code AND i.deleted = 0
);
