-- P2 健康记录 + 异常预警表
USE elderly_care;

CREATE TABLE IF NOT EXISTS health_threshold (
  id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  indicator     VARCHAR(64)   NOT NULL COMMENT '指标：SYSTOLIC_PRESSURE/DIASTOLIC_PRESSURE/BLOOD_GLUCOSE/TEMPERATURE/HEART_RATE',
  rule_name     VARCHAR(128)  NOT NULL COMMENT '规则名称',
  min_value     DECIMAL(10,2) DEFAULT NULL COMMENT '下限（含），空表示不限',
  max_value     DECIMAL(10,2) DEFAULT NULL COMMENT '上限（含），空表示不限',
  warning_level VARCHAR(32)   NOT NULL DEFAULT 'WARNING' COMMENT '预警级别：WARNING/CRITICAL',
  unit          VARCHAR(32)   DEFAULT NULL COMMENT '单位',
  status        TINYINT       NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  description   VARCHAR(255)  DEFAULT NULL COMMENT '说明（辅助提醒，非医疗诊断）',
  deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_health_threshold_indicator (indicator),
  KEY idx_health_threshold_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康指标阈值规则（辅助提醒，非医疗诊断）';

CREATE TABLE IF NOT EXISTS health_record (
  id                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id           BIGINT        NOT NULL COMMENT '老人ID',
  measured_at        DATETIME      NOT NULL COMMENT '测量时间',
  systolic_pressure  INT           DEFAULT NULL COMMENT '收缩压 mmHg',
  diastolic_pressure INT           DEFAULT NULL COMMENT '舒张压 mmHg',
  blood_glucose      DECIMAL(6,2)  DEFAULT NULL COMMENT '血糖 mmol/L',
  body_temperature   DECIMAL(4,1)  DEFAULT NULL COMMENT '体温 ℃',
  heart_rate         INT           DEFAULT NULL COMMENT '心率 次/分',
  recorded_by        BIGINT        DEFAULT NULL COMMENT '录入人用户ID',
  remark             VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  deleted            TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_health_record_elder (elder_id),
  KEY idx_health_record_measured (measured_at),
  CONSTRAINT fk_health_record_elder FOREIGN KEY (elder_id) REFERENCES elder (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康记录';

CREATE TABLE IF NOT EXISTS health_warning (
  id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id         BIGINT        NOT NULL COMMENT '老人ID',
  health_record_id BIGINT        NOT NULL COMMENT '关联健康记录',
  indicator        VARCHAR(64)   NOT NULL COMMENT '异常指标',
  actual_value     VARCHAR(64)   NOT NULL COMMENT '实际值',
  threshold_desc   VARCHAR(128)  NOT NULL COMMENT '阈值描述',
  direction        VARCHAR(16)   NOT NULL COMMENT 'HIGH/LOW',
  warning_level    VARCHAR(32)   NOT NULL DEFAULT 'WARNING' COMMENT 'WARNING/CRITICAL',
  status           VARCHAR(32)   NOT NULL DEFAULT 'UNHANDLED' COMMENT 'UNHANDLED/HANDLED',
  generated_at     DATETIME      NOT NULL COMMENT '生成时间',
  handled_by       BIGINT        DEFAULT NULL COMMENT '处理人',
  handled_at       DATETIME      DEFAULT NULL COMMENT '处理时间',
  handling_result  VARCHAR(500)  DEFAULT NULL COMMENT '处理备注',
  deleted          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_health_warning_elder (elder_id),
  KEY idx_health_warning_record (health_record_id),
  KEY idx_health_warning_status (status),
  CONSTRAINT fk_health_warning_elder FOREIGN KEY (elder_id) REFERENCES elder (id),
  CONSTRAINT fk_health_warning_record FOREIGN KEY (health_record_id) REFERENCES health_record (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康异常预警（辅助提醒，非医疗诊断）';
