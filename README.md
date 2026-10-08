# 社区养老服务中心照护管理系统

本科毕业设计项目：**社区养老服务中心照护管理系统的设计与实现**。

通过 Web 管理端与家属端，统一管理老人档案、健康记录与预警、照护服务与订单、护理员排班/请假/考勤、用药提醒、周菜单与活动、模拟支付、服务评价、运营统计与操作日志。

## 技术栈

| 端 | 技术 |
|----|------|
| 后端 | Java、Spring Boot、Spring Security、JWT、MyBatis-Plus、MySQL、Redis、Maven |
| 前端 | Vue 3、Vite、Element Plus、Axios、Vue Router、Pinia、ECharts |
| 数据库 | MySQL（`sql/schema` + `sql/data`） |

## 项目结构

```text
yanglao/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue3 管理端 / 家属端 / 护理员端
├── sql/              # 表结构与种子数据
├── docs/             # 架构与数据库文档
├── README.md
├── .gitignore
├── CLAUDE.md / AGENTS.md   # AI 协作规则（可选）
└── cursor-*.md             # 开发辅助提示（可选）
```

## 环境要求

- JDK 17+（推荐 21）
- Maven 3.8+
- Node.js 18+（建议 20+）
- MySQL 8.x
- Redis 6+

## 数据库与 Redis

默认开发配置（可用环境变量覆盖）：

| 项 | 默认值 | 环境变量 |
|----|--------|----------|
| MySQL URL | `jdbc:mysql://127.0.0.1:3306/elderly_care?...` | `MYSQL_URL` |
| MySQL 用户 | `root` | `MYSQL_USERNAME` |
| MySQL 密码 | 空 | `MYSQL_PASSWORD` |
| Redis | `127.0.0.1:6379` / db `0` | `REDIS_HOST` / `REDIS_PORT` / `REDIS_DATABASE` |
| JWT 密钥 | 仅开发默认值 | `ELDERCARE_JWT_SECRET`（生产必改） |
| 服务端口 | `8080` | `SERVER_PORT` |

### SQL 初始化

在 MySQL 中创建库 `elderly_care` 后，按文件名顺序执行：

1. `sql/schema/` 下全部脚本（`01` → `15`）
2. `sql/data/` 下全部种子脚本（`01` → `16`）

详见 `sql/README.md`。

## 后端启动

```bash
cd backend
mvn test
mvn spring-boot:run
```

默认 API：`http://127.0.0.1:8080/api`

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

开发地址：`http://127.0.0.1:5173`  
（Vite 已将 `/api` 代理到后端 `8080`）

生产构建：

```bash
cd frontend
npm run build
```

## 默认演示账号

（密码以 `sql/data/01_seed_rbac.sql` 注释为准，常见开发种子如下）

| 角色 | 用户名 | 密码 | 入口 |
|------|--------|------|------|
| 管理员 ADMIN | `admin` | `Admin@123` | `/admin` |
| 护理员 CARE_STAFF | `care01` | `Care@123` | `/staff` |
| 家属 FAMILY | `family01` | `Family@123` | `/family` |

## 三种角色说明

- **管理员**：档案、健康、服务、排班审批、用药/菜单/活动、用户与权限、统计与操作日志。
- **家属**：查看绑定老人、健康与预警、预约服务与订单支付、评价、膳食与活动。
- **护理员**：我的服务执行、排班、请假、考勤、今日膳食与活动。

## 说明

本仓库为第 6–7 周迭代交付版本：功能补全与 UI/UX 优化后的可运行软件版本；原则上不再增加新业务功能。
