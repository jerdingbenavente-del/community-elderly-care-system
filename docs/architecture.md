# docs/architecture.md

# 社区养老服务中心照护管理系统 —— 系统架构设计

## 1. 架构目标

系统采用前后端分离架构：

```text
                    ┌──────────────────────┐
                    │      Web 管理端       │
                    │ Vue3 + Element Plus  │
                    └──────────┬───────────┘
                               │ REST API
                               ↓
                    ┌──────────────────────┐
                    │      Spring Boot      │
                    │ Controller / Service  │
                    │ Mapper / Security     │
                    └───────┬────────┬──────┘
                            │        │
                     ┌──────┘        └──────┐
                     ↓                      ↓
              ┌─────────────┐        ┌───────────┐
              │    MySQL    │        │   Redis   │
              │ 核心业务数据 │        │ 缓存/提醒 │
              └─────────────┘        └───────────┘

                    ↑ REST API
                    │
           ┌────────┴─────────┐
           │ 微信小程序家属端  │
           └──────────────────┘
```

---

# 2. 分层原则

## Controller

职责：

- 接收请求
- 参数校验
- 权限入口
- 调用 Service
- 返回统一结果

禁止在 Controller 编写复杂业务。

---

## Service

职责：

- 核心业务逻辑
- 状态转换
- 数据权限
- 事务
- 重复操作判断
- 业务异常

---

## Mapper

职责：

- 数据库访问
- 查询
- 新增
- 修改
- 删除

---

## Entity

对应数据库持久化模型。

---

## DTO

请求参数模型。

---

## VO

返回给前端的展示模型。

---

# 3. 推荐后端目录

```text
backend/
└── src/main/java/...
    ├── controller/
    ├── service/
    │   └── impl/
    ├── mapper/
    ├── entity/
    ├── dto/
    ├── vo/
    ├── config/
    ├── security/
    ├── exception/
    ├── common/
    └── utils/
```

具体包名以现有项目为准。

---

# 4. 推荐前端目录

```text
frontend/
└── src/
    ├── api/
    ├── assets/
    ├── components/
    ├── layouts/
    ├── router/
    ├── stores/
    ├── utils/
    └── views/
        ├── dashboard/
        ├── elder/
        ├── health/
        ├── warning/
        ├── service/
        ├── schedule/
        ├── activity/
        ├── meal/
        ├── medicine/
        ├── system/
        └── report/
```

---

# 5. 家属端架构

```text
微信小程序
   ↓
登录/身份
   ↓
JWT/会话
   ↓
家属身份
   ↓
老人绑定关系
   ↓
数据查询
```

家属端只能查看授权老人。

---

# 6. 核心业务域

## 老人域

```text
elder
elder_family
emergency_contact
```

## 健康域

```text
health_record
health_warning
health_warning_rule
```

## 照护域

```text
care_service
care_order
care_record
care_evaluation
```

## 排班域

```text
care_staff
staff_schedule
schedule_change_request
```

## 餐饮域

```text
meal_menu
meal_order
diet_requirement
```

## 活动域

```text
activity
activity_registration
activity_checkin
```

## 用药域

```text
medicine_plan
medicine_reminder
medicine_record
```

## 系统域

```text
user
role
permission
user_role
role_permission
operation_log
```

---

# 7. 业务依赖关系

```text
用户/权限
   ↓
老人档案
   ↓
健康 ──→ 预警
   ↓
照护服务 ←→ 排班
   ↓
服务记录 → 评价

老人
 ├── 家属
 ├── 餐饮
 ├── 活动
 └── 用药
```

---

# 8. 关键业务闭环

## 健康

```text
健康数据
 ↓
异常判断
 ↓
预警
 ↓
处理
 ↓
关闭
```

## 服务

```text
服务项目
 ↓
预约
 ↓
排班
 ↓
签到
 ↓
服务记录
 ↓
评价
```

## 活动

```text
活动
 ↓
发布
 ↓
报名
 ↓
签到
 ↓
活动记录
```

---

# 9. 事务边界

以下业务通常需要事务：

- 创建服务订单
- 服务签到
- 活动报名
- 活动签到
- 调班审核
- 健康记录 + 异常预警
- 用户角色修改

原则：

> 业务状态和关键关联记录必须保持一致。

---

# 10. Redis 架构位置

Redis 不负责最终业务数据。

```text
MySQL = 最终事实来源
Redis = 缓存/任务辅助
```

缓存失效必须与数据修改保持一致。

---

# 11. 安全架构

```text
请求
 ↓
JWT 认证
 ↓
用户
 ↓
角色
 ↓
权限
 ↓
数据权限
 ↓
Service
 ↓
数据库
```

家属必须经过老人绑定关系校验。

---

# 12. 统计架构

```text
业务表
 ↓
SQL 聚合
 ↓
ReportService
 ↓
ReportController
 ↓
ECharts
```

禁止单独维护假统计表。

---

# 13. 异常架构

```text
业务异常
 ↓
Service 抛出
 ↓
GlobalExceptionHandler
 ↓
统一响应
 ↓
前端友好提示
```

---

# 14. 日志架构

重要业务操作：

```text
业务 Service
 ↓
OperationLog
 ↓
MySQL
```

日志不得泄露密码、Token 等敏感信息。

---

# 15. 架构演进原则

毕业设计阶段优先：

```text
简单
稳定
容易理解
容易测试
容易答辩
```

不要为了理论上的高并发过度设计。

