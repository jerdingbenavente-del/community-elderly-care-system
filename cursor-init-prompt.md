# Cursor 项目初始化扫描与开工指令

你现在作为本项目的主 Coding Agent。

项目：
**社区养老服务中心照护管理系统的设计与实现**

## 最高要求

在执行任何代码修改前，必须先阅读：

1. `CLAUDE.md`
2. `AGENTS.md`
3. `docs/architecture.md`
4. `docs/database.md`

这些文件是项目 AI 开发规则和设计基线。

---

# 第一阶段：只扫描，不修改

暂时不要修改任何代码、数据库和配置。

请检查整个项目：

## 1. 项目结构

识别：

- 后端目录
- Web 前端目录
- 微信小程序目录
- SQL 目录
- 配置文件
- 文档目录
- 测试目录

## 2. 技术栈

确认实际使用：

- Java / Spring Boot 版本
- Maven / Gradle
- MyBatis / MyBatis-Plus
- Vue / Vue3
- Vite
- Element Plus
- Axios
- Pinia
- MySQL
- Redis
- JWT
- 微信小程序框架

不要根据文档猜，以实际代码和配置为准。

## 3. 后端扫描

检查：

- Controller
- Service
- Mapper
- Entity
- DTO
- VO
- Config
- Security
- JWT
- Exception
- Common
- Utils

找出：

- 统一响应格式
- 统一异常处理
- 登录流程
- 权限实现
- 分页方式
- 参数校验方式
- 日志实现方式

## 4. 前端扫描

检查：

- router
- store
- api
- views
- components
- layouts
- 权限指令
- Axios 拦截器
- 登录状态

## 5. 数据库扫描

重点读取：

- 所有 SQL
- 数据库配置
- Entity
- Mapper
- XML

输出：

```text
现有数据库表
表用途
主键
核心字段
表之间关系
索引
唯一约束
```

## 6. 模块完成度

按照下面 12 个模块逐项判断：

1. 老人档案管理
2. 健康记录管理
3. 健康异常预警
4. 照护服务项目管理
5. 照护人员排班
6. 服务记录与评价
7. 家属信息查看
8. 餐饮管理
9. 活动管理
10. 药品管理与提醒
11. 系统权限与日志
12. 统计报表

每个模块标记：

```text
未开始
基础完成
部分完成
基本完成
存在严重问题
```

---

# 第二阶段：一致性检查

比较：

```text
CLAUDE.md
AGENTS.md
docs/architecture.md
docs/database.md
实际代码
实际 SQL
```

找出：

- 文档与代码冲突
- 文档与数据库冲突
- API 与前端调用不一致
- Entity 与 SQL 不一致
- 权限设计缺失
- Mock 数据
- 写死统计
- 假接口
- 假按钮
- 安全风险
- 明显重复代码
- 未完成但看起来完成的功能

---

# 第三阶段：数据库 ER 设计

在扫描完成后，根据**实际数据库 + 需求**设计最终 ER 模型。

核心实体至少考虑：

```text
User
Role
Permission
UserRole
RolePermission
Elder
ElderFamily
EmergencyContact
CareStaff
HealthRecord
HealthWarningRule
HealthWarning
CareService
CareOrder
CareRecord
CareEvaluation
StaffSchedule
ScheduleChangeRequest
MealMenu
MealOrder
DietRequirement
Activity
ActivityRegistration
ActivityCheckin
MedicinePlan
MedicineRecord
OperationLog
```

如果实际项目已有等价表：

> 必须复用，不得创建同义重复表。

输出：

```text
实体
主键
核心字段
一对一/一对多/多对多
唯一约束
重要索引
删除策略
状态字段
```

---

# 第四阶段：输出实施计划

最后输出一份按优先级排列的开发计划：

## P0 基础设施

- 数据库
- 登录
- JWT
- RBAC
- 统一响应
- 全局异常
- 日志

## P1 核心业务

- 老人档案
- 健康记录
- 健康预警
- 服务项目
- 排班
- 服务记录

## P2 协同业务

- 家属端
- 活动
- 餐饮
- 药品提醒

## P3 数据分析

- 统计报表
- ECharts
- 首页 Dashboard

---

# 第五阶段：给出第一批任务

请推荐最合理的第一个开发任务。

原则：

```text
依赖最少
收益最大
后续模块复用最多
风险最低
```

---

# 最终输出格式

```markdown
# 项目扫描报告

## 1. 项目结构

## 2. 实际技术栈

## 3. 后端现状

## 4. 前端现状

## 5. 数据库现状

## 6. 登录与权限现状

## 7. 12 个模块完成度

## 8. 文档与代码冲突

## 9. 数据库 ER 设计

## 10. P0-P3 开发路线

## 11. 风险清单

## 12. 推荐第一开发任务
```

再次强调：

**本轮只扫描和分析，不修改代码。**
