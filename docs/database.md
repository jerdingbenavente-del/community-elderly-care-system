# docs/database.md

# 社区养老服务中心照护管理系统 —— 数据库设计基线

> 本文件是数据库设计基线。
> 实际项目中已经存在的表结构优先于示例命名；新增表必须先检查是否已有同义表。

---

# 1. 数据库原则

数据库：

```text
MySQL
```

核心原则：

1. MySQL 是最终业务数据来源。
2. Redis 不能代替核心业务持久化。
3. 业务关系优先使用明确外键/关联字段。
4. 老人主数据统一使用 `elder_id`。
5. 不重复创建老人主表。
6. 历史业务记录尽量保留。
7. 敏感数据必须限制访问。
8. SQL 必须参数化。
9. 高频查询需要考虑索引。
10. 数据库变更必须考虑旧数据兼容。

---

# 2. 命名规范

表名：

```text
snake_case
```

例如：

```text
elder
health_record
care_order
operation_log
```

字段：

```text
elder_id
created_at
updated_at
service_status
```

---

# 3. 通用字段

根据实际业务需要使用：

```text
id
created_at
updated_at
created_by
updated_by
status
deleted
remark
```

不要机械添加没有业务意义的字段。

---

# 4. 核心表关系

```text
user
 ├── user_role ── role ── role_permission ── permission
 │
 └── operation_log

elder
 ├── elder_family ── family/user
 ├── health_record ── health_warning
 ├── care_order ── care_record ── care_evaluation
 ├── activity_registration ── activity_checkin
 ├── meal_order
 └── medicine_plan ── medicine_record

care_staff
 └── staff_schedule
       └── schedule_change_request

care_service
 └── care_order
```

---

# 5. elder —— 老人主表

建议核心字段：

```text
id
name
gender
birth_date
phone
address
id_card
care_level
status
medical_history
allergy_history
special_care_requirement
remark
created_at
updated_at
```

注意：

- 身份证等敏感字段不能无条件返回。
- 老人是其他业务数据的核心关联对象。
- 不要在其他表重复存完整老人资料。

---

# 6. elder_family —— 老人与家属关系

建议：

```text
id
elder_id
family_user_id
relationship
is_primary
status
created_at
```

用途：

> 控制家属是否有权限访问目标老人。

该表是家属端数据隔离的关键。

---

# 7. emergency_contact —— 紧急联系人

建议：

```text
id
elder_id
name
relationship
phone
priority
remark
created_at
updated_at
```

---

# 8. health_record —— 健康记录

建议：

```text
id
elder_id
systolic_pressure
diastolic_pressure
blood_glucose
body_temperature
heart_rate
measured_at
recorded_by
remark
created_at
```

不要把所有健康指标强行塞成一个模糊的 `value` 字段，除非项目现有模型已经采用统一指标表设计。

---

# 9. health_warning_rule —— 健康预警规则

建议：

```text
id
indicator
rule_name
min_value
max_value
warning_level
status
description
created_at
updated_at
```

实际字段以最终业务规则为准。

阈值必须统一管理。

---

# 10. health_warning —— 健康异常预警

建议：

```text
id
elder_id
health_record_id
indicator
actual_value
rule_value
warning_level
status
generated_at
handled_by
handled_at
handling_result
remark
```

状态建议：

```text
UNHANDLED
PROCESSING
HANDLED
CLOSED
```

---

# 11. care_service —— 照护服务项目

建议：

```text
id
name
service_type
description
price
duration
status
created_at
updated_at
```

服务类型：

```text
MEAL
BATH
REHABILITATION
ENTERTAINMENT
OTHER
```

---

# 12. care_order —— 服务预约/订单

建议：

```text
id
elder_id
service_id
staff_id
scheduled_start
scheduled_end
status
created_by
created_at
updated_at
```

必须考虑：

- 时间冲突
- 人员冲突
- 老人状态
- 重复预约

---

# 13. care_record —— 服务执行记录

建议：

```text
id
care_order_id
elder_id
staff_id
checkin_at
checkout_at
service_content
execution_result
status
created_at
updated_at
```

---

# 14. care_evaluation —— 服务评价

建议：

```text
id
care_order_id
elder_id
evaluator_user_id
score
content
created_at
```

必须防止同一服务重复评价。

---

# 15. care_staff —— 照护人员

建议：

```text
id
user_id
name
phone
position
status
created_at
updated_at
```

---

# 16. staff_schedule —— 人员排班

建议：

```text
id
staff_id
work_date
start_time
end_time
shift_type
status
remark
created_at
updated_at
```

必须有合理索引支持：

```text
staff_id + work_date
```

并在 Service 层检查时间冲突。

---

# 17. schedule_change_request —— 调班申请

建议：

```text
id
schedule_id
applicant_id
target_staff_id
reason
status
reviewer_id
reviewed_at
review_comment
created_at
updated_at
```

---

# 18. meal_menu —— 餐饮食谱

建议：

```text
id
menu_date
meal_type
food_name
description
status
created_at
updated_at
```

唯一性要结合实际业务判断，例如：

```text
menu_date + meal_type
```

---

# 19. meal_order —— 老人订餐

建议：

```text
id
elder_id
menu_id
quantity
special_requirement
status
ordered_at
created_at
updated_at
```

---

# 20. diet_requirement —— 特殊饮食

建议：

```text
id
elder_id
requirement_type
description
status
created_at
updated_at
```

例如：

```text
LOW_SALT
LOW_SUGAR
SOFT
LIQUID
ALLERGY
TABOO
OTHER
```

---

# 21. activity —— 活动

建议：

```text
id
title
description
location
start_time
end_time
capacity
status
publisher_id
created_at
updated_at
```

---

# 22. activity_registration —— 活动报名

建议：

```text
id
activity_id
elder_id
registrant_user_id
status
registered_at
created_at
updated_at
```

建议唯一约束：

```text
activity_id + elder_id
```

---

# 23. activity_checkin —— 活动签到

建议：

```text
id
activity_id
elder_id
registration_id
checkin_at
created_at
```

避免重复签到。

---

# 24. medicine_plan —— 用药计划

建议：

```text
id
elder_id
medicine_name
dosage
usage
frequency
start_date
end_date
reminder_time
status
remark
created_at
updated_at
```

---

# 25. medicine_record —— 用药执行记录

建议：

```text
id
medicine_plan_id
elder_id
scheduled_time
actual_time
status
executor_id
remark
created_at
```

---

# 26. user —— 用户

实际表名：`sys_user`

```text
id
username
password_hash
real_name
phone
status
must_change_password
deleted
created_at
updated_at
```

- `password_hash`：仅保存 BCrypt，禁止明文。
- `must_change_password`（TINYINT，默认 0）：`0` 正常；`1` 必须修改密码后才能访问业务 API。
- 旧账号升级后默认 `0`；P8 业务开户与管理员重置密码后为 `1`。

密码只能保存哈希。

迁移脚本：`sql/schema/08_p8_account_security.sql`

---

# 27. role —— 角色

默认：

```text
ADMIN
CARE_STAFF
FAMILY
```

---

# 28. permission —— 权限

权限建议使用：

```text
模块:操作
```

例如：

```text
elder:view
elder:create
elder:update
elder:delete
health:view
health:create
warning:handle
service:view
service:create
schedule:update
report:view
```

---

# 29. user_role

```text
id
user_id
role_id
```

建议唯一：

```text
user_id + role_id
```

---

# 30. role_permission

```text
id
role_id
permission_id
```

建议唯一：

```text
role_id + permission_id
```

---

# 31. operation_log

建议：

```text
id
user_id
username
role
module
operation
target_type
target_id
request_method
request_uri
result
ip_address
created_at
```

禁止保存：

```text
password
JWT
```

等敏感认证信息。

---

# 32. 索引原则

重点考虑：

```text
elder.name
elder.phone
health_record.elder_id
health_record.measured_at
health_warning.elder_id
health_warning.status
care_order.elder_id
care_order.staff_id
care_order.scheduled_start
staff_schedule.staff_id + work_date
activity.start_time
activity_registration.activity_id
medicine_plan.elder_id
operation_log.user_id
operation_log.created_at
```

具体索引必须结合实际 SQL 查询，不要为了“看起来专业”给所有字段加索引。

---

# 33. 外键与业务约束

如果项目使用数据库外键，应优先保证：

```text
child record → parent exists
```

但业务状态约束不能只依赖外键。

例如：

```text
活动存在
```

不代表：

```text
活动仍然允许报名
```

这必须由 Service 判断。

---

# 34. 数据迁移原则

新增字段：

```text
优先允许旧数据兼容
```

不要直接：

```text
DROP TABLE
TRUNCATE
```

大改表结构时必须考虑：

```text
旧数据
代码兼容
索引
唯一约束
回滚风险
```

---

# 35. 测试数据

测试/演示数据可以包含：

```text
老人
照护人员
健康记录
预警
服务
排班
活动
餐饮
用药
```

但必须：

```text
真实写入 MySQL
```

并明确：

> 测试数据 / 演示数据

---

# 36. 数据库验收

数据库设计完成后必须检查：

```text
[ ] 没有重复老人主表
[ ] 家属关系清楚
[ ] 健康记录可追溯
[ ] 预警可追溯
[ ] 服务可追溯
[ ] 排班可追溯
[ ] 活动报名可追溯
[ ] 用药计划可追溯
[ ] 权限关系清楚
[ ] 日志可追溯
[ ] 关键查询有合理索引
[ ] 没有明显重复字段
[ ] 敏感数据有访问控制
```
