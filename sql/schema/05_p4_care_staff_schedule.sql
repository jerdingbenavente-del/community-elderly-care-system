-- P4 护理员排班
USE elderly_care;

CREATE TABLE IF NOT EXISTS care_staff_schedule (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  care_staff_id   BIGINT       NOT NULL COMMENT '护理员ID',
  schedule_date   DATE         NOT NULL COMMENT '排班日期',
  start_time      TIME         NOT NULL COMMENT '班次开始',
  end_time        TIME         NOT NULL COMMENT '班次结束',
  status          VARCHAR(32)  NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE/CANCELLED',
  remark          VARCHAR(500) DEFAULT NULL COMMENT '备注',
  deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_care_schedule_staff (care_staff_id),
  KEY idx_care_schedule_date (schedule_date),
  KEY idx_care_schedule_status (status),
  KEY idx_care_schedule_staff_date (care_staff_id, schedule_date),
  CONSTRAINT fk_care_schedule_staff FOREIGN KEY (care_staff_id) REFERENCES care_staff (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理员排班';
