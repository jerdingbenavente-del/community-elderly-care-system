-- P8 账号安全：首次登录强制改密标记（最小 ALTER，不重建表）
USE elderly_care;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'elderly_care'
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'must_change_password'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE sys_user ADD COLUMN must_change_password TINYINT NOT NULL DEFAULT 0 COMMENT ''是否必须修改密码：1是 0否'' AFTER status',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
