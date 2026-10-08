-- P7 服务评价表
USE elderly_care;

CREATE TABLE IF NOT EXISTS care_evaluation (
  id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  service_order_id  BIGINT       NOT NULL COMMENT '服务订单ID',
  elder_id          BIGINT       NOT NULL COMMENT '老人ID(冗余自订单，防篡改)',
  family_user_id    BIGINT       NOT NULL COMMENT '评价家属用户ID(JWT)',
  score             INT          NOT NULL COMMENT '评分1-5',
  content           VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
  deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  -- 物理唯一：同一订单仅允许一条评价记录（含已逻辑删除），P7 不提供删除/重评
  UNIQUE KEY uk_care_evaluation_order (service_order_id),
  KEY idx_care_evaluation_elder (elder_id),
  KEY idx_care_evaluation_family (family_user_id),
  CONSTRAINT fk_care_evaluation_order FOREIGN KEY (service_order_id) REFERENCES care_service_order (id),
  CONSTRAINT fk_care_evaluation_elder FOREIGN KEY (elder_id) REFERENCES elder (id),
  CONSTRAINT fk_care_evaluation_family FOREIGN KEY (family_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务评价';
