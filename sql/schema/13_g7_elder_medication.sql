-- G7 老人用药计划 + 每日多个服药时刻
USE elderly_care;

CREATE TABLE IF NOT EXISTS elder_medication (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id       BIGINT        NOT NULL COMMENT '老人ID',
  medicine_name  VARCHAR(100)  NOT NULL COMMENT '药品名称',
  dosage         VARCHAR(32)   NOT NULL COMMENT '剂量',
  dosage_unit    VARCHAR(16)   NOT NULL COMMENT '剂量单位',
  usage_method   VARCHAR(32)   NOT NULL COMMENT '服用方式',
  start_date     DATE          NOT NULL COMMENT '开始日期（含）',
  end_date       DATE          NOT NULL COMMENT '结束日期（含）',
  status         VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  remark         VARCHAR(500)  DEFAULT NULL COMMENT '备注',
  created_by     BIGINT        DEFAULT NULL COMMENT '创建人 sys_user.id',
  deleted        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_med_elder (elder_id),
  KEY idx_med_status (status),
  KEY idx_med_dates (start_date, end_date),
  CONSTRAINT fk_med_elder FOREIGN KEY (elder_id) REFERENCES elder (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人用药计划';

CREATE TABLE IF NOT EXISTS elder_medication_time (
  id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  medication_id   BIGINT NOT NULL COMMENT '用药计划ID',
  dose_time       TIME   NOT NULL COMMENT '服药时刻',
  PRIMARY KEY (id),
  KEY idx_med_time_plan (medication_id),
  CONSTRAINT fk_med_time_plan FOREIGN KEY (medication_id) REFERENCES elder_medication (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药计划服药时刻';
