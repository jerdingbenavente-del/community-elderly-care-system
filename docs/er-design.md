# docs/er-design.md

# 社区养老服务中心照护管理系统 —— 最终 ER 设计基线

> 本文件用于 Cursor 在实际数据库落地前进行实体关系设计。
> 不允许脱离现有数据库直接创建同义重复表。

---

# 1. 总体关系

```text
User
 ├── UserRole ── Role ── RolePermission ── Permission
 ├── OperationLog
 └── ElderFamily ── Elder
                         ├── EmergencyContact
                         ├── HealthRecord ── HealthWarning
                         │                    ↑
                         │                    │
                         │             HealthWarningRule
                         │
                         ├── CareOrder ── CareRecord
                         │      │
                         │      └── CareEvaluation
                         │
                         ├── ActivityRegistration ── ActivityCheckin
                         ├── MealOrder ── MealMenu
                         ├── DietRequirement
                         └── MedicinePlan ── MedicineRecord

CareStaff
 ├── StaffSchedule
 │      └── ScheduleChangeRequest
 └── CareOrder

CareService
 └── CareOrder
```

---

# 2. 权限域

## User

系统用户。

关系：

```text
User 1:N UserRole
```

## Role

角色：

```text
ADMIN
CARE_STAFF
FAMILY
```

## Permission

权限粒度：

```text
模块:操作
```

例如：

```text
elder:view
elder:create
elder:update
health:view
health:create
warning:handle
service:view
schedule:update
report:view
```

关系：

```text
Role N:M Permission
```

通过：

```text
RolePermission
```

实现。

---

# 3. 老人域

## Elder

系统核心主实体。

```text
Elder 1:N HealthRecord
Elder 1:N CareOrder
Elder 1:N ActivityRegistration
Elder 1:N MealOrder
Elder 1:N MedicinePlan
Elder 1:N EmergencyContact
Elder 1:N DietRequirement
Elder N:M User
```

老人和用户的多对多关系通过：

```text
ElderFamily
```

---

# 4. 家属数据权限

关键关系：

```text
User
 ↓
ElderFamily
 ↓
Elder
```

所有家属端老人查询必须依据该关系进行授权。

不得：

```text
family_user_id + elder_id
```

只由前端决定。

---

# 5. 健康域

```text
Elder
  ↓ 1:N
HealthRecord
  ↓ 0:N
HealthWarning
  ↑
HealthWarningRule
```

健康记录必须保留：

- 测量时间
- 录入人员
- 指标
- 实际值

预警必须关联原始健康记录。

---

# 6. 照护域

```text
CareService
 ↓
CareOrder
 ↓
CareRecord
 ↓
CareEvaluation
```

老人：

```text
Elder 1:N CareOrder
```

照护人员：

```text
CareStaff 1:N CareOrder
```

---

# 7. 排班域

```text
CareStaff
 ↓
StaffSchedule
 ↓
ScheduleChangeRequest
```

排班必须防止：

```text
同一人员
+
时间重叠
```

---

# 8. 餐饮域

```text
MealMenu
 ↓
MealOrder
 ↑
Elder
```

特殊饮食：

```text
Elder
 ↓
DietRequirement
```

---

# 9. 活动域

```text
Activity
 ↓
ActivityRegistration
 ↓
ActivityCheckin
 ↑
Elder
```

推荐：

```text
activity_id + elder_id
```

唯一，防止重复报名。

---

# 10. 用药域

```text
Elder
 ↓
MedicinePlan
 ↓
MedicineRecord
```

MedicinePlan 描述长期计划。

MedicineRecord 描述每次提醒/执行事实。

---

# 11. 日志域

```text
User
 ↓
OperationLog
```

日志记录：

- 谁
- 什么时间
- 什么模块
- 什么操作
- 操作对象
- 结果

禁止记录：

- 明文密码
- JWT
- 不必要的完整健康敏感数据

---

# 12. 删除策略

历史事实数据优先：

```text
逻辑删除 / 状态停用 / 归档
```

而不是物理删除。

重点保护：

```text
HealthRecord
HealthWarning
CareRecord
CareEvaluation
ActivityCheckin
MedicineRecord
OperationLog
```

---

# 13. ER 验收

```text
[ ] 没有重复老人表
[ ] 家属关系可追溯
[ ] 健康记录可追溯
[ ] 预警可追溯
[ ] 服务可追溯
[ ] 排班可追溯
[ ] 活动报名可追溯
[ ] 用药执行可追溯
[ ] 权限关系完整
[ ] 日志可追溯
[ ] 关键查询可建立索引
[ ] 数据权限可以落地
```
