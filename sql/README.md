# SQL 脚本说明

## 执行顺序

1. `schema/01_phase1_tables.sql` — 第一阶段表结构
2. `schema/02_p1_elder_alter.sql` — P1 增量（registered_at）
3. `schema/03_p2_health.sql` — P2 健康记录/阈值/预警
4. `schema/04_p3_care_service.sql` — P3 照护服务项目/服务订单
5. `schema/05_p4_care_staff_schedule.sql` — P4 护理员排班
6. `schema/06_p5_care_staff.sql` — P5 护理员工号/性别/备注增量
7. `data/01_seed_rbac.sql` — RBAC 角色/权限/开发账号
8. `data/02_p1_elder_permissions.sql` — 老人档案权限增量
9. `data/03_p1_elder_test.sql` — 可选测试家属账号 family02
10. `data/04_p2_health_seed.sql` — 健康阈值与权限增量
11. `data/05_p3_care_seed.sql` — 照护服务权限与示例服务项目
12. `data/06_p4_care_staff_schedule_seed.sql` — 排班权限、示例排班、可选 care02
13. `data/07_p5_care_staff_seed.sql` — 护理员管理权限与工号回填
14. `data/08_p6_sys_user_seed.sql` — 系统用户管理权限增量

## 注意

- `01_seed_rbac.sql` 会 TRUNCATE 系统权限相关表，仅用于开发环境初始化。
- 密码均为 BCrypt 哈希，明文仅在 `backend/README.md` 中说明开发用途。
- 若已执行过前期脚本，P2 只需补跑 `03_p2_health.sql` 与 `04_p2_health_seed.sql`。
- 若已执行过 P2，P3 只需补跑 `04_p3_care_service.sql` 与 `05_p3_care_seed.sql`。
- 若已执行过 P3，P4 只需补跑 `05_p4_care_staff_schedule.sql` 与 `06_p4_care_staff_schedule_seed.sql`。
- 若已执行过 P4，P5 只需补跑 `06_p5_care_staff.sql` 与 `07_p5_care_staff_seed.sql`。
- 若已执行过 P5，P6 只需补跑 `08_p6_sys_user_seed.sql`（无新表）。
