-- G8 公共周菜单、老人饮食备注、老人餐次个性化调整（三套数据互不覆盖）
USE elderly_care;

CREATE TABLE IF NOT EXISTS weekly_menu (
  id               BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  week_start_date  DATE        NOT NULL COMMENT '周一',
  week_end_date    DATE        NOT NULL COMMENT '周日',
  status           VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED',
  created_by       BIGINT      DEFAULT NULL COMMENT '创建人',
  deleted          TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_weekly_menu_start (week_start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公共周菜单';

CREATE TABLE IF NOT EXISTS weekly_menu_item (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  weekly_menu_id  BIGINT       NOT NULL COMMENT '周菜单ID',
  menu_date       DATE         NOT NULL COMMENT '日期',
  meal_type       VARCHAR(32)  NOT NULL COMMENT 'BREAKFAST/LUNCH/DINNER',
  dish_name       VARCHAR(200) NOT NULL COMMENT '菜品',
  description     VARCHAR(500) DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (id),
  KEY idx_menu_item_menu (weekly_menu_id),
  KEY idx_menu_item_date (menu_date, meal_type),
  CONSTRAINT fk_menu_item_week FOREIGN KEY (weekly_menu_id) REFERENCES weekly_menu (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公共周菜单餐次';

CREATE TABLE IF NOT EXISTS elder_dietary_note (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id    BIGINT        NOT NULL COMMENT '老人ID',
  note        VARCHAR(1000) NOT NULL COMMENT '家属饮食备注',
  status      VARCHAR(32)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
  updated_by  BIGINT        DEFAULT NULL COMMENT '最后修改人',
  deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dietary_elder (elder_id),
  CONSTRAINT fk_dietary_elder FOREIGN KEY (elder_id) REFERENCES elder (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人饮食备注';

CREATE TABLE IF NOT EXISTS elder_meal_adjustment (
  id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id          BIGINT       NOT NULL COMMENT '老人ID',
  menu_date         DATE         NOT NULL COMMENT '日期',
  meal_type         VARCHAR(32)  NOT NULL COMMENT 'BREAKFAST/LUNCH/DINNER',
  adjusted_content  VARCHAR(200) NOT NULL COMMENT '该老人专属菜品',
  reason            VARCHAR(500) DEFAULT NULL COMMENT '调整原因',
  status            VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CANCELLED',
  created_by        BIGINT       DEFAULT NULL COMMENT '创建人',
  updated_by        BIGINT       DEFAULT NULL COMMENT '修改人',
  deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_meal_adjust (elder_id, menu_date, meal_type),
  CONSTRAINT fk_meal_adjust_elder FOREIGN KEY (elder_id) REFERENCES elder (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人个性化餐次调整';
