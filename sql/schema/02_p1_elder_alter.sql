-- P1 老人档案：对已有库的增量变更
USE elderly_care;

-- 登记日期（入住/登记），表已存在则幂等添加
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'elderly_care'
    AND TABLE_NAME = 'elder'
    AND COLUMN_NAME = 'registered_at'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE elder ADD COLUMN registered_at DATE NULL COMMENT ''登记日期'' AFTER status',
  'SELECT ''registered_at already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
