-- 社区养老服务中心照护管理系统
-- 第一阶段建表：系统权限域 + 老人基础域
-- 数据库：elderly_care

CREATE DATABASE IF NOT EXISTS elderly_care
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE elderly_care;

-- ----------------------------
-- 系统用户
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(64)  NOT NULL COMMENT '登录用户名',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
  real_name     VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
  phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ----------------------------
-- 角色
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_code  VARCHAR(64)  NOT NULL COMMENT '角色编码',
  role_name  VARCHAR(64)  NOT NULL COMMENT '角色名称',
  status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  remark     VARCHAR(255) DEFAULT NULL COMMENT '备注',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

-- ----------------------------
-- 权限
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_permission (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  perm_code  VARCHAR(128) NOT NULL COMMENT '权限码，如 elder:view',
  perm_name  VARCHAR(128) NOT NULL COMMENT '权限名称',
  module     VARCHAR(64)  DEFAULT NULL COMMENT '所属模块',
  status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_permission_code (perm_code),
  KEY idx_sys_permission_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限';

-- ----------------------------
-- 用户-角色
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
  id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_role (role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- ----------------------------
-- 角色-权限
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
  id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_id       BIGINT NOT NULL COMMENT '角色ID',
  permission_id BIGINT NOT NULL COMMENT '权限ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_permission (role_id, permission_id),
  KEY idx_sys_role_permission_perm (permission_id),
  CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
  CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';

-- ----------------------------
-- 操作日志（不做逻辑删除）
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_operation_log (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id        BIGINT       DEFAULT NULL COMMENT '操作用户ID',
  username       VARCHAR(64)  DEFAULT NULL COMMENT '操作用户名',
  module         VARCHAR(64)  DEFAULT NULL COMMENT '模块',
  operation      VARCHAR(128) NOT NULL COMMENT '操作类型',
  target_type    VARCHAR(64)  DEFAULT NULL COMMENT '对象类型',
  target_id      VARCHAR(64)  DEFAULT NULL COMMENT '对象ID',
  request_method VARCHAR(16)  DEFAULT NULL COMMENT 'HTTP方法',
  request_uri    VARCHAR(255) DEFAULT NULL COMMENT '请求URI',
  request_ip     VARCHAR(64)  DEFAULT NULL COMMENT '请求IP',
  result         VARCHAR(32)  NOT NULL COMMENT '结果：SUCCESS/FAIL',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_sys_operation_log_user (user_id),
  KEY idx_sys_operation_log_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ----------------------------
-- 老人主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS elder (
  id                        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name                      VARCHAR(64)  NOT NULL COMMENT '姓名',
  gender                    TINYINT      DEFAULT NULL COMMENT '性别：1男 2女',
  birth_date                DATE         DEFAULT NULL COMMENT '出生日期',
  phone                     VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  address                   VARCHAR(255) DEFAULT NULL COMMENT '住址',
  id_card                   VARCHAR(32)  DEFAULT NULL COMMENT '身份证号',
  care_level                VARCHAR(32)  DEFAULT NULL COMMENT '护理等级',
  status                    TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1在册 0退住',
  registered_at             DATE         DEFAULT NULL COMMENT '登记日期',
  medical_history           VARCHAR(1000) DEFAULT NULL COMMENT '病史',
  allergy_history           VARCHAR(1000) DEFAULT NULL COMMENT '过敏史',
  special_care_requirement  VARCHAR(1000) DEFAULT NULL COMMENT '特殊照护需求',
  remark                    VARCHAR(500) DEFAULT NULL COMMENT '备注',
  deleted                   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_elder_name (name),
  KEY idx_elder_phone (phone),
  KEY idx_elder_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人档案';

-- ----------------------------
-- 老人-家属绑定（家属数据权限关键）
-- ----------------------------
CREATE TABLE IF NOT EXISTS elder_family (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id       BIGINT      NOT NULL COMMENT '老人ID',
  family_user_id BIGINT      NOT NULL COMMENT '家属用户ID',
  relationship   VARCHAR(32) DEFAULT NULL COMMENT '与老人关系',
  is_primary     TINYINT     NOT NULL DEFAULT 0 COMMENT '是否主要联系人：1是 0否',
  status         TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1有效 0无效',
  deleted        TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_elder_family (elder_id, family_user_id),
  KEY idx_elder_family_user (family_user_id),
  CONSTRAINT fk_elder_family_elder FOREIGN KEY (elder_id) REFERENCES elder (id),
  CONSTRAINT fk_elder_family_user FOREIGN KEY (family_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人家属绑定';

-- ----------------------------
-- 紧急联系人
-- ----------------------------
CREATE TABLE IF NOT EXISTS emergency_contact (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  elder_id     BIGINT       NOT NULL COMMENT '老人ID',
  name         VARCHAR(64)  NOT NULL COMMENT '联系人姓名',
  relationship VARCHAR(32)  DEFAULT NULL COMMENT '关系',
  phone        VARCHAR(20)  NOT NULL COMMENT '电话',
  priority     INT          NOT NULL DEFAULT 1 COMMENT '优先级，数字越小越优先',
  remark       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_emergency_contact_elder (elder_id),
  CONSTRAINT fk_emergency_contact_elder FOREIGN KEY (elder_id) REFERENCES elder (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='紧急联系人';

-- ----------------------------
-- 照护人员
-- ----------------------------
CREATE TABLE IF NOT EXISTS care_staff (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id    BIGINT       DEFAULT NULL COMMENT '关联系统用户ID',
  name       VARCHAR(64)  NOT NULL COMMENT '姓名',
  phone      VARCHAR(20)  DEFAULT NULL COMMENT '电话',
  position   VARCHAR(64)  DEFAULT NULL COMMENT '岗位',
  status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1在职 0离职',
  deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_care_staff_user (user_id),
  KEY idx_care_staff_status (status),
  CONSTRAINT fk_care_staff_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照护人员';
