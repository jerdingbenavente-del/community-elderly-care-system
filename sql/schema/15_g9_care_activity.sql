-- G9 社区活动管理（无报名/收费/签到）
USE elderly_care;

CREATE TABLE IF NOT EXISTS care_activity (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  activity_name   VARCHAR(100) NOT NULL COMMENT '活动名称',
  activity_date   DATE         NOT NULL COMMENT '活动日期',
  start_time      TIME         NOT NULL COMMENT '开始时间',
  end_time        TIME         NOT NULL COMMENT '结束时间',
  location        VARCHAR(200) NOT NULL COMMENT '地点',
  description     VARCHAR(1000) DEFAULT NULL COMMENT '简介',
  status          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/CANCELLED/COMPLETED',
  created_by      BIGINT       DEFAULT NULL COMMENT '创建人',
  deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_activity_date (activity_date),
  KEY idx_activity_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社区活动';
