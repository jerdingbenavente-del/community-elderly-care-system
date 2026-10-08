-- G1 个人中心：sys_user 增加头像字段（最小 ALTER，不重建表）
USE elderly_care;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'elderly_care'
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'avatar'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE sys_user ADD COLUMN avatar VARCHAR(255) DEFAULT NULL COMMENT ''头像相对路径，如 /uploads/avatar/xxx.jpg'' AFTER phone',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
