-- G6 护理员基础考勤（与服务订单执行签到分离）
USE elderly_care;

CREATE TABLE IF NOT EXISTS care_staff_attendance (
  id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  care_staff_id    BIGINT       NOT NULL COMMENT '护理员ID',
  attendance_date  DATE         NOT NULL COMMENT '考勤日期',
  check_in_time    DATETIME     NOT NULL COMMENT '上班签到时间（服务器时间）',
  check_out_time   DATETIME     DEFAULT NULL COMMENT '下班签退时间（服务器时间）',
  status           VARCHAR(32)  NOT NULL COMMENT 'WORKING/COMPLETED',
  remark           VARCHAR(500) DEFAULT NULL COMMENT '备注',
  deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_attendance_staff_date (care_staff_id, attendance_date),
  KEY idx_attendance_date (attendance_date),
  KEY idx_attendance_status (status),
  CONSTRAINT fk_attendance_care_staff FOREIGN KEY (care_staff_id) REFERENCES care_staff (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理员上下班考勤';
