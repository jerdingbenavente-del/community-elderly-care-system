-- P2 健康阈值种子 + 权限增量
USE elderly_care;

-- 阈值规则（社区健康管理辅助提醒，非医疗诊断）
INSERT INTO health_threshold (indicator, rule_name, min_value, max_value, warning_level, unit, status, description)
SELECT * FROM (
  SELECT 'SYSTOLIC_PRESSURE' AS indicator, '收缩压阈值' AS rule_name, 90.00 AS min_value, 139.00 AS max_value, 'WARNING' AS warning_level, 'mmHg' AS unit, 1 AS status, '收缩压建议参考范围 90-139' AS description UNION ALL
  SELECT 'DIASTOLIC_PRESSURE', '舒张压阈值', 60.00, 89.00, 'WARNING', 'mmHg', 1, '舒张压建议参考范围 60-89' UNION ALL
  SELECT 'BLOOD_GLUCOSE', '血糖阈值', 3.90, 7.00, 'WARNING', 'mmol/L', 1, '血糖建议参考范围 3.9-7.0' UNION ALL
  SELECT 'TEMPERATURE', '体温阈值', 36.00, 37.20, 'WARNING', '℃', 1, '体温建议参考范围 36.0-37.2' UNION ALL
  SELECT 'HEART_RATE', '心率阈值', 60.00, 100.00, 'WARNING', '次/分', 1, '心率建议参考范围 60-100'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM health_threshold t WHERE t.indicator = v.indicator AND t.deleted = 0
);

-- 权限码
INSERT INTO sys_permission (perm_code, perm_name, module, status, deleted)
SELECT v.perm_code, v.perm_name, v.module, 1, 0
FROM (
  SELECT 'health:record:list' AS perm_code, '健康记录列表' AS perm_name, 'health' AS module UNION ALL
  SELECT 'health:record:view', '健康记录详情', 'health' UNION ALL
  SELECT 'health:record:add', '健康记录新增', 'health' UNION ALL
  SELECT 'health:record:update', '健康记录修改', 'health' UNION ALL
  SELECT 'health:record:delete', '健康记录删除', 'health' UNION ALL
  SELECT 'health:warning:list', '健康预警列表', 'health' UNION ALL
  SELECT 'health:warning:view', '健康预警详情', 'health' UNION ALL
  SELECT 'health:warning:handle', '健康预警处理', 'health' UNION ALL
  SELECT 'family:health:list', '家属端健康列表', 'family' UNION ALL
  SELECT 'family:health:view', '家属端健康详情', 'family'
) v
WHERE NOT EXISTS (
  SELECT 1 FROM sys_permission p WHERE p.perm_code = v.perm_code AND p.deleted = 0
);

-- ADMIN 补全
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, p.id FROM sys_permission p
WHERE p.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.permission_id = p.id);

-- CARE_STAFF：健康记录与预警（含删除）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, p.id FROM sys_permission p
WHERE p.perm_code IN (
  'health:record:list','health:record:view','health:record:add','health:record:update','health:record:delete',
  'health:warning:list','health:warning:view','health:warning:handle'
)
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 2 AND rp.permission_id = p.id);

-- FAMILY：仅家属端健康查看；移除旧的管理端 health:list/view/add
DELETE rp FROM sys_role_permission rp
INNER JOIN sys_permission p ON p.id = rp.permission_id
WHERE rp.role_id = 3 AND p.perm_code IN ('health:list','health:view','health:add');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, p.id FROM sys_permission p
WHERE p.perm_code IN ('family:health:list','family:health:view')
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 3 AND rp.permission_id = p.id);
