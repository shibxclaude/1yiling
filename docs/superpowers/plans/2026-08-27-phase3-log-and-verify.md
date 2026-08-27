# Phase 3: Operation Log, Navbar Polish, and Final Verification

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Task numbering continues from Phase 2 (ended at Task 14).

**Goal:** Add automatic operation-log capture (no changes needed to any existing controller — see Task 15's pointcut design), finish the two Navbar features Phase 0 deferred (fullscreen, size-switch — the spec's §六 layout requirements), then run the full acceptance walkthrough from the spec's verification plan against the whole system built across Phases 0-2.

**Architecture:** A single `@Aspect` with a pointcut matching `save`/`update`/`delete` methods on any class under `com.yiling.modules..controller` — this logs every mutating call across **all nine modules already built** without touching a single existing controller file.

**Tech Stack:** same as prior phases.

**Spec:** `docs/superpowers/specs/2026-08-27-system-management-design.md`

## Global Constraints

Same as Phases 0-2. One addition specific to this phase: `sys_oper_log` does **not** extend `BaseEntity` (it has no `createUser/createTime/updateUser/updateTime` columns — see Phase 0's DDL) and its own `status` column means operation success(0)/fail(1), unrelated to the `0/1/2` soft-delete convention used everywhere else.

---

## File Structure (this phase's additions)

```
backend/src/main/java/com/yiling/
├── common/aspect/{Log.java?, LogAspect.java}  -- see Task 15, Log.java is NOT used (pointcut-based, no annotation needed)
└── modules/log/{entity/SysOperLog,dto/SysOperLogDTO,mapper/SysOperLogMapper,service/...,controller/SysOperLogController}.java

frontend/src/
├── layout/components/Navbar.vue (modified — add fullscreen + size-switch)
├── api/system/log.js
└── views/system/sysLog/index.vue
```

---

### Task 15: Operation Log (AOP capture + CRUD + frontend)

**Files:**
- Create: `backend/.../common/aspect/LogAspect.java`
- Create: `backend/.../modules/log/entity/SysOperLog.java`
- Create: `backend/.../modules/log/dto/SysOperLogDTO.java`
- Create: `backend/.../modules/log/mapper/SysOperLogMapper.java`
- Create: `backend/.../modules/log/service/SysOperLogService.java` + `impl/SysOperLogServiceImpl.java`
- Create: `backend/.../modules/log/controller/SysOperLogController.java`
- Test: `backend/src/test/java/com/yiling/modules/log/SysOperLogControllerIT.java`
- Create: `frontend/src/api/system/log.js`
- Create: `frontend/src/views/system/sysLog/index.vue`

**Interfaces:**
- Produces: every `save`/`update`/`delete` call across every controller from Phases 1-2 gets a `sys_oper_log` row automatically — no other file needs modification for this to work. `POST rest/sysOperLog/{listPage,detailById,delete}` (delete restricted to users holding the `admin` role — checked via a direct `sys_user_role`/`sys_role` join, since `LoginUser`'s `roleKeys` aren't in the `SecurityContext` today per Phase 0 Task 3's simplified filter).

- [ ] **Step 1: `entity/SysOperLog.java`** — deliberately does NOT extend `BaseEntity`

```java
package com.yiling.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Integer businessType;
    private String method;
    private String requestMethod;
    private Integer operatorType;
    private Long operId;
    private String operName;
    private Long deptId;
    private String deptName;
    private String operUrl;
    private String operIp;
    private String operLocation;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operTime;
    private Long costTime;
}
```

- [ ] **Step 2: `dto/SysOperLogDTO.java`**

```java
package com.yiling.modules.log.dto;

import lombok.Data;

@Data
public class SysOperLogDTO {
    private String title;
    private Integer businessType;
    private String operName;
    private String operIp;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

- [ ] **Step 3: `mapper/SysOperLogMapper.java`**

```java
package com.yiling.modules.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.log.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
}
```

- [ ] **Step 4: `service/SysOperLogService.java` + `impl/SysOperLogServiceImpl.java`**

```java
package com.yiling.modules.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.log.dto.SysOperLogDTO;
import com.yiling.modules.log.entity.SysOperLog;

import java.util.List;

public interface SysOperLogService extends IService<SysOperLog> {
    PageResult<SysOperLog> listPage(SysOperLogDTO query);
    SysOperLog detailById(Long id);
    void delete(List<Long> ids);
}
```

```java
package com.yiling.modules.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.log.dto.SysOperLogDTO;
import com.yiling.modules.log.entity.SysOperLog;
import com.yiling.modules.log.mapper.SysOperLogMapper;
import com.yiling.modules.log.service.SysOperLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Override
    public PageResult<SysOperLog> listPage(SysOperLogDTO q) {
        LambdaQueryWrapper<SysOperLog> qw = new LambdaQueryWrapper<>();
        qw.like(q.getTitle() != null && !q.getTitle().isBlank(), SysOperLog::getTitle, q.getTitle());
        qw.eq(q.getBusinessType() != null, SysOperLog::getBusinessType, q.getBusinessType());
        qw.like(q.getOperName() != null && !q.getOperName().isBlank(), SysOperLog::getOperName, q.getOperName());
        qw.like(q.getOperIp() != null && !q.getOperIp().isBlank(), SysOperLog::getOperIp, q.getOperIp());
        qw.orderByDesc(SysOperLog::getOperTime);
        Page<SysOperLog> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public SysOperLog detailById(Long id) {
        SysOperLog log = getById(id);
        if (log == null) throw new BusinessException("日志不存在");
        return log;
    }

    @Override
    public void delete(List<Long> ids) {
        removeByIds(ids);
    }
}
```

- [ ] **Step 5: `controller/SysOperLogController.java`** — `admin`-only delete, checked directly (no roles in `SecurityContext` today)

```java
package com.yiling.modules.log.controller;

import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import com.yiling.modules.log.dto.SysOperLogDTO;
import com.yiling.modules.log.entity.SysOperLog;
import com.yiling.modules.log.service.SysOperLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/sysOperLog")
public class SysOperLogController {

    private final SysOperLogService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public SysOperLogController(SysOperLogService service) {
        this.service = service;
    }

    @PostMapping("listPage")
    public Result<PageResult<SysOperLog>> listPage(@RequestBody SysOperLogDTO query) {
        return Result.success(service.listPage(query));
    }

    @PostMapping("detailById")
    public Result<SysOperLog> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("delete")
    @SuppressWarnings("unchecked")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Integer isAdmin = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user u JOIN sys_user_role ur ON ur.user_id = u.id " +
                        "JOIN sys_role r ON r.id = ur.role_id WHERE u.username = ? AND r.role_key = 'admin'",
                Integer.class, username);
        if (isAdmin == null || isAdmin == 0) {
            throw new BusinessException("仅超级管理员可删除操作日志");
        }
        Object idsOrId = body.get("ids") != null ? body.get("ids") : List.of(body.get("id"));
        service.delete(((List<Object>) idsOrId).stream().map(o -> Long.valueOf(o.toString())).toList());
        return Result.success();
    }
}
```

- [ ] **Step 6: `common/aspect/LogAspect.java`** — the automatic capture; matches `save`/`update`/`delete` on any `*Controller` under `com.yiling.modules`

```java
package com.yiling.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yiling.modules.log.entity.SysOperLog;
import com.yiling.modules.log.mapper.SysOperLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysOperLogMapper operLogMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Pointcut("execution(public * com.yiling.modules..*Controller.save(..)) " +
            "|| execution(public * com.yiling.modules..*Controller.update(..)) " +
            "|| execution(public * com.yiling.modules..*Controller.delete(..))")
    public void mutatingEndpoint() {}

    @Around("mutatingEndpoint()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        int businessType = switch (methodName) {
            case "save" -> 1;
            case "update" -> 2;
            case "delete" -> 3;
            default -> 0;
        };
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String title = className.replace("Controller", "");

        SysOperLog log = new SysOperLog();
        log.setTitle(title);
        log.setBusinessType(businessType);
        log.setMethod(joinPoint.getSignature().toShortString());
        log.setOperatorType(1);
        log.setOperTime(LocalDateTime.now());

        var requestAttrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttrs != null) {
            HttpServletRequest request = requestAttrs.getRequest();
            log.setRequestMethod(request.getMethod());
            log.setOperUrl(request.getRequestURI());
            log.setOperIp(request.getRemoteAddr());
        }

        try {
            log.setOperParam(safeJson(joinPoint.getArgs()));
        } catch (Exception ignored) {
            log.setOperParam("<unserializable>");
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            String username = auth.getName();
            log.setOperName(username);
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, dept_id FROM sys_user WHERE username = ?", username);
                if (!rows.isEmpty()) {
                    log.setOperId(((Number) rows.get(0).get("id")).longValue());
                    Object deptId = rows.get(0).get("dept_id");
                    if (deptId != null) {
                        log.setDeptId(((Number) deptId).longValue());
                        List<String> deptNames = jdbcTemplate.queryForList(
                                "SELECT dept_name FROM sys_dept WHERE id = ?", String.class, deptId);
                        if (!deptNames.isEmpty()) log.setDeptName(deptNames.get(0));
                    }
                }
            } catch (Exception ignored) {}
        }

        try {
            Object result = joinPoint.proceed();
            log.setStatus(0);
            log.setJsonResult(safeJson(result));
            return result;
        } catch (Throwable t) {
            log.setStatus(1);
            log.setErrorMsg(t.getMessage());
            throw t;
        } finally {
            log.setCostTime(System.currentTimeMillis() - start);
            operLogMapper.insert(log);
        }
    }

    private String safeJson(Object o) throws com.fasterxml.jackson.core.JsonProcessingException {
        String json = objectMapper.writeValueAsString(o);
        return json.length() > 1900 ? json.substring(0, 1900) : json;
    }
}
```

- [ ] **Step 7: integration test** — verifies the aspect fires without any change to `SysPostController`

```java
package com.yiling.modules.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SysOperLogControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void savingAPost_automaticallyWritesOperLog() throws Exception {
        Map<String, Object> create = Map.of("postCode", "logtest", "postName", "日志测试岗位", "postSort", 1);
        mockMvc.perform(post("/rest/sysPost/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysOperLog/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"SysPost\",\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].businessType").value(1))
                .andExpect(jsonPath("$.data.records[0].status").value(0));
    }

    @Test
    @WithMockUser(username = "admin")
    void delete_asAdmin_succeeds() throws Exception {
        mockMvc.perform(post("/rest/sysOperLog/delete").contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[999999]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 8: run tests** — `mvn -q test -Dtest=SysOperLogControllerIT` — expect PASS. Also re-run the full backend suite once here to confirm the aspect didn't break any earlier test: `mvn -q test`.

- [ ] **Step 9: `frontend/src/api/system/log.js`**

```js
import request from '../../utils/request'

export function listLogPage(query) { return request.post('/rest/sysOperLog/listPage', query) }
export function getLog(id) { return request.post('/rest/sysOperLog/detailById', { id }) }
export function delLog(ids) { return request.post('/rest/sysOperLog/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
```

- [ ] **Step 10: `frontend/src/views/system/sysLog/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="操作人员"><el-input v-model="query.operName" clearable /></el-form-item>
      <el-form-item label="业务类型">
        <el-select v-model="query.businessType" clearable style="width:140px">
          <el-option label="其他" :value="0" /><el-option label="新增" :value="1" /><el-option label="修改" :value="2" /><el-option label="删除" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作IP"><el-input v-model="query.operIp" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="danger" :disabled="!selected.length" @click="handleBatchDelete">批量删除</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="日志编号" width="90" />
      <el-table-column prop="title" label="系统模块" width="140" />
      <el-table-column label="业务类型" width="90">
        <template #default="{ row }"><el-tag :type="['info','primary','warning','danger'][row.businessType] || 'info'">{{ ['其他','新增','修改','删除'][row.businessType] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="operName" label="操作人员" width="110" />
      <el-table-column prop="operIp" label="主机地址" width="140" />
      <el-table-column label="操作状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '成功' : '失败' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="operTime" label="操作时间" width="170" />
      <el-table-column prop="costTime" label="耗时(ms)" width="100" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">详细</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-drawer v-model="detailVisible" title="操作日志详情" size="500px">
      <el-descriptions :column="1" border v-if="detail">
        <el-descriptions-item label="系统模块">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ detail.method }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="操作人员">{{ detail.operName }}</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ detail.operUrl }}</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ detail.operIp }}</el-descriptions-item>
        <el-descriptions-item label="请求参数"><pre style="white-space:pre-wrap;">{{ detail.operParam }}</pre></el-descriptions-item>
        <el-descriptions-item label="返回结果"><pre style="white-space:pre-wrap;">{{ detail.jsonResult }}</pre></el-descriptions-item>
        <el-descriptions-item label="操作状态">{{ detail.status === 0 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="错误消息" v-if="detail.errorMsg">{{ detail.errorMsg }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ detail.operTime }}</el-descriptions-item>
        <el-descriptions-item label="消耗时间">{{ detail.costTime }} ms</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { listLogPage, getLog, delLog } from '../../../api/system/log'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ operName: '', businessType: null, operIp: '', pageNo: 1, pageSize: 10 })
const detailVisible = ref(false)
const detail = ref(null)

async function fetchList() {
  loading.value = true
  try {
    const data = await listLogPage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.operName = ''; query.businessType = null; query.operIp = ''; query.pageNo = 1; fetchList() }

async function openDetail(row) {
  detail.value = await getLog(row.id)
  detailVisible.value = true
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条日志吗？`, '提示', { type: 'warning' }).then(async () => {
    await delLog(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
```

- [ ] **Step 11: Commit**

```bash
git add backend/src frontend/src
git commit -m "feat: operation log — AOP auto-capture (no controller changes) + CRUD + frontend"
```

---

### Task 16: Navbar Polish (fullscreen, size-switch — spec §六 layout requirements deferred from Phase 0)

**Files:**
- Modify: `frontend/src/layout/components/Navbar.vue`
- Modify: `frontend/src/main.js` (wrap app root in `el-config-provider` for the size switch)

**Interfaces:** Produces a working screenfull toggle and a size dropdown (large/default/small) that actually resizes Element Plus components app-wide via `el-config-provider`.

- [ ] **Step 1: rewrite `frontend/src/layout/components/Navbar.vue`**

```vue
<template>
  <div class="navbar">
    <div class="breadcrumb">{{ $route.meta?.title }}</div>
    <div class="right-menu">
      <el-tooltip content="全屏">
        <el-icon class="right-menu-item" @click="toggleFullscreen"><FullScreen /></el-icon>
      </el-tooltip>
      <el-dropdown class="right-menu-item" @command="changeSize">
        <el-icon><Grid /></el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="large">大</el-dropdown-item>
            <el-dropdown-item command="default">默认</el-dropdown-item>
            <el-dropdown-item command="small">小</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-dropdown class="right-menu-item user-dropdown">
        <span class="user-name">{{ userStore.nickName || userStore.name }}</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
            <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import screenfull from 'screenfull'
import { FullScreen, Grid } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/user'
import { useAppStore } from '../../stores/app'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()

function toggleFullscreen() {
  if (screenfull.isEnabled) screenfull.toggle()
}

function changeSize(size) {
  appStore.size = size
}

function goProfile() { router.push('/user/profile') }

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.navbar { height:50px; display:flex; align-items:center; justify-content:space-between; padding:0 16px; background:#fff; box-shadow:0 1px 4px rgba(0,0,0,.1); }
.right-menu { display:flex; align-items:center; gap:16px; }
.right-menu-item { cursor:pointer; font-size:18px; }
.user-name { cursor:pointer; }
</style>
```

- [ ] **Step 2: `frontend/src/stores/app.js`**

```js
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({ size: 'default' })
})
```

- [ ] **Step 3: wrap the app in `el-config-provider` — modify `frontend/src/App.vue`**

```vue
<template>
  <el-config-provider :size="appStore.size">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { useAppStore } from './stores/app'
const appStore = useAppStore()
</script>
```

- [ ] **Step 4: manual verification**

Run the dev server, log in, click the fullscreen icon (browser goes fullscreen and back), click the size dropdown and confirm buttons/inputs across a page (e.g. `/system/post`) visibly shrink/grow.

- [ ] **Step 5: Commit**

```bash
git add frontend/src
git commit -m "feat(frontend): navbar fullscreen toggle and component-size switch"
```

---

### Task 17: Final End-to-End Verification (all 9 modules, full stack)

**Files:** none created — this task runs the spec's verification plan and fixes anything that breaks. Record findings inline in this file's checkboxes as you go; if a step fails, fix the root cause in its owning Phase's files (don't patch around it here) and re-run.

- [ ] **Step 1: fresh database bring-up**

```bash
cd /home/ubuntu/proj/1yiling
docker compose down -v
docker compose up -d
sleep 5
docker compose exec mysql mysql -uroot -proot123 -e "USE yiling; SELECT COUNT(*) FROM sys_menu; SELECT COUNT(*) FROM sys_user;"
```
Expected: 55 menus, 1 user, no errors.

- [ ] **Step 2: full backend test suite**

```bash
cd backend && mvn test
```
Expected: `BUILD SUCCESS`, every `*ControllerIT` and `HealthControllerTest` passes (this is the full regression check across all three phases' tests run together for the first time).

- [ ] **Step 3: start both servers**

```bash
cd backend && mvn spring-boot:run &
cd frontend && npm run dev &
```

- [ ] **Step 4: browser walkthrough** (use `claude-in-chrome` or an equivalent browser-driving tool — this is the step the spec's acceptance criteria explicitly require; a passing build is not sufficient on its own)

For **each** of the 9 modules (`/system/user`, `/system/role`, `/system/menu`, `/system/dept`, `/system/post`, `/system/dict`, `/system/config`, `/system/notice`, `/system/log`) confirm:
1. Login as `admin`/`admin123` lands on `/dashboard` with the correct sidebar (9 items).
2. The module's list page loads with no console errors.
3. Create → appears in the list. Edit → change persists. Delete (single + batch where applicable) → row disappears, with the confirm dialog shown first.
4. Pagination controls work where the module is paged.
5. Time columns display as `yyyy-MM-dd HH:mm:ss`.
6. For User: dept-tree filter narrows results; role/post multi-select in the dialog saves correctly; "更多→重置密码" and "分配角色" both work; Profile page (`/user/profile`) loads, saves basic info, changes password, and the avatar cropper uploads successfully.
7. For Role: menu-tree checkbox assignment persists (log out, create a second non-admin user with a restricted role, log back in as them, confirm the sidebar only shows their role's menus); data-scope dialog saves; "分配用户" sub-page adds/removes users.
8. For Menu/Dept: tree table renders correctly; "新增下级" pre-fills the parent; delete is blocked with an error toast when the node has children.
9. For Dict: type page → click a type → data page shows its entries; a page elsewhere (User's 性别 column, per Phase 2 Task 12's retrofit) renders via `useDict`/`<dict-tag>` correctly.
10. For Notice: rich-text editor loads and saves HTML content that renders back correctly on edit.
11. For Log: every create/edit/delete performed during this walkthrough shows up as a row; detail drawer opens; batch delete works (as `admin`).
12. Logout clears the session and redirects to `/login`; hitting a `/system/*` URL directly while logged out redirects to `/login`.
13. Fullscreen and size-switch (Task 16) both work.

- [ ] **Step 5: record and fix any failures found in Step 4**

For each failure: identify which Phase/Task's file is responsible, fix it there (not with a workaround in this task), re-run the specific affected `mvn test -Dtest=...` and re-check the browser flow, then commit the fix with a message referencing what broke, e.g. `fix(role): menu tree checked-keys not restored on edit dialog reopen`.

- [ ] **Step 6: final commit**

```bash
git add -A
git commit -m "chore: Phase 3 complete — full 9-module system verified end-to-end"
```

---

## Overall System Definition of Done

- All backend `*ControllerIT` tests pass via a single `mvn test` run in `backend/`.
- `docker compose up -d && npm run dev && mvn spring-boot:run` gives a system where all 9 modules in `docs/系统管理功能提示词.md` §五 are reachable and functional per that document's own §八 quality acceptance criteria.
- Every commit from Phase 0 through this task exists on the `develop` branch (per this user's standing convention — see project memory `1yiling-develop-default`).
