# P8 账号安全 API

> 本阶段仅后端；管理端开户 UI / 改密页属后续阶段。

## 1. 业务开户（自动生成账号）

`POST /api/system/users/business-accounts`

权限：`system:user:add` + ADMIN

请求：

```json
{ "name": "王小丽", "roleCode": "FAMILY" }
```

`roleCode` 仅允许 `FAMILY` / `CARE_STAFF`。

响应 data：

```json
{
  "userId": 99,
  "username": "wxl@48219",
  "realName": "王小丽",
  "roleCode": "FAMILY",
  "mustChangePassword": true
}
```

说明：

- 用户名：姓名拼音首字母 + `@` + 5 位数字（可含前导 0），冲突最多重试 10 次。
- 初始密码固定 `123456`（BCrypt 入库），**响应不返回明文密码**。
- `must_change_password = 1`。

## 2. 登录

`POST /api/auth/login`

响应新增字段：`mustChangePassword`（boolean）。旧字段保持不变。

## 3. 修改密码

`POST /api/system/users/change-password`

权限：已登录

请求：

```json
{
  "oldPassword": "123456",
  "newPassword": "abc123",
  "confirmPassword": "abc123"
}
```

规则：新密码 6–20 位，须同时含英文字母与数字，仅字母数字，禁止空格。

成功后：`must_change_password = 0`。建议前端清 Token 并重新登录。

## 4. 管理员重置密码

`POST /api/system/users/{id}/reset-password`

权限：`system:user:update` + ADMIN

- 密码重置为 `123456`（BCrypt）
- `must_change_password = 1`
- 不可重置自己（请用 change-password）

## 5. 首次改密限制

当数据库 `must_change_password = 1` 时，除下列接口外业务 API 返回 **403**，提示「首次登录请先修改密码」：

- `POST /api/auth/login`
- `POST /api/system/users/change-password`
- `GET /api/system/users/me`
- `GET /api/auth/me`

校验以每次鉴权查库结果为准，不依赖 JWT Claim。

## 6. 家属绑定

`POST /api/elder-families`

Service 层按 **roleCode=FAMILY** 校验；非家属角色绑定失败。
