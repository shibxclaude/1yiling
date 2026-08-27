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

-- BCrypt hash of "admin123" (regenerated by Task 3 step 10 once PasswordEncoder is on the classpath)
INSERT INTO sys_user (id,dept_id,username,nick_name,passwd,sex,status,create_user,create_time)
VALUES (1,100,'admin','管理员','$2a$10$PePY7G.Bmja.7VdvTgOym.YxB1M3YPx.Y4f/ZBQoy07O3pv3pcrPG',2,1,'system',NOW());

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
