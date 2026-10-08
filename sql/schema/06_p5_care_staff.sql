-- P5 护理员管理增量（最小 ALTER，不重建表）
USE elderly_care;

-- 工号：旧数据允许先为空，随后用种子回填；UNIQUE 允许多个 NULL
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'elderly_care' AND TABLE_NAME = 'care_staff' AND COLUMN_NAME = 'employee_no'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE care_staff ADD COLUMN employee_no VARCHAR(32) DEFAULT NULL COMMENT ''工号'' AFTER user_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'elderly_care' AND TABLE_NAME = 'care_staff' AND COLUMN_NAME = 'gender'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE care_staff ADD COLUMN gender TINYINT DEFAULT NULL COMMENT ''性别：1男 2女'' AFTER name',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'elderly_care' AND TABLE_NAME = 'care_staff' AND COLUMN_NAME = 'remark'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE care_staff ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT ''备注'' AFTER position',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = 'elderly_care' AND TABLE_NAME = 'care_staff' AND INDEX_NAME = 'uk_care_staff_employee_no'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE care_staff ADD UNIQUE KEY uk_care_staff_employee_no (employee_no)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
