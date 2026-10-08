-- G5 护理员临时无法值班申请
USE elderly_care;

CREATE TABLE IF NOT EXISTS care_staff_leave_application (
  id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  care_staff_id         BIGINT       NOT NULL COMMENT '护理员ID',
  start_time            DATETIME     NOT NULL COMMENT '请假开始时间',
  end_time              DATETIME     NOT NULL COMMENT '请假结束时间',
  reason                VARCHAR(500) NOT NULL COMMENT '原因',
  status                VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  reviewed_by           BIGINT       DEFAULT NULL COMMENT '审批人 sys_user.id',
  reviewed_at           DATETIME     DEFAULT NULL COMMENT '审批时间',
  review_remark         VARCHAR(500) DEFAULT NULL COMMENT '审批备注',
  affected_order_count  INT          NOT NULL DEFAULT 0 COMMENT '审批通过时解除护理员的订单数',
  deleted               TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_leave_staff (care_staff_id),
  KEY idx_leave_status (status),
  KEY idx_leave_start (start_time),
  KEY idx_leave_end (end_time),
  CONSTRAINT fk_leave_care_staff FOREIGN KEY (care_staff_id) REFERENCES care_staff (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理员临时无法值班申请';
