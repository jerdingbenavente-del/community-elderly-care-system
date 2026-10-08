# 智慧养老服务平台 — 前端

Elder Care Management System · Web Frontend

## 技术栈

- Vue 3
- Vite
- JavaScript
- Vue Router
- Pinia
- Axios
- Element Plus

## 环境要求

- Node.js 18+（推荐 20 / 22）
- npm 9+

## 安装

```bash
cd frontend
npm install
```

## 启动开发服务

```bash
npm run dev
```

默认地址：http://localhost:5173/

开发环境通过 Vite 代理将 `/api` 转发到后端 `http://127.0.0.1:8080`（与 Spring Boot `server.port` 一致）。

## 构建

```bash
npm run build
```

产物输出到 `dist/`。

## 预览构建结果

```bash
npm run preview
```

## 目录说明

```text
src/
  api/          Axios 封装 + auth.js
  components/   AppHeader / AppSidebar / UserAccountMenu
  layouts/      Family / Staff / Admin 三端布局
  router/       路由 + JWT/RBAC 守卫
  stores/       Pinia user（token / roles / 登录）
  styles/       全局样式与主题变量
  views/        登录与三角色首页 + 菜单占位页
```

## 当前阶段

**F1**：登录 + JWT + RBAC 三角色权限路由（真实后端 `/api/auth/login`）。
