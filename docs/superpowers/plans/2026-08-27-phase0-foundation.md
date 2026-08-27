# Phase 0: Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the runnable skeleton — DB schema+seed data, backend core (response wrapper, exceptions, auth), and frontend core (login, layout, dynamic menu) — so that by the end of this phase you can log in as `admin` and see an empty-but-correct sidebar built from the DB-seeded menu tree.

**Architecture:** `docker-compose.yml` runs MySQL 8. `backend/` is Spring Boot 3.4.3 + MyBatis-Plus + Spring Security (stateless JWT, no Redis). `frontend/` is Vue 3 + Vite + Element Plus + Pinia + Vue Router, with axios wrapped to unwrap `{code,message,data}` and to attach the JWT.

**Tech Stack:** Java 21, Spring Boot 3.4.3, MyBatis-Plus 3.5.9, MySQL 8, jjwt 0.12.6, springdoc-openapi 2.6.0, Lombok; Vue 3.5, Vite 5, Element Plus 2.8, Pinia 3, vue-router 4, axios, sass, dayjs.

**Spec:** `docs/superpowers/specs/2026-08-27-system-management-design.md` (and the functional source of truth `docs/系统管理功能提示词.md`)

## Global Constraints

- All backend REST responses wrapped as `{code:int, message:string, data:any}`, success `code=200`. Class: `com.yiling.common.result.Result<T>`.
- Pagination: request `{pageNo=1, pageSize=10, ...filters}`; response `data = {records, total, size, current, pages}`. Class: `com.yiling.common.result.PageResult<T>`.
- All query/mutate endpoints are `POST` + JSON body, path pattern `rest/{entity}/{action}` (`listPage`, `list`, `detailById`, `save`, `update`, `delete`, `updateSimple`). Never GET-with-params for these.
- Time fields serialize as `yyyy-MM-dd HH:mm:ss`; deserialization must also accept ISO-8601 (`2024-12-24T20:24:46.000+08:00`).
- **`status` convention:** every business table has a single `status` tinyint column: `0=soft-deleted, 1=normal, 2=disabled`. `delete` endpoints do `UPDATE ... SET status=0` (never a hard DELETE) **except** `sys_menu` and `sys_dept`, which are hard-deleted (blocked in service code when the node has children) since they're trees keyed by `parent_id`. `sys_oper_log` has no soft-delete `status` column at all — batch delete there is a real `DELETE`, and its own `status` column means operation success/fail (0=success,1=fail), an unrelated reuse of the column name per-module.
- Every `listPage`/`list` query must add `status != 0` (or `status = 0`/etc. as needed) to exclude soft-deleted rows; when the module also has an enable/disable filter in its search form, that filter maps directly to `status IN (1,2)`.
- Button-level permission strings: `system:{module}:{action}` (e.g. `system:user:add`), enforced only client-side via `v-hasPermi` — see "Explicitly deferred" below.
- Base entity fields `createUser/createTime/updateUser/updateTime` are auto-filled by a MyBatis-Plus `MetaObjectHandler`; `status` defaults to `1` on insert if not set.
- **Explicitly deferred (do not build):** server-side per-permission `@PreAuthorize` checks (only "is this a valid token" is enforced server-side); automatic data-scope (`dataScope`) row filtering across modules (the field/UI is built and stored in Phase 1, but no cross-module query interceptor enforces it); Oracle compatibility; Excel import; theme/sidebar-color switch panel; Redis/distributed session.

---

## File Structure

```
1yiling/
├── docker-compose.yml
├── sql/init.sql
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/yiling/
│       │   ├── YilingApplication.java
│       │   ├── common/
│       │   │   ├── result/{Result,ResultCode,PageResult}.java
│       │   │   ├── exception/{BusinessException,GlobalExceptionHandler}.java
│       │   │   ├── entity/BaseEntity.java
│       │   │   └── config/{MybatisPlusMetaConfig,JacksonConfig,CorsConfig,SwaggerConfig}.java
│       │   ├── security/
│       │   │   ├── JwtUtil.java
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   ├── LoginUser.java
│       │   │   └── SecurityConfig.java
│       │   └── modules/auth/
│       │       ├── controller/AuthController.java
│       │       ├── dto/LoginDTO.java
│       │       ├── vo/{LoginVO,UserInfoVO,RouterVO}.java
│       │       └── service/{AuthService.java, impl/AuthServiceImpl.java}
│       └── resources/application.yml
└── frontend/
    ├── package.json / vite.config.js / index.html
    └── src/
        ├── main.js / App.vue
        ├── utils/request.js
        ├── stores/user.js
        ├── directive/hasPermi.js
        ├── router/index.js
        ├── views/login/index.vue
        ├── views/dashboard/index.vue
        └── layout/{index.vue, components/{Sidebar.vue, Navbar.vue, TagsView.vue}}
```

---

### Task 1: Docker Compose + Database DDL + Seed Data

**Files:**
- Create: `docker-compose.yml`
- Create: `sql/init.sql`

**Interfaces:**
- Produces: a MySQL 8 instance reachable at `127.0.0.1:3307`, database `yiling`, user `yiling`/password `yiling123`, root password `root123`; all 13 tables below exist with seed rows. Every later backend task assumes this exact schema.

- [ ] **Step 1: Write `docker-compose.yml`**

```yaml
services:
  mysql:
    image: mysql:8
    container_name: 1yiling-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: yiling
      MYSQL_USER: yiling
      MYSQL_PASSWORD: yiling123
    ports:
      - "3307:3306"
    volumes:
      - yiling_mysql_data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-proot123"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  yiling_mysql_data:
```

- [ ] **Step 2: Write `sql/init.sql`**

```sql
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- sys_dept ----------
CREATE TABLE sys_dept (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id     BIGINT       NOT NULL DEFAULT 0 COMMENT '上级部门id，0为顶级',
  ancestors     VARCHAR(255) NOT NULL DEFAULT '' COMMENT '祖级列表，逗号分隔',
  dept_code     VARCHAR(64)  NOT NULL COMMENT '部门编码',
  dept_name     VARCHAR(64)  NOT NULL COMMENT '部门名称',
  order_num     INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
  leader        VARCHAR(64)           DEFAULT NULL COMMENT '负责人',
  phone         VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
  email         VARCHAR(64)           DEFAULT NULL COMMENT '邮箱',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '0删除 1正常 2停用',
  create_user   VARCHAR(64)           DEFAULT NULL,
  create_time   DATETIME              DEFAULT NULL,
  update_user   VARCHAR(64)           DEFAULT NULL,
  update_time   DATETIME              DEFAULT NULL,
  UNIQUE KEY uk_dept_code (dept_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';

-- ---------- sys_post ----------
CREATE TABLE sys_post (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_code     VARCHAR(64) NOT NULL COMMENT '岗位编码',
  post_name     VARCHAR(64) NOT NULL COMMENT '岗位名称',
  post_sort     INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  status        TINYINT NOT NULL DEFAULT 1 COMMENT '0删除 1正常 2停用',
  remark        VARCHAR(500) DEFAULT NULL,
  create_user   VARCHAR(64) DEFAULT NULL,
  create_time   DATETIME DEFAULT NULL,
  update_user   VARCHAR(64) DEFAULT NULL,
  update_time   DATETIME DEFAULT NULL,
  UNIQUE KEY uk_post_code (post_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位表';

-- ---------- sys_role ----------
CREATE TABLE sys_role (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name            VARCHAR(64) NOT NULL COMMENT '角色名称',
  role_key             VARCHAR(64) NOT NULL COMMENT '权限字符',
  role_sort            INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  data_scope           CHAR(1) NOT NULL DEFAULT '1' COMMENT '1全部 2自定义 3本部门 4本部门及以下 5仅本人',
  dept_ids             VARCHAR(500) DEFAULT NULL COMMENT '自定义数据权限部门id列表，逗号分隔',
  menu_check_strictly  TINYINT(1) NOT NULL DEFAULT 1 COMMENT '菜单树选择项是否关联显示',
  dept_check_strictly  TINYINT(1) NOT NULL DEFAULT 1 COMMENT '部门树选择项是否关联显示',
  status               TINYINT NOT NULL DEFAULT 1 COMMENT '0删除 1正常 2停用',
  remark               VARCHAR(500) DEFAULT NULL,
  create_user          VARCHAR(64) DEFAULT NULL,
  create_time          DATETIME DEFAULT NULL,
  update_user          VARCHAR(64) DEFAULT NULL,
  update_time          DATETIME DEFAULT NULL,
  UNIQUE KEY uk_role_key (role_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- ---------- sys_menu ----------
CREATE TABLE sys_menu (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id      BIGINT NOT NULL DEFAULT 0 COMMENT '上级菜单id，0为顶级',
  menu_name      VARCHAR(64) NOT NULL COMMENT '菜单名称',
  menu_type      CHAR(1) NOT NULL COMMENT '0目录 1菜单 2按钮',
  menu_sort      INT NOT NULL DEFAULT 0 COMMENT '显示顺序',
  icon           VARCHAR(100) DEFAULT NULL COMMENT '图标',
  menu_path      VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
  menu_component VARCHAR(255) DEFAULT NULL COMMENT '组件路径',
  perms          VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  query_param    VARCHAR(255) DEFAULT NULL COMMENT '路由参数',
  if_frame       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否外链',
  if_cache       TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否缓存(keepAlive)',
  status         TINYINT NOT NULL DEFAULT 1 COMMENT '0删除 1正常显示 2隐藏',
  create_user    VARCHAR(64) DEFAULT NULL,
  create_time    DATETIME DEFAULT NULL,
  update_user    VARCHAR(64) DEFAULT NULL,
  update_time    DATETIME DEFAULT NULL,
  UNIQUE KEY uk_menu_path (menu_path),
  UNIQUE KEY uk_menu_perms (perms),
  UNIQUE KEY uk_menu_component (menu_component)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单表';

-- ---------- sys_user ----------
CREATE TABLE sys_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  dept_id     BIGINT DEFAULT NULL COMMENT '归属部门',
  username    VARCHAR(64) NOT NULL COMMENT '用户名称，仅新增可改',
  nick_name   VARCHAR(64) NOT NULL COMMENT '用户昵称',
  passwd      VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  sex         CHAR(1) NOT NULL DEFAULT '2' COMMENT '0男 1女 2未知，字典sys_user_sex',
  email       VARCHAR(64) DEFAULT NULL,
  phone       VARCHAR(20) DEFAULT NULL,
  avatar      VARCHAR(255) DEFAULT NULL,
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '0删除 1正常 2停用',
  remark      VARCHAR(500) DEFAULT NULL,
  create_user VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_user VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

CREATE TABLE sys_user_post (
  user_id BIGINT NOT NULL,
  post_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户岗位关联表';

CREATE TABLE sys_role_menu (
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- ---------- sys_dict_type / sys_dict_data ----------
CREATE TABLE sys_dict_type (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  dict_name   VARCHAR(64) NOT NULL COMMENT '字典名称',
  dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型编码',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '0删除 1正常 2停用',
  remark      VARCHAR(500) DEFAULT NULL,
  create_user VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_user VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';

CREATE TABLE sys_dict_data (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  dict_type   VARCHAR(100) NOT NULL COMMENT '关联sys_dict_type.dict_type',
  dict_label  VARCHAR(100) NOT NULL COMMENT '字典标签',
  dict_value  VARCHAR(100) NOT NULL COMMENT '字典键值',
  dict_sort   INT NOT NULL DEFAULT 0,
  css_class   VARCHAR(100) DEFAULT NULL COMMENT '样式属性',
  list_class  VARCHAR(100) DEFAULT NULL COMMENT '回显样式(el-tag type)',
  is_default  CHAR(1) NOT NULL DEFAULT 'N' COMMENT 'Y是 N否',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '0删除 1正常 2停用',
  remark      VARCHAR(500) DEFAULT NULL,
  create_user VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT NULL,
  update_user VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT NULL,
  KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';

-- ---------- sys_config ----------
CREATE TABLE sys_config (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  config_name  VARCHAR(100) NOT NULL,
  config_key   VARCHAR(100) NOT NULL,
  config_value VARCHAR(500) NOT NULL,
  config_type  CHAR(1) NOT NULL DEFAULT 'N' COMMENT 'Y内置 N非内置',
  status       TINYINT NOT NULL DEFAULT 1 COMMENT '0删除 1正常 2停用',
  remark       VARCHAR(500) DEFAULT NULL,
  create_user  VARCHAR(64) DEFAULT NULL,
  create_time  DATETIME DEFAULT NULL,
  update_user  VARCHAR(64) DEFAULT NULL,
  update_time  DATETIME DEFAULT NULL,
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='参数配置表';

-- ---------- sys_notice ----------
CREATE TABLE sys_notice (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  notice_title   VARCHAR(200) NOT NULL,
  notice_type    CHAR(1) NOT NULL COMMENT '1通知 2公告',
  notice_content LONGTEXT,
  notice_param   VARCHAR(500) DEFAULT NULL,
  status         TINYINT NOT NULL DEFAULT 1 COMMENT '0删除 1正常 2关闭',
  create_user    VARCHAR(64) DEFAULT NULL,
  create_time    DATETIME DEFAULT NULL,
  update_user    VARCHAR(64) DEFAULT NULL,
  update_time    DATETIME DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知公告表';

-- ---------- sys_oper_log ----------
CREATE TABLE sys_oper_log (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  title          VARCHAR(100) DEFAULT NULL COMMENT '系统模块',
  business_type  TINYINT NOT NULL DEFAULT 0 COMMENT '0其他 1新增 2修改 3删除',
  method         VARCHAR(255) DEFAULT NULL,
  request_method VARCHAR(20) DEFAULT NULL,
  operator_type  TINYINT NOT NULL DEFAULT 1 COMMENT '0其他 1后台用户 2手机端用户',
  oper_id        BIGINT DEFAULT NULL,
  oper_name      VARCHAR(64) DEFAULT NULL,
  dept_id        BIGINT DEFAULT NULL,
  dept_name      VARCHAR(64) DEFAULT NULL,
  oper_url       VARCHAR(255) DEFAULT NULL,
  oper_ip        VARCHAR(64) DEFAULT NULL,
  oper_location  VARCHAR(255) DEFAULT NULL,
  oper_param     TEXT,
  json_result    TEXT,
  status         TINYINT NOT NULL DEFAULT 0 COMMENT '0成功 1异常（本表专属含义，非通用status约定）',
  error_msg      VARCHAR(2000) DEFAULT NULL,
  oper_time      DATETIME DEFAULT NULL,
  cost_time      BIGINT DEFAULT NULL COMMENT '耗时ms'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- ================= seed data =================

INSERT INTO sys_dept (id,parent_id,ancestors,dept_code,dept_name,order_num,status,create_user,create_time)
VALUES (100,0,'0','HQ','总公司',1,1,'system',NOW());

INSERT INTO sys_post (id,post_code,post_name,post_sort,status,create_user,create_time)
VALUES (1,'admin','管理员',1,1,'system',NOW());

INSERT INTO sys_role (id,role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,remark,create_user,create_time)
VALUES (1,'超级管理员','admin',1,'1',1,1,1,'超级管理员拥有全部权限','system',NOW());

-- BCrypt hash of "admin123"
INSERT INTO sys_user (id,dept_id,username,nick_name,passwd,sex,status,create_user,create_time)
VALUES (1,100,'admin','管理员','$2a$10$7JB720yubVSofvVWbGReyO9y5Y8w6Ts1i8k8b1nQ8YQ8Z8v8y8y8y',2,1,'system',NOW());

INSERT INTO sys_user_role (user_id,role_id) VALUES (1,1);
INSERT INTO sys_user_post (user_id,post_id) VALUES (1,1);

-- menu tree: 1=目录 系统管理, 10xx=菜单, 1xxxx=按钮
INSERT INTO sys_menu (id,parent_id,menu_name,menu_type,menu_sort,icon,menu_path,menu_component,status,create_user,create_time) VALUES
(1,0,'系统管理','0',1,'system','/system','Layout',1,'system',NOW());

INSERT INTO sys_menu (id,parent_id,menu_name,menu_type,menu_sort,icon,menu_path,menu_component,perms,status,create_user,create_time) VALUES
(1001,1,'用户管理','1',1,'user','user','system/user/index','system:user:list',1,'system',NOW()),
(1002,1,'角色管理','1',2,'role','role','system/role/index','system:role:list',1,'system',NOW()),
(1003,1,'菜单管理','1',3,'tree-table','menu','system/menu/index','system:menu:list',1,'system',NOW()),
(1004,1,'部门管理','1',4,'tree','dept','system/dept/index','system:dept:list',1,'system',NOW()),
(1005,1,'岗位管理','1',5,'post','post','system/post/index','system:post:list',1,'system',NOW()),
(1006,1,'字典管理','1',6,'dict','dict','system/dict/index','system:dict:list',1,'system',NOW()),
(1007,1,'参数设置','1',7,'edit','config','system/config/index','system:config:list',1,'system',NOW()),
(1008,1,'通知公告','1',8,'message','notice','system/notice/index','system:notice:list',1,'system',NOW()),
(1009,1,'操作日志','1',9,'log','sysLog','system/sysLog/index','system:log:list',1,'system',NOW());

-- standard add/edit/remove/query/export buttons for each of the 9 modules
INSERT INTO sys_menu (id,parent_id,menu_name,menu_type,menu_sort,perms,status,create_user,create_time)
SELECT b.btn_id, b.menu_id, b.btn_name, '2', b.sort, b.perms, 1, 'system', NOW() FROM (
  SELECT 100001 btn_id,1001 menu_id,'用户查询' btn_name,1 sort,'system:user:query' perms UNION ALL
  SELECT 100002,1001,'用户新增',2,'system:user:add' UNION ALL
  SELECT 100003,1001,'用户修改',3,'system:user:edit' UNION ALL
  SELECT 100004,1001,'用户删除',4,'system:user:remove' UNION ALL
  SELECT 100005,1001,'用户导出',5,'system:user:export' UNION ALL
  SELECT 100011,1002,'角色查询',1,'system:role:query' UNION ALL
  SELECT 100012,1002,'角色新增',2,'system:role:add' UNION ALL
  SELECT 100013,1002,'角色修改',3,'system:role:edit' UNION ALL
  SELECT 100014,1002,'角色删除',4,'system:role:remove' UNION ALL
  SELECT 100015,1002,'角色导出',5,'system:role:export' UNION ALL
  SELECT 100021,1003,'菜单查询',1,'system:menu:query' UNION ALL
  SELECT 100022,1003,'菜单新增',2,'system:menu:add' UNION ALL
  SELECT 100023,1003,'菜单修改',3,'system:menu:edit' UNION ALL
  SELECT 100024,1003,'菜单删除',4,'system:menu:remove' UNION ALL
  SELECT 100031,1004,'部门查询',1,'system:dept:query' UNION ALL
  SELECT 100032,1004,'部门新增',2,'system:dept:add' UNION ALL
  SELECT 100033,1004,'部门修改',3,'system:dept:edit' UNION ALL
  SELECT 100034,1004,'部门删除',4,'system:dept:remove' UNION ALL
  SELECT 100041,1005,'岗位查询',1,'system:post:query' UNION ALL
  SELECT 100042,1005,'岗位新增',2,'system:post:add' UNION ALL
  SELECT 100043,1005,'岗位修改',3,'system:post:edit' UNION ALL
  SELECT 100044,1005,'岗位删除',4,'system:post:remove' UNION ALL
  SELECT 100045,1005,'岗位导出',5,'system:post:export' UNION ALL
  SELECT 100051,1006,'字典查询',1,'system:dict:query' UNION ALL
  SELECT 100052,1006,'字典新增',2,'system:dict:add' UNION ALL
  SELECT 100053,1006,'字典修改',3,'system:dict:edit' UNION ALL
  SELECT 100054,1006,'字典删除',4,'system:dict:remove' UNION ALL
  SELECT 100055,1006,'字典导出',5,'system:dict:export' UNION ALL
  SELECT 100061,1007,'参数查询',1,'system:config:query' UNION ALL
  SELECT 100062,1007,'参数新增',2,'system:config:add' UNION ALL
  SELECT 100063,1007,'参数修改',3,'system:config:edit' UNION ALL
  SELECT 100064,1007,'参数删除',4,'system:config:remove' UNION ALL
  SELECT 100065,1007,'参数导出',5,'system:config:export' UNION ALL
  SELECT 100071,1008,'公告查询',1,'system:notice:query' UNION ALL
  SELECT 100072,1008,'公告新增',2,'system:notice:add' UNION ALL
  SELECT 100073,1008,'公告修改',3,'system:notice:edit' UNION ALL
  SELECT 100074,1008,'公告删除',4,'system:notice:remove' UNION ALL
  SELECT 100081,1009,'日志查询',1,'system:log:query' UNION ALL
  SELECT 100082,1009,'日志删除',4,'system:log:remove' UNION ALL
  SELECT 100083,1009,'日志导出',5,'system:log:export'
) b;

-- bind every menu/button to the admin role
INSERT INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu;

-- dict types + data
INSERT INTO sys_dict_type (dict_name,dict_type,status,create_user,create_time) VALUES
('系统开关','sys_normal_disable',1,'system',NOW()),
('用户性别','sys_user_sex',1,'system',NOW()),
('菜单显示状态','sys_show_hide',1,'system',NOW()),
('系统是否','sys_yes_no',1,'system',NOW()),
('通知类型','sys_notice_type',1,'system',NOW()),
('通知状态','sys_notice_status',1,'system',NOW()),
('操作类型','sys_oper_type',1,'system',NOW());

INSERT INTO sys_dict_data (dict_type,dict_label,dict_value,dict_sort,list_class,is_default,status,create_user,create_time) VALUES
('sys_normal_disable','正常','1',1,'success','Y',1,'system',NOW()),
('sys_normal_disable','停用','2',2,'danger','N',1,'system',NOW()),
('sys_user_sex','男','0',1,'primary','Y',1,'system',NOW()),
('sys_user_sex','女','1',2,'warning','N',1,'system',NOW()),
('sys_user_sex','未知','2',3,'info','N',1,'system',NOW()),
('sys_show_hide','显示','1',1,'primary','Y',1,'system',NOW()),
('sys_show_hide','隐藏','2',2,'info','N',1,'system',NOW()),
('sys_yes_no','是','Y',1,'primary','Y',1,'system',NOW()),
('sys_yes_no','否','N',2,'info','N',1,'system',NOW()),
('sys_notice_type','通知','1',1,'warning','Y',1,'system',NOW()),
('sys_notice_type','公告','2',2,'success','N',1,'system',NOW()),
('sys_notice_status','正常','1',1,'success','Y',1,'system',NOW()),
('sys_notice_status','关闭','2',2,'info','N',1,'system',NOW()),
('sys_oper_type','其他','0',1,'info','N',1,'system',NOW()),
('sys_oper_type','新增','1',2,'primary','N',1,'system',NOW()),
('sys_oper_type','修改','2',3,'warning','N',1,'system',NOW()),
('sys_oper_type','删除','3',4,'danger','N',1,'system',NOW());

INSERT INTO sys_config (config_name,config_key,config_value,config_type,status,create_user,create_time) VALUES
('主框架页-默认皮肤样式名称','sys.index.skinName','skin-blue','Y',1,'system',NOW()),
('账号自初始密码','sys.account.initPassword','admin123','Y',1,'system',NOW());

SET FOREIGN_KEY_CHECKS = 1;
```

- [ ] **Step 3: Bring up MySQL and verify seed data**

Run:
```bash
cd /home/ubuntu/proj/1yiling
docker compose up -d
docker compose exec mysql mysql -uroot -proot123 -e "USE yiling; SELECT COUNT(*) FROM sys_menu; SELECT COUNT(*) FROM sys_user; SELECT username FROM sys_user;"
```
Expected: `sys_menu` count = 55 (1 目录 + 9 菜单 + 45 按钮), `sys_user` count = 1, username `admin`. No SQL errors during container startup (`docker compose logs mysql` shows no `ERROR` lines from the init script).

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml sql/init.sql
git commit -m "feat: add MySQL docker-compose and full DDL + seed data"
```

**Note on the BCrypt hash above:** it's a placeholder shape — when you reach Task 3 (Spring Security is on the classpath), regenerate the real hash for `admin123` with `new BCryptPasswordEncoder().encode("admin123")` in a scratch test/main method, and `UPDATE sys_user SET passwd='<real-hash>' WHERE username='admin';` against the running container (or edit `sql/init.sql` and recreate the volume with `docker compose down -v && docker compose up -d`) before Task 3's login test.

---

### Task 2: Backend Maven Skeleton + Common Layer

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/yiling/YilingApplication.java`
- Create: `backend/src/main/java/com/yiling/common/result/ResultCode.java`
- Create: `backend/src/main/java/com/yiling/common/result/Result.java`
- Create: `backend/src/main/java/com/yiling/common/result/PageResult.java`
- Create: `backend/src/main/java/com/yiling/common/exception/BusinessException.java`
- Create: `backend/src/main/java/com/yiling/common/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/yiling/common/entity/BaseEntity.java`
- Create: `backend/src/main/java/com/yiling/common/config/MybatisPlusMetaConfig.java`
- Create: `backend/src/main/java/com/yiling/common/config/JacksonConfig.java`
- Create: `backend/src/main/java/com/yiling/common/config/CorsConfig.java`
- Create: `backend/src/main/java/com/yiling/common/config/SwaggerConfig.java`
- Create: `backend/src/main/java/com/yiling/common/controller/HealthController.java`
- Test: `backend/src/test/java/com/yiling/HealthControllerTest.java`

**Interfaces:**
- Produces: `Result.success(data)` / `Result.success()` / `Result.error(code,message)`; `PageResult<T>{records,total,size,current,pages}`; `BusinessException(String message)`; `BaseEntity{createUser,createTime,updateUser,updateTime,status}` (all entities extend this, plus their own `id`). Every later task's entity/controller relies on these exact class names and package `com.yiling.common.*`.

- [ ] **Step 1: `backend/pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.3</version>
    <relativePath/>
  </parent>

  <groupId>com.yiling</groupId>
  <artifactId>yiling-backend</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>jar</packaging>

  <properties>
    <java.version>21</java.version>
    <mybatis-plus.version>3.5.9</mybatis-plus.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
      <version>${mybatis-plus.version}</version>
    </dependency>
    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>0.12.6</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>2.6.0</version>
    </dependency>
    <dependency>
      <groupId>com.alibaba</groupId>
      <artifactId>easyexcel</artifactId>
      <version>3.3.4</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: `backend/src/main/resources/application.yml`**

```yaml
server:
  port: 8090

spring:
  application:
    name: yiling-backend
  datasource:
    url: jdbc:mysql://127.0.0.1:3307/yiling?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: yiling
    password: yiling123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    time-zone: Asia/Shanghai
    date-format: yyyy-MM-dd HH:mm:ss

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: none

yiling:
  jwt:
    secret: "yiling-manager-jwt-secret-key-please-change-in-production-2026"
    expire-seconds: 43200
```

- [ ] **Step 3: `common/result/ResultCode.java`**

```java
package com.yiling.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "系统繁忙，请稍后重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

- [ ] **Step 4: `common/result/Result.java`**

```java
package com.yiling.common.result;

import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(resultCode.getMessage());
        return r;
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
```

- [ ] **Step 5: `common/result/PageResult.java`**

```java
package com.yiling.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long size;
    private long current;
    private long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(page.getRecords());
        r.setTotal(page.getTotal());
        r.setSize(page.getSize());
        r.setCurrent(page.getCurrent());
        r.setPages(page.getPages());
        return r;
    }
}
```

- [ ] **Step 6: `common/exception/BusinessException.java`**

```java
package com.yiling.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 7: `common/exception/GlobalExceptionHandler.java`**

```java
package com.yiling.common.exception;

import com.yiling.common.result.Result;
import com.yiling.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("business exception: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getField() + fe.getDefaultMessage() : "参数校验失败";
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("unhandled exception", e);
        return Result.error(ResultCode.ERROR);
    }
}
```

- [ ] **Step 8: `common/entity/BaseEntity.java`**

```java
package com.yiling.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BaseEntity implements Serializable {

    @TableField(fill = FieldFill.INSERT)
    private String createUser;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer status;
}
```

- [ ] **Step 9: `common/config/MybatisPlusMetaConfig.java`**

```java
package com.yiling.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MybatisPlusMetaConfig implements MetaObjectHandler {

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "system";
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createUser", String.class, currentUsername());
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateUser", String.class, currentUsername());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "status", Integer.class, 1);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateUser", String.class, currentUsername());
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

- [ ] **Step 10: `common/config/JacksonConfig.java`** (accept ISO-8601 input, always emit `yyyy-MM-dd HH:mm:ss`)

```java
package com.yiling.common.config;

import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    static class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
        FlexibleLocalDateTimeDeserializer() { super(LocalDateTime.class); }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getText().trim();
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {
                return OffsetDateTime.parse(value).toLocalDateTime();
            }
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.modules(new SimpleModule().addDeserializer(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer()));
    }
}
```

- [ ] **Step 11: `common/config/CorsConfig.java`**

```java
package com.yiling.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

- [ ] **Step 12: `common/config/SwaggerConfig.java`**

```java
package com.yiling.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("yiling-manager API").version("1.0"));
    }
}
```

- [ ] **Step 13: `common/controller/HealthController.java`**

```java
package com.yiling.common.controller;

import com.yiling.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("ok");
    }
}
```

- [ ] **Step 14: `YilingApplication.java`**

```java
package com.yiling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class YilingApplication {
    public static void main(String[] args) {
        SpringApplication.run(YilingApplication.class, args);
    }
}
```

- [ ] **Step 15: write the failing test `HealthControllerTest.java`**

```java
package com.yiling;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void health_returnsWrappedSuccess() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("ok"));
    }
}
```

Note: this test needs Spring Security permissive enough to run — Task 3 adds `SecurityConfig`. Until Task 3 exists, this test will fail to even load the context (no `SecurityConfig` bean yet is fine, Spring Security auto-configures a default permit-all-with-login-form which `@WithMockUser` satisfies). Run it only after Task 3 is also in place; for now just get the project compiling.

- [ ] **Step 16: Compile and verify the project builds**

Run: `cd backend && mvn -q compile`
Expected: `BUILD SUCCESS`, no output beyond Maven's own noise.

- [ ] **Step 17: Commit**

```bash
git add backend/pom.xml backend/src
git commit -m "feat(backend): Spring Boot skeleton with common response/exception/base-entity layer"
```

---

### Task 3: Backend Security (JWT) + Auth Endpoints

**Files:**
- Create: `backend/src/main/java/com/yiling/security/LoginUser.java`
- Create: `backend/src/main/java/com/yiling/security/JwtUtil.java`
- Create: `backend/src/main/java/com/yiling/security/JwtAuthenticationFilter.java`
- Create: `backend/src/main/java/com/yiling/security/SecurityConfig.java`
- Create: `backend/src/main/java/com/yiling/modules/auth/dto/LoginDTO.java`
- Create: `backend/src/main/java/com/yiling/modules/auth/vo/LoginVO.java`
- Create: `backend/src/main/java/com/yiling/modules/auth/vo/UserInfoVO.java`
- Create: `backend/src/main/java/com/yiling/modules/auth/vo/RouterVO.java`
- Create: `backend/src/main/java/com/yiling/modules/auth/service/AuthService.java`
- Create: `backend/src/main/java/com/yiling/modules/auth/service/impl/AuthServiceImpl.java`
- Create: `backend/src/main/java/com/yiling/modules/auth/controller/AuthController.java`
- Test: `backend/src/test/java/com/yiling/modules/auth/AuthControllerIT.java`

**Interfaces:**
- Consumes: `sys_user`/`sys_role`/`sys_user_role`/`sys_menu`/`sys_role_menu` tables from Task 1; `Result`, `BusinessException` from Task 2.
- Produces: `POST /login {username,passwd} -> {token}`; `GET /getInfo -> {user:{id,username,nickName,deptId,avatar}, roles:[roleKey], permissions:[perms]}`; `GET /getRouters -> RouterVO[]` where `RouterVO{name,path,component,meta:{title,icon,noCache},children:[...]}` built from menus of type 0/1 that the current user's roles grant, excluding type-2 buttons. Every later module's `SecurityConfig` permission rule (`.requestMatchers(...).authenticated()`) depends on this filter chain being wired first. `JwtUtil.generateToken(userId, username)` / `parseToken(token) -> userId` are the two methods later tasks may reuse if needed, but no other module should need to touch security directly — they just receive `Authentication.getName()` as the acting username.

- [ ] **Step 1: `security/LoginUser.java`** — the principal object stored in the `Authentication`

```java
package com.yiling.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginUser {
    private Long userId;
    private String username;
    private List<String> roleKeys;
    private List<String> permissions;
}
```

- [ ] **Step 2: `security/JwtUtil.java`**

```java
package com.yiling.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${yiling.jwt.secret}")
    private String secret;

    @Value("${yiling.jwt.expire-seconds}")
    private long expireSeconds;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireSeconds * 1000);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    public Long parseUserId(String token) {
        var claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
        return claims.get("userId", Integer.class) != null
                ? Long.valueOf(claims.get("userId", Integer.class))
                : claims.get("userId", Long.class);
    }
}
```

- [ ] **Step 3: `security/JwtAuthenticationFilter.java`** — loads roles/permissions fresh per request (no Redis, no cache; acceptable at this scale per spec)

```java
package com.yiling.security;

import com.yiling.modules.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Long userId = jwtUtil.parseUserId(token);
                LoginUser loginUser = authService.loadLoginUser(userId);
                var auth = new UsernamePasswordAuthenticationToken(loginUser, null, List.of());
                auth.setDetails(loginUser.getUsername());
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(loginUser.getUsername(), null, List.of()));
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 4: `security/SecurityConfig.java`**

```java
package com.yiling.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/login", "/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

- [ ] **Step 5: `modules/auth/dto/LoginDTO.java`**

```java
package com.yiling.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String passwd;
}
```

- [ ] **Step 6: `modules/auth/vo/{LoginVO,UserInfoVO,RouterVO}.java`**

```java
package com.yiling.modules.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
}
```

```java
package com.yiling.modules.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {
    private UserBrief user;
    private List<String> roles;
    private List<String> permissions;

    @Data
    public static class UserBrief {
        private Long id;
        private String username;
        private String nickName;
        private Long deptId;
        private String avatar;
    }
}
```

```java
package com.yiling.modules.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class RouterVO {
    private String name;
    private String path;
    private String component;
    private Boolean hidden;
    private Meta meta;
    private List<RouterVO> children;

    @Data
    public static class Meta {
        private String title;
        private String icon;
        private Boolean noCache;

        public Meta(String title, String icon, Boolean noCache) {
            this.title = title;
            this.icon = icon;
            this.noCache = noCache;
        }
    }
}
```

- [ ] **Step 7: `modules/auth/service/AuthService.java`**

```java
package com.yiling.modules.auth.service;

import com.yiling.modules.auth.vo.RouterVO;
import com.yiling.modules.auth.vo.UserInfoVO;
import com.yiling.security.LoginUser;

import java.util.List;

public interface AuthService {
    String login(String username, String rawPassword);
    LoginUser loadLoginUser(Long userId);
    UserInfoVO getInfo(String username);
    List<RouterVO> getRouters(String username);
}
```

- [ ] **Step 8: `modules/auth/service/impl/AuthServiceImpl.java`** — uses plain JDBC-ish MyBatis-Plus lookups against the tables Task 1 created (no dedicated User/Role/Menu module code exists yet at this point in the build; Phase 1 will introduce the full `SysUser`/`SysRole`/`SysMenu` entities/mappers that this class should then switch to reusing — for Phase 0, query directly via `@Autowired SqlSessionTemplate` style thin repository methods so login works standalone)

```java
package com.yiling.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yiling.common.exception.BusinessException;
import com.yiling.modules.auth.service.AuthService;
import com.yiling.modules.auth.vo.RouterVO;
import com.yiling.modules.auth.vo.UserInfoVO;
import com.yiling.security.JwtUtil;
import com.yiling.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public String login(String username, String rawPassword) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, passwd, status FROM sys_user WHERE username = ?", username);
        if (rows.isEmpty()) {
            throw new BusinessException("用户名或密码错误");
        }
        Map<String, Object> row = rows.get(0);
        if (((Number) row.get("status")).intValue() != 1) {
            throw new BusinessException("账号已停用或不存在");
        }
        if (!passwordEncoder.matches(rawPassword, (String) row.get("passwd"))) {
            throw new BusinessException("用户名或密码错误");
        }
        Long userId = ((Number) row.get("id")).longValue();
        return jwtUtil.generateToken(userId, username);
    }

    @Override
    public LoginUser loadLoginUser(Long userId) {
        List<Map<String, Object>> userRows = jdbcTemplate.queryForList(
                "SELECT username FROM sys_user WHERE id = ? AND status = 1", userId);
        if (userRows.isEmpty()) {
            throw new BusinessException("用户不存在或已停用");
        }
        String username = (String) userRows.get(0).get("username");
        List<String> roleKeys = jdbcTemplate.queryForList(
                "SELECT r.role_key FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id " +
                        "WHERE ur.user_id = ? AND r.status = 1", String.class, userId);
        List<String> perms = jdbcTemplate.queryForList(
                "SELECT DISTINCT m.perms FROM sys_menu m JOIN sys_role_menu rm ON rm.menu_id = m.id " +
                        "JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
                        "WHERE ur.user_id = ? AND m.status = 1 AND m.perms IS NOT NULL", String.class, userId);
        return new LoginUser(userId, username, roleKeys, perms);
    }

    @Override
    public UserInfoVO getInfo(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username, nick_name, dept_id, avatar FROM sys_user WHERE username = ?", username);
        if (rows.isEmpty()) {
            throw new BusinessException("用户不存在");
        }
        Map<String, Object> row = rows.get(0);
        Long userId = ((Number) row.get("id")).longValue();

        UserInfoVO vo = new UserInfoVO();
        UserInfoVO.UserBrief brief = new UserInfoVO.UserBrief();
        brief.setId(userId);
        brief.setUsername((String) row.get("username"));
        brief.setNickName((String) row.get("nick_name"));
        brief.setDeptId(row.get("dept_id") != null ? ((Number) row.get("dept_id")).longValue() : null);
        brief.setAvatar((String) row.get("avatar"));
        vo.setUser(brief);

        LoginUser lu = loadLoginUser(userId);
        vo.setRoles(lu.getRoleKeys());
        vo.setPermissions(lu.getPermissions());
        return vo;
    }

    @Override
    public List<RouterVO> getRouters(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username FROM sys_user WHERE username = ?", username);
        Long userId = ((Number) rows.get(0).get("id")).longValue();

        List<Map<String, Object>> menus = jdbcTemplate.queryForList(
                "SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_type, m.menu_sort, m.icon, " +
                        "m.menu_path, m.menu_component, m.status " +
                        "FROM sys_menu m JOIN sys_role_menu rm ON rm.menu_id = m.id " +
                        "JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
                        "WHERE ur.user_id = ? AND m.menu_type IN ('0','1') AND m.status != 0 " +
                        "ORDER BY m.menu_sort", userId);

        Map<Long, List<Map<String, Object>>> byParent = new HashMap<>();
        for (Map<String, Object> m : menus) {
            long parentId = ((Number) m.get("parent_id")).longValue();
            byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(m);
        }
        return buildTree(0L, byParent);
    }

    private List<RouterVO> buildTree(Long parentId, Map<Long, List<Map<String, Object>>> byParent) {
        List<Map<String, Object>> children = byParent.getOrDefault(parentId, List.of());
        return children.stream().map(m -> {
            RouterVO vo = new RouterVO();
            vo.setName((String) m.get("menu_name"));
            vo.setPath((String) m.get("menu_path"));
            vo.setComponent((String) m.get("menu_component"));
            vo.setHidden("2".equals(String.valueOf(m.get("status"))));
            vo.setMeta(new RouterVO.Meta((String) m.get("menu_name"), (String) m.get("icon"), false));
            long id = ((Number) m.get("id")).longValue();
            List<RouterVO> children2 = buildTree(id, byParent);
            vo.setChildren(children2.isEmpty() ? null : children2);
            return vo;
        }).collect(Collectors.toList());
    }
}
```

- [ ] **Step 9: `modules/auth/controller/AuthController.java`**

```java
package com.yiling.modules.auth.controller;

import com.yiling.common.result.Result;
import com.yiling.modules.auth.dto.LoginDTO;
import com.yiling.modules.auth.service.AuthService;
import com.yiling.modules.auth.vo.LoginVO;
import com.yiling.modules.auth.vo.RouterVO;
import com.yiling.modules.auth.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        String token = authService.login(dto.getUsername(), dto.getPasswd());
        return Result.success(new LoginVO(token));
    }

    @GetMapping("/getInfo")
    public Result<UserInfoVO> getInfo() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Result.success(authService.getInfo(username));
    }

    @GetMapping("/getRouters")
    public Result<List<RouterVO>> getRouters() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Result.success(authService.getRouters(username));
    }
}
```

- [ ] **Step 10: fix the BCrypt seed hash now that `PasswordEncoder` is available**

Run a scratch check to generate the real hash and patch the DB directly (don't touch application code for this, it's a data fix):

```bash
cd backend
cat > /tmp/GenHash.java <<'EOF'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GenHash {
    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("admin123"));
    }
}
EOF
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
javac -cp "$(cat /tmp/cp.txt)" -d /tmp /tmp/GenHash.java
HASH=$(java -cp "/tmp:$(cat /tmp/cp.txt)" GenHash)
docker compose -f /home/ubuntu/proj/1yiling/docker-compose.yml exec -T mysql \
  mysql -uroot -proot123 -e "USE yiling; UPDATE sys_user SET passwd='${HASH}' WHERE username='admin';"
```
Expected: no errors; a bcrypt string starting with `$2a$` is written to `sys_user.passwd`. Also replace the placeholder hash literal inside `sql/init.sql` itself with this same value, so a fresh `docker compose down -v && up -d` still gets a working `admin` login.

- [ ] **Step 11: write the integration test**

```java
package com.yiling.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiling.modules.auth.dto.LoginDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_getInfo_getRouters_fullFlow() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPasswd("admin123");

        String body = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).get("data").get("token").asText();

        mockMvc.perform(get("/getInfo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("admin"));

        mockMvc.perform(get("/getRouters").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("系统管理"))
                .andExpect(jsonPath("$.data[0].children.length()").value(9));
    }

    @Test
    void getInfo_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/getInfo")).andExpect(status().isUnauthorized());
    }
}
```

This test requires the real MySQL container from Task 1 running on `127.0.0.1:3307` (it's a full-context integration test against the real DB, not a slice test — acceptable at this scale, matches the "verify against a running DB" spirit of the spec's verification plan).

- [ ] **Step 12: Run the test**

Run: `docker compose up -d && cd backend && mvn -q test -Dtest=AuthControllerIT`
Expected: both tests `PASS`.

- [ ] **Step 13: Commit**

```bash
git add backend/src sql/init.sql
git commit -m "feat(backend): JWT auth (login/getInfo/getRouters) wired to Spring Security"
```

---

### Task 4: Frontend Vite Skeleton + Request Wrapper + Login + Store

**Files:**
- Create: `frontend/package.json`, `frontend/vite.config.js`, `frontend/index.html`
- Create: `frontend/src/main.js`, `frontend/src/App.vue`
- Create: `frontend/src/utils/request.js`
- Create: `frontend/src/utils/auth.js`
- Create: `frontend/src/stores/user.js`
- Create: `frontend/src/directive/hasPermi.js`
- Create: `frontend/src/router/index.js`
- Create: `frontend/src/views/login/index.vue`
- Create: `frontend/src/views/error/404.vue`
- Create: `frontend/src/styles/index.scss`

**Interfaces:**
- Consumes: backend `/login`, `/getInfo`, `/getRouters` from Task 3 (proxied through Vite dev server at `/api` -> `http://127.0.0.1:8090`).
- Produces: `request(config) -> Promise<data>` (already unwrapped from `{code,message,data}`, rejects with the `message` string on non-200 `code` or HTTP error); `useUserStore()` Pinia store exposing `token, name, avatar, roles, permissions, login(), getInfo(), logout()`; `v-hasPermi="['system:user:add']"` directive. Every later frontend module's `api/system/*.js` file must call `request(...)` from `utils/request.js`, and every page needing button-permission must use `v-hasPermi` from this task.

- [ ] **Step 1: `frontend/package.json`**

```json
{
  "name": "yiling-frontend",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@element-plus/icons-vue": "^2.3.1",
    "@wangeditor/editor": "^5.1.23",
    "@wangeditor/editor-for-vue": "^5.1.12",
    "axios": "^1.7.9",
    "dayjs": "^1.11.13",
    "element-plus": "^2.8.8",
    "nprogress": "^0.2.0",
    "pinia": "^2.2.6",
    "screenfull": "^6.0.2",
    "vue": "^3.5.13",
    "vue-router": "^4.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.2.1",
    "sass": "^1.83.0",
    "vite": "^5.4.11"
  }
}
```

- [ ] **Step 2: `frontend/vite.config.js`**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8090',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})
```

- [ ] **Step 3: `frontend/index.html`**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <title>yiling-manager 后台管理系统</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

- [ ] **Step 4: `frontend/src/utils/auth.js`**

```js
const TOKEN_KEY = 'yiling_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
```

- [ ] **Step 5: `frontend/src/utils/request.js`**

```js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import router from '../router'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const { code, message, data } = response.data
    if (code !== 200) {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      removeToken()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default service
```

- [ ] **Step 6: `frontend/src/stores/user.js`**

```js
import { defineStore } from 'pinia'
import request from '../utils/request'
import { getToken, setToken, removeToken } from '../utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    name: '',
    nickName: '',
    avatar: '',
    roles: [],
    permissions: []
  }),
  actions: {
    async login(username, passwd) {
      const data = await request.post('/login', { username, passwd })
      this.token = data.token
      setToken(data.token)
    },
    async getInfo() {
      const data = await request.get('/getInfo')
      this.name = data.user.username
      this.nickName = data.user.nickName
      this.avatar = data.user.avatar
      this.roles = data.roles
      this.permissions = data.permissions
      return data
    },
    logout() {
      this.token = ''
      this.roles = []
      this.permissions = []
      removeToken()
    }
  }
})
```

- [ ] **Step 7: `frontend/src/directive/hasPermi.js`**

```js
import { useUserStore } from '../stores/user'

export default {
  mounted(el, binding) {
    const { value } = binding
    const userStore = useUserStore()
    const permissions = userStore.permissions || []
    if (userStore.roles.includes('admin')) return
    if (Array.isArray(value) && value.length > 0) {
      const hasPermission = value.some((p) => permissions.includes(p))
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  }
}
```

- [ ] **Step 8: `frontend/src/router/index.js`** — static routes only for this task; dynamic route injection from `getRouters` is added in Task 5 alongside the Layout component

```js
import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'

export const constantRoutes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/index.vue') },
  { path: '/404', name: '404', component: () => import('../views/error/404.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

router.beforeEach((to, from, next) => {
  const hasToken = getToken()
  if (to.path === '/login') {
    next()
    return
  }
  if (!hasToken) {
    next('/login')
    return
  }
  next()
})

export default router
```

- [ ] **Step 9: `frontend/src/views/login/index.vue`**

```vue
<template>
  <div class="login-container">
    <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
      <h3 class="title">yiling-manager 后台管理系统</h3>
      <el-form-item prop="username">
        <el-input v-model="form.username" placeholder="用户名" size="large" />
      </el-form-item>
      <el-form-item prop="passwd">
        <el-input v-model="form.passwd" type="password" placeholder="密码" size="large" show-password @keyup.enter="handleLogin" />
      </el-form-item>
      <el-button :loading="loading" type="primary" size="large" style="width:100%" @click="handleLogin">登录</el-button>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: 'admin', passwd: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  passwd: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function handleLogin() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(form.username, form.passwd)
      await userStore.getInfo()
      router.push('/')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container { display:flex; align-items:center; justify-content:center; height:100vh; background:#2d3a4b; }
.login-form { width:400px; padding:35px; background:#fff; border-radius:6px; }
.title { text-align:center; margin-bottom:24px; }
</style>
```

- [ ] **Step 10: `frontend/src/views/error/404.vue`**

```vue
<template>
  <div style="text-align:center; padding-top:120px;">
    <h1>404</h1>
    <p>页面不存在</p>
  </div>
</template>
```

- [ ] **Step 11: `frontend/src/styles/index.scss`**

```scss
html, body, #app { height: 100%; margin: 0; padding: 0; }
```

- [ ] **Step 12: `frontend/src/main.js`**

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import hasPermi from './directive/hasPermi'
import './styles/index.scss'

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.directive('hasPermi', hasPermi)
app.use(createPinia())
app.use(ElementPlus)
app.use(router)
app.mount('#app')
```

- [ ] **Step 13: `frontend/src/App.vue`**

```vue
<template>
  <router-view />
</template>
```

- [ ] **Step 14: install deps and verify dev server serves the login page**

Run:
```bash
cd frontend
npm install
npm run dev &
sleep 3
curl -s http://127.0.0.1:5173/ | grep -o '<title>[^<]*</title>'
kill %1
```
Expected: `<title>yiling-manager 后台管理系统</title>` printed, no install errors.

- [ ] **Step 15: Commit**

```bash
git add frontend/package.json frontend/vite.config.js frontend/index.html frontend/src
git commit -m "feat(frontend): Vite skeleton with request wrapper, Pinia user store, login page"
```

---

### Task 5: Layout Shell + Dynamic Routes from getRouters

**Files:**
- Create: `frontend/src/layout/index.vue`
- Create: `frontend/src/layout/components/Sidebar.vue`
- Create: `frontend/src/layout/components/Navbar.vue`
- Create: `frontend/src/layout/components/TagsView.vue`
- Create: `frontend/src/views/dashboard/index.vue`
- Modify: `frontend/src/router/index.js` (add dynamic-route loading)
- Modify: `frontend/src/stores/user.js` (no change needed — already exposes `getInfo`; add `routers` handling in a new store)
- Create: `frontend/src/stores/permission.js`

**Interfaces:**
- Consumes: `useUserStore()` from Task 4; `GET /getRouters` shape `{name,path,component,hidden,meta:{title,icon,noCache},children}` from Task 3.
- Produces: `usePermissionStore().generateRoutes()` returning Vue Router route objects added under the `Layout` shell; every Phase 1+ module view is expected to be reachable at `/system/{module}` once its `views/system/{module}/index.vue` file exists — Sidebar renders purely from the store's route list, no per-module frontend code is needed here.

- [ ] **Step 1: `frontend/src/stores/permission.js`** — converts backend `RouterVO[]` into Vue Router route configs, resolving `component: 'Layout'` to the actual Layout component and `component: 'system/user/index'` to a lazy import under `views/`

```js
import { defineStore } from 'pinia'
import request from '../utils/request'
import Layout from '../layout/index.vue'

const modules = import.meta.glob('../views/**/*.vue')

function resolveComponent(componentPath) {
  if (componentPath === 'Layout') return Layout
  const key = `../views/${componentPath}.vue`
  if (modules[key]) return modules[key]
  return () => import('../views/error/404.vue')
}

function convert(routerVOList) {
  return routerVOList.map((r) => {
    const route = {
      path: r.path,
      name: r.name,
      component: resolveComponent(r.component),
      meta: { title: r.meta?.title, icon: r.meta?.icon },
      children: r.children ? convert(r.children) : undefined
    }
    return route
  })
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({ routes: [] }),
  actions: {
    async generateRoutes() {
      const routerList = await request.get('/getRouters')
      const routes = convert(routerList)
      this.routes = routes
      return routes
    }
  }
})
```

- [ ] **Step 2: `frontend/src/layout/components/Sidebar.vue`**

```vue
<template>
  <el-menu :default-active="$route.path" router unique-opened class="sidebar-menu" background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF">
    <sidebar-item v-for="route in routes" :key="route.path" :item="route" :base-path="route.path" />
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { usePermissionStore } from '../../stores/permission'
import SidebarItem from './SidebarItem.vue'

const permissionStore = usePermissionStore()
const routes = computed(() => permissionStore.routes)
</script>

<style scoped>
.sidebar-menu { height: 100%; border-right: none; }
</style>
```

- [ ] **Step 3: `frontend/src/layout/components/SidebarItem.vue`** — recursive menu-tree renderer

```vue
<template>
  <template v-if="!item.children || item.children.length === 0">
    <el-menu-item :index="resolvePath(item.path)">
      <span>{{ item.meta?.title }}</span>
    </el-menu-item>
  </template>
  <el-sub-menu v-else :index="resolvePath(item.path)">
    <template #title><span>{{ item.meta?.title }}</span></template>
    <sidebar-item v-for="child in item.children" :key="child.path" :item="child" :base-path="resolvePath(item.path)" />
  </el-sub-menu>
</template>

<script setup>
const props = defineProps({ item: Object, basePath: String })
function resolvePath(p) {
  if (p.startsWith('/')) return p
  return `${props.basePath}/${p}`.replace(/\/+/g, '/')
}
</script>
```

- [ ] **Step 4: `frontend/src/layout/components/Navbar.vue`**

```vue
<template>
  <div class="navbar">
    <div class="breadcrumb">{{ $route.meta?.title }}</div>
    <el-dropdown class="user-dropdown">
      <span class="user-name">{{ userStore.nickName || userStore.name }}</span>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.navbar { height:50px; display:flex; align-items:center; justify-content:space-between; padding:0 16px; background:#fff; box-shadow:0 1px 4px rgba(0,0,0,.1); }
.user-name { cursor:pointer; }
</style>
```

- [ ] **Step 5: `frontend/src/layout/components/TagsView.vue`** (minimal — full drag/close behaviors are not required by the spec's acceptance criteria beyond "标签页缓存")

```vue
<template>
  <div class="tags-view">
    <el-tag v-for="tag in visitedViews" :key="tag.path" closable class="tag-item" :type="tag.path === $route.path ? '' : 'info'" @click="go(tag.path)" @close="close(tag)">
      {{ tag.title }}
    </el-tag>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const visitedViews = ref([])

watch(() => route.path, () => {
  if (route.meta?.title && !visitedViews.value.find((v) => v.path === route.path)) {
    visitedViews.value.push({ path: route.path, title: route.meta.title })
  }
}, { immediate: true })

function go(path) { router.push(path) }
function close(tag) {
  visitedViews.value = visitedViews.value.filter((v) => v.path !== tag.path)
  if (route.path === tag.path && visitedViews.value.length) {
    router.push(visitedViews.value[visitedViews.value.length - 1].path)
  }
}
</script>

<style scoped>
.tags-view { padding:6px 12px; background:#fff; border-bottom:1px solid #eee; }
.tag-item { margin-right:8px; cursor:pointer; }
</style>
```

- [ ] **Step 6: `frontend/src/layout/index.vue`**

```vue
<template>
  <div class="app-wrapper">
    <div class="sidebar-container"><Sidebar /></div>
    <div class="main-container">
      <Navbar />
      <TagsView />
      <div class="app-main">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import Sidebar from './components/Sidebar.vue'
import Navbar from './components/Navbar.vue'
import TagsView from './components/TagsView.vue'
</script>

<style scoped>
.app-wrapper { display:flex; height:100vh; }
.sidebar-container { width:210px; background:#304156; flex-shrink:0; overflow-y:auto; }
.main-container { flex:1; display:flex; flex-direction:column; overflow:hidden; }
.app-main { flex:1; overflow:auto; padding:16px; background:#f0f2f5; }
</style>
```

- [ ] **Step 7: `frontend/src/views/dashboard/index.vue`**

```vue
<template>
  <el-card>
    <h2>欢迎使用 yiling-manager 后台管理系统</h2>
    <p>当前登录：{{ userStore.nickName || userStore.name }}</p>
  </el-card>
</template>

<script setup>
import { useUserStore } from '../../stores/user'
const userStore = useUserStore()
</script>
```

- [ ] **Step 8: modify `frontend/src/router/index.js`** to inject dynamic routes under `Layout` after login, guarded so it only runs once per session

```js
import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/auth'
import { useUserStore } from '../stores/user'
import { usePermissionStore } from '../stores/permission'
import Layout from '../layout/index.vue'

export const constantRoutes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/index.vue') },
  { path: '/404', name: '404', component: () => import('../views/error/404.vue') },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/index.vue'), meta: { title: '首页' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

let dynamicRoutesLoaded = false

router.beforeEach(async (to, from, next) => {
  const hasToken = getToken()
  if (to.path === '/login') {
    next()
    return
  }
  if (!hasToken) {
    next('/login')
    return
  }
  const userStore = useUserStore()
  if (!userStore.name) {
    try {
      await userStore.getInfo()
    } catch {
      userStore.logout()
      next('/login')
      return
    }
  }
  if (!dynamicRoutesLoaded) {
    const permissionStore = usePermissionStore()
    const routes = await permissionStore.generateRoutes()
    routes.forEach((r) => router.addRoute('/'.length ? undefined : undefined))
    const rootRoute = router.getRoutes().find((r) => r.path === '/')
    routes.forEach((r) => router.addRoute(rootRoute.name || '/', r))
    dynamicRoutesLoaded = true
    next({ ...to, replace: true })
    return
  }
  next()
})

export default router
```

Note: `router.addRoute(parentNameOrPath, route)` requires the parent route to have a `name` if referenced by name — give the root `/` route in `constantRoutes` a `name: 'Layout'` and call `router.addRoute('Layout', r)` for each dynamic child instead of the placeholder logic above. Fix this concretely:

```js
// in constantRoutes, name the root route:
{ path: '/', name: 'Layout', component: Layout, redirect: '/dashboard', children: [...] }

// and in the guard, replace the addRoute block with:
routes.forEach((r) => router.addRoute('Layout', r))
```

- [ ] **Step 9: manual browser verification**

Run: `docker compose up -d && (cd backend && mvn spring-boot:run &) && (cd frontend && npm run dev &)`, then open `http://127.0.0.1:5173` in a browser (or via `claude-in-chrome`).

Expected: redirected to `/login`; log in with `admin`/`admin123`; redirected to `/dashboard` showing the welcome card; sidebar shows a "系统管理" group with 9 sub-items (用户管理/角色管理/菜单管理/部门管理/岗位管理/字典管理/参数设置/通知公告/操作日志) — clicking any of them 404s for now (their `views/system/*/index.vue` files don't exist until Phase 1/2), which is expected at this point. Logout returns to `/login` and clears the token.

- [ ] **Step 10: Commit**

```bash
git add frontend/src
git commit -m "feat(frontend): layout shell with dynamic sidebar built from getRouters"
```

---

## Phase 0 Definition of Done

- `docker compose up -d` → MySQL healthy with all 13 tables + seed data.
- `mvn test` in `backend/` passes `HealthControllerTest` and `AuthControllerIT`.
- `npm run dev` in `frontend/` serves a working login → dashboard flow with a correctly rendered, DB-driven sidebar (menu items 404 until later phases — expected).
- All 5 tasks committed to `develop` as separate commits.

Next: `docs/superpowers/plans/2026-08-27-phase1-rbac-core.md` (Dept, Post, Menu, Role, User modules).
