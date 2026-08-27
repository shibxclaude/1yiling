# 通用后台管理系统（yiling-manager）设计文档

日期：2026-08-27
状态：已批准，进入实施计划

## 背景与来源

功能需求的权威来源是 [`docs/系统管理功能提示词.md`](../../系统管理功能提示词.md)（下称"原始提示词"），
其中已经完整定义了九大模块（用户/角色/菜单/部门/字典/参数/通知公告/岗位/操作日志）的功能点、
接口清单、数据库字段和验收标准。本文档**不重复**那些内容，只补充原始提示词里留白或需要
在本仓库落地时做出的技术决策。实施计划与代码必须同时满足本文档 + 原始提示词。

调研结论：仓库内另一个项目 `1wlyj`（在线阅卷系统）虽然同样是 Vue3 + Element Plus + Pinia +
Spring Boot 技术栈，但业务模型（阅卷/评分/角色为 director/leader/teacher）与通用 RBAC 后台
管理完全不同，没有可直接复用的通用系统管理模块代码。本项目是从零搭建。

## 范围

全栈：`backend/`（Spring Boot）+ `frontend/`（Vue3）+ 数据库初始化脚本，一次性交付全部
九大模块，不分批停下确认。

## 仓库结构

```
1yiling/
├── docker-compose.yml       # 独立 MySQL 8 容器，宿主机端口 3307（3306 被 1smjd 占用）
├── sql/
│   └── init.sql             # 建表 + 初始数据（admin 账号、菜单树、角色绑定等）
├── backend/                 # Spring Boot 3.4.x / Java 21 / MyBatis-Plus
│   └── src/main/java/com/yiling/...
├── frontend/                # Vue3 + Vite + Element Plus + Pinia
│   └── src/...
└── docs/
    ├── 系统管理功能提示词.md   # 需求来源（保留不动）
    └── superpowers/
        ├── specs/2026-08-27-system-management-design.md  # 本文档
        └── plans/...          # 下一步 writing-plans 产出
```

## 技术决策（本文档新增，原始提示词未定义的部分）

1. **后端基础包名**：`com.yiling`（原始提示词占位符是 `com.demo`）。
2. **认证方案**：JWT + Spring Security 无状态方案，**不引入 Redis**。Token 由后端签发/校验，
   前端存 Pinia + localStorage。这与本用户在 1smjd 项目上"登录/JWT 先简化、上线前再决定是否加固"
   的既有偏好一致（见项目记忆 smjd-deferred-auth-decisions）。
3. **数据库**：独立 `docker-compose.yml`，MySQL 8，容器名 `1yiling-mysql`，宿主机端口映射
   `3307:3306`，避免与 `1smjd-mysql` 的 3306 冲突。`sql/init.sql` 作为初始化卷挂载
   （`docker-entrypoint-initdb.d`），首次启动自动建表建数据。
4. **后端端口**：8090（避免与其他项目常用的 8080/8081 冲突；实施时如发现被占用可调整，
   以 `backend/src/main/resources/application.yml` 中的实际值为准）。
5. **响应/异常/分页**：严格按原始提示词第四章执行：`{code,message,data}` 包装、
   `pageNo/pageSize` 入参、`records/total/size/current/pages` 分页返回、全局异常处理器、
   时间统一 `yyyy-MM-dd HH:mm:ss` 并兼容 ISO-8601 输入。
6. **操作日志实现方式**：自定义 `@Log` 注解 + AOP 环绕通知，写入 `sys_oper_log`，
   记录方法/参数/返回值/耗时/IP/操作人，失败时记录 `error_msg` 并保持业务异常正常抛出。
7. **前端状态与路由**：Pinia `store/modules/user.js` 持有 token/用户信息/roles/permissions；
   路由分静态路由（登录、404、Layout 外壳）与动态路由（`getRouters` 返回菜单树，
   前端转换为 Vue Router 路由并挂到 Layout 的 children 下）。
8. **字典组件**：`useDict(...types)` 组合式函数按需请求并缓存字典数据到内存（非持久化），
   `<dict-tag>` 组件按 `dictType` + `value` 渲染 `el-tag`。

## 数据模型摘要

沿用原始提示词第七章要求，建表清单：
`sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_post`、`sys_dict_type`、`sys_dict_data`、
`sys_config`、`sys_notice`、`sys_oper_log`，关联表 `sys_user_role`、`sys_role_menu`、
`sys_user_post`。全部 InnoDB + utf8mb4，字段/索引按各模块章节标注的字段清单实现。

初始数据：
- `admin` 超管账号（BCrypt 密码，初始密码约定为 `admin123`，仅本地开发环境使用）
- 角色 `super_admin`（`role_key=admin`，`data_scope=1` 全部数据权限）
- 部门"总公司"作为根部门
- 字典：`sys_normal_disable`、`sys_user_sex`、`sys_show_hide`、`sys_yes_no`、
  `sys_notice_type`、`sys_notice_status`、`sys_oper_type`
- `sys_config` 两条示例参数
- `sys_menu`：系统管理目录 + 九个模块菜单 + 各模块按钮级权限，`perms` 与前端
  `v-hasPermi` 一一对应；`sys_role_menu` 绑定 admin 角色到全部菜单

## 验证计划

1. `docker compose up -d` 拉起 MySQL，确认 `sql/init.sql` 自动执行成功
2. `cd backend && mvn spring-boot:run`，确认无报错，Swagger/Knife4j 页面可访问
3. `cd frontend && npm install && npm run dev`，确认无编译报错
4. 用浏览器（claude-in-chrome 或等效方式）走通登录 → 九大模块各自的
   查询→新增→编辑→删除→分页 → 按钮权限 `v-hasPermi` 生效 → 退出登录/未登录跳转 `/login`
5. 核对时间列展示为 `yyyy-MM-dd HH:mm:ss`

不满足验证计划中任意一步不算完成。

## 明确排除（YAGNI）

- 不做 Oracle 兼容（原始提示词标注"如需要则注明"，本次不需要）
- 不做用户导入 Excel（原始提示词标注为可选）
- 不做主题色/侧边栏配色切换面板（原始提示词标注为可选）
- 不引入 Redis／分布式 Session
