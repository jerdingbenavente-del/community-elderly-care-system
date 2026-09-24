# 社区养老服务中心照护管理系统 —— 后端

## 技术栈

- Java 17
- Spring Boot 3.3.x
- Maven
- MyBatis-Plus
- Spring Security + JWT
- MySQL 8.x
- Redis

## 当前进度

已完成第二阶段：

- 第一阶段数据库表（见 `../sql`）
- RBAC 种子数据
- JWT 登录鉴权
- 操作日志（登录成功/失败）
- 系统用户只读列表（用于权限联调）

尚未实现：老人 CRUD、健康、照护、排班、餐饮、活动、用药、统计、前端、小程序。

## 本地准备

### 1. 导入 SQL

```bash
mysql -uroot --default-character-set=utf8mb4 < ../sql/schema/01_phase1_tables.sql
mysql -uroot --default-character-set=utf8mb4 < ../sql/data/01_seed_rbac.sql
```

PowerShell 可用：

```powershell
Get-Content ..\sql\schema\01_phase1_tables.sql -Raw -Encoding UTF8 | mysql -uroot --default-character-set=utf8mb4
Get-Content ..\sql\data\01_seed_rbac.sql -Raw -Encoding UTF8 | mysql -uroot --default-character-set=utf8mb4
```

### 2. 配置

修改 `application-dev.yml`，或使用环境变量：

| 变量 | 说明 |
|---|---|
| `MYSQL_URL` | JDBC URL |
| `MYSQL_USERNAME` | 数据库用户 |
| `MYSQL_PASSWORD` | 数据库密码 |
| `REDIS_HOST` / `REDIS_PORT` | Redis |
| `ELDERCARE_JWT_SECRET` | JWT 密钥（生产必改） |

### 3. 启动

```bash
mvn -DskipTests package
mvn spring-boot:run
```

## 开发环境测试账号（仅本地）

| 用户名 | 密码 | 角色 | 用途 |
|---|---|---|---|
| `admin` | `Admin@123` | ADMIN | 管理员联调 |
| `care01` | `Care@123` | CARE_STAFF | 照护人员权限边界测试 |
| `family01` | `Family@123` | FAMILY | 家属角色测试 |

数据库中仅保存 BCrypt 哈希，无明文密码。

Dashboard 统计权限种子（幂等，可重复执行）：

```bash
mysql -uroot elderly_care < ../sql/data/10_fa10_dashboard_statistics_seed.sql
```

将为 ADMIN 写入 `dashboard:statistics:view`（接口仍以 ADMIN 角色兜底校验）。

## 主要接口

```text
POST /api/auth/login
GET  /api/auth/me
GET  /api/system/users   # 需要 system:user:list
GET  /api/health
```

登录示例：

```bash
curl -X POST http://127.0.0.1:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"Admin@123\"}"
```
