-- G2 模拟支付：订单金额快照 + 支付状态（不改历史 schema，不改订单五态）
USE elderly_care;

SET @col_amount := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'elderly_care'
    AND TABLE_NAME = 'care_service_order'
    AND COLUMN_NAME = 'amount'
);
SET @sql_amount := IF(@col_amount = 0,
  'ALTER TABLE care_service_order ADD COLUMN amount DECIMAL(10,2) DEFAULT NULL COMMENT ''订单金额快照（创建时取服务项目价格）'' AFTER status',
  'SELECT 1');
PREPARE stmt FROM @sql_amount; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_pay := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'elderly_care'
    AND TABLE_NAME = 'care_service_order'
    AND COLUMN_NAME = 'payment_status'
);
SET @sql_pay := IF(@col_pay = 0,
  'ALTER TABLE care_service_order ADD COLUMN payment_status VARCHAR(32) NOT NULL DEFAULT ''UNPAID'' COMMENT ''支付状态：UNPAID/PAID'' AFTER amount',
  'SELECT 1');
PREPARE stmt FROM @sql_pay; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_paid_at := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'elderly_care'
    AND TABLE_NAME = 'care_service_order'
    AND COLUMN_NAME = 'paid_at'
);
SET @sql_paid_at := IF(@col_paid_at = 0,
  'ALTER TABLE care_service_order ADD COLUMN paid_at DATETIME DEFAULT NULL COMMENT ''模拟支付完成时间'' AFTER payment_status',
  'SELECT 1');
PREPARE stmt FROM @sql_paid_at; EXECUTE stmt; DEALLOCATE PREPARE stmt;
