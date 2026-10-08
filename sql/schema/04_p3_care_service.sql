-- P3 照护服务项目 + 服务订单
USE elderly_care;

CREATE TABLE IF NOT EXISTS care_service_item (
  id               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
  service_code     VARCHAR(64)    NOT NULL COMMENT '服务编码',
  service_name     VARCHAR(128)   NOT NULL COMMENT '服务名称',
  service_type     VARCHAR(64)    DEFAULT NULL COMMENT '服务类型',
  description      VARCHAR(500)   DEFAULT NULL COMMENT '描述',
  duration_minutes INT            NOT NULL COMMENT '时长(分钟)',
  price            DECIMAL(10,2)  DEFAULT NULL COMMENT '参考价格(不实现支付)',
  status           VARCHAR(32)    NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
  deleted          TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_care_service_item_code (service_code),
  KEY idx_care_service_item_status (status),
  KEY idx_care_service_item_name (service_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照护服务项目';

CREATE TABLE IF NOT EXISTS care_service_order (
  id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no              VARCHAR(64)  NOT NULL COMMENT '订单号',
  elder_id              BIGINT       NOT NULL COMMENT '老人ID',
  service_item_id       BIGINT       NOT NULL COMMENT '服务项目ID',
  scheduled_start_time  DATETIME     NOT NULL COMMENT '预约开始时间',
  scheduled_end_time    DATETIME     NOT NULL COMMENT '预约结束时间',
  care_staff_id         BIGINT       DEFAULT NULL COMMENT '执行护理人员ID(可空，无排班)',
  status                VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONFIRMED/IN_SERVICE/COMPLETED/CANCELLED',
  remark                VARCHAR(500) DEFAULT NULL COMMENT '备注',
  cancel_reason         VARCHAR(500) DEFAULT NULL COMMENT '取消原因',
  created_by            BIGINT       DEFAULT NULL COMMENT '创建人',
  completed_at          DATETIME     DEFAULT NULL COMMENT '完成时间',
  cancelled_at          DATETIME     DEFAULT NULL COMMENT '取消时间',
  deleted               TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_care_service_order_no (order_no),
  KEY idx_care_order_elder (elder_id),
  KEY idx_care_order_item (service_item_id),
  KEY idx_care_order_staff (care_staff_id),
  KEY idx_care_order_status (status),
  KEY idx_care_order_start (scheduled_start_time),
  CONSTRAINT fk_care_order_elder FOREIGN KEY (elder_id) REFERENCES elder (id),
  CONSTRAINT fk_care_order_item FOREIGN KEY (service_item_id) REFERENCES care_service_item (id),
  CONSTRAINT fk_care_order_staff FOREIGN KEY (care_staff_id) REFERENCES care_staff (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照护服务订单/预约';
