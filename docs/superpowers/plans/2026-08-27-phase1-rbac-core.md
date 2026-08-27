# Phase 1: RBAC Core Modules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Task numbering continues from Phase 0 (which ended at Task 5).

**Goal:** Build Dept, Post, Menu, Role, and User — the five interdependent modules that make the RBAC system actually manage itself (a role's menu tree, a user's dept/role/post assignment, data scope).

**Architecture:** Same layered backend (`controller/service/impl/mapper/entity/dto/vo`) per module under `com.yiling.modules.{dept,post,menu,role,user}`; same frontend shape (`api/system/*.js` + `views/system/*/index.vue`) per module. Dept and Menu render as **tree tables** using Element Plus `el-table`'s built-in `row-key`+`default-expand-all` tree mode (no custom recursive component needed for the table; the recursive `SidebarItem.vue`-style component from Phase 0 is only for the sidebar).

**Tech Stack:** same as Phase 0 — Spring Boot 3.4.3 / MyBatis-Plus 3.5.9 backend, Vue 3 / Element Plus 2.8 frontend.

**Spec:** `docs/superpowers/specs/2026-08-27-system-management-design.md`; functional field/endpoint source for User is `docs/系统管理功能提示词.md` §模块1 (already gives the full endpoint table verbatim — reuse it, don't reinvent).

## Global Constraints

(Same as Phase 0 — repeated here because a fresh worker on this file alone needs them.)
- Responses `{code,message,data}`; pagination `{pageNo,pageSize}` in / `{records,total,size,current,pages}` out. All endpoints `POST rest/{entity}/{action}`.
- `status`: `0=soft-deleted,1=normal,2=disabled`, except `sys_menu`/`sys_dept` which hard-delete (blocked if children exist).
- No server-side per-permission `@PreAuthorize` — only "valid token" is enforced (see Phase 0 Global Constraints for the full deferred list).
- Every entity extends `com.yiling.common.entity.BaseEntity`; every controller returns `Result<T>` or `Result<PageResult<T>>`.

---

## File Structure (this phase's additions)

```
backend/src/main/java/com/yiling/
├── common/vo/TreeSelectVO.java
└── modules/
    ├── dept/{entity/SysDept,dto/SysDeptDTO,vo/SysDeptTreeVO?,mapper/SysDeptMapper,service/...,controller/SysDeptController}.java
    ├── post/{entity/SysPost,dto/SysPostDTO,mapper/SysPostMapper,service/...,controller/SysPostController}.java
    ├── menu/{entity/SysMenu,dto/SysMenuDTO,vo/RoleMenuTreeVO,mapper/SysMenuMapper,service/...,controller/SysMenuController}.java
    ├── role/{entity/SysRole,dto/{SysRoleDTO,DataScopeDTO},vo/DeptTreeByRoleVO,mapper/{SysRoleMapper,SysRoleMenuMapper},service/...,controller/{SysRoleController,SysUserRoleController}}.java
    └── user/{entity/SysUser,dto/{SysUserDTO,ResetPwdDTO,UpdateSelfDTO,UpdatePwdDTO},vo/{SysUserVO,SysUserDetailVO},mapper/{SysUserMapper,SysUserRoleMapper,SysUserPostMapper},service/...,controller/SysUserController}.java

frontend/src/
├── api/system/{dept,post,menu,role,user}.js
├── components/{DeptTreeSelect.vue, RightToolbar.vue, Pagination.vue}
└── views/system/
    ├── dept/index.vue
    ├── post/index.vue
    ├── menu/index.vue
    ├── role/{index.vue, authUser.vue, selectUser.vue}
    └── user/{index.vue, authRole.vue}
```

---

### Task 6: Shared Components (`Pagination`, `RightToolbar`, `DeptTreeSelect`)

**Files:**
- Create: `frontend/src/components/Pagination.vue`
- Create: `frontend/src/components/RightToolbar.vue`
- Create: `frontend/src/components/DeptTreeSelect.vue`

**Interfaces:**
- Produces: `<Pagination v-model:page="pageNo" v-model:limit="pageSize" :total="total" @pagination="fetchList" />`; `<RightToolbar @queryTable="fetchList" v-model:showSearch="showSearch" />`; `<DeptTreeSelect v-model="form.deptId" />` (fetches `POST rest/sysDept/deptTree` internally). Every module task below uses all three.

- [ ] **Step 1: `Pagination.vue`**

```vue
<template>
  <el-pagination
    v-model:current-page="pageNo"
    v-model:page-size="pageSize"
    :total="total"
    :page-sizes="[10, 20, 50, 100]"
    layout="total, sizes, prev, pager, next, jumper"
    style="margin-top:12px; justify-content:flex-end; display:flex;"
    @size-change="emitChange"
    @current-change="emitChange"
  />
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ page: { type: Number, default: 1 }, limit: { type: Number, default: 10 }, total: { type: Number, default: 0 } })
const emit = defineEmits(['update:page', 'update:limit', 'pagination'])

const pageNo = ref(props.page)
const pageSize = ref(props.limit)

watch(() => props.page, (v) => { pageNo.value = v })
watch(() => props.limit, (v) => { pageSize.value = v })

function emitChange() {
  emit('update:page', pageNo.value)
  emit('update:limit', pageSize.value)
  emit('pagination', { pageNo: pageNo.value, pageSize: pageSize.value })
}
</script>
```

- [ ] **Step 2: `RightToolbar.vue`**

```vue
<template>
  <div class="right-toolbar">
    <el-tooltip content="刷新">
      <el-button circle :icon="Refresh" @click="$emit('queryTable')" />
    </el-tooltip>
    <el-tooltip content="隐藏/显示搜索">
      <el-button circle :icon="Search" @click="toggleSearch" />
    </el-tooltip>
  </div>
</template>

<script setup>
import { Refresh, Search } from '@element-plus/icons-vue'

const props = defineProps({ showSearch: { type: Boolean, default: true } })
const emit = defineEmits(['update:showSearch', 'queryTable'])

function toggleSearch() {
  emit('update:showSearch', !props.showSearch)
}
</script>

<style scoped>
.right-toolbar { display:flex; gap:8px; float:right; }
</style>
```

- [ ] **Step 3: `DeptTreeSelect.vue`**

```vue
<template>
  <el-tree-select
    v-model="innerValue"
    :data="deptTree"
    :props="{ label: 'label', children: 'children', value: 'id' }"
    node-key="id"
    check-strictly
    placeholder="选择归属部门"
    style="width:100%"
    @change="(v) => $emit('update:modelValue', v)"
  />
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import request from '../utils/request'

const props = defineProps({ modelValue: [Number, String] })
const emit = defineEmits(['update:modelValue'])
const innerValue = ref(props.modelValue)
const deptTree = ref([])

watch(() => props.modelValue, (v) => { innerValue.value = v })

onMounted(async () => {
  deptTree.value = await request.post('/rest/sysDept/deptTree', {})
})
</script>
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components
git commit -m "feat(frontend): shared Pagination/RightToolbar/DeptTreeSelect components"
```

---

### Task 7: Dept Module (backend + frontend)

**Files:**
- Create: `backend/src/main/java/com/yiling/common/vo/TreeSelectVO.java`
- Create: `backend/.../modules/dept/entity/SysDept.java`
- Create: `backend/.../modules/dept/dto/SysDeptDTO.java`
- Create: `backend/.../modules/dept/mapper/SysDeptMapper.java`
- Create: `backend/.../modules/dept/service/SysDeptService.java` + `impl/SysDeptServiceImpl.java`
- Create: `backend/.../modules/dept/controller/SysDeptController.java`
- Test: `backend/src/test/java/com/yiling/modules/dept/SysDeptControllerIT.java`
- Create: `frontend/src/api/system/dept.js`
- Create: `frontend/src/views/system/dept/index.vue`

**Interfaces:**
- Produces: `TreeSelectVO{id,label,children}` (reused by Menu/Role tasks below). Endpoints: `POST rest/sysDept/list {deptName?,status?} -> SysDept[]` (unpaged, full entity, for the tree-table page itself); `POST rest/sysDept/deptTree {} -> TreeSelectVO[]` (for pickers, Task 6's `DeptTreeSelect`); `POST rest/sysDept/save|update|delete|detailById` standard.

- [ ] **Step 1: `common/vo/TreeSelectVO.java`**

```java
package com.yiling.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TreeSelectVO {
    private Long id;
    private String label;
    private List<TreeSelectVO> children;
}
```

- [ ] **Step 2: `modules/dept/entity/SysDept.java`**

```java
package com.yiling.modules.dept.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String ancestors;
    private String deptCode;
    private String deptName;
    private Integer orderNum;
    private String leader;
    private String phone;
    private String email;
}
```

- [ ] **Step 3: `modules/dept/dto/SysDeptDTO.java`**

```java
package com.yiling.modules.dept.dto;

import lombok.Data;

@Data
public class SysDeptDTO {
    private Long id;
    private Long parentId;
    private String deptCode;
    private String deptName;
    private Integer orderNum;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
}
```

- [ ] **Step 4: `modules/dept/mapper/SysDeptMapper.java`**

```java
package com.yiling.modules.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.dept.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {
}
```

- [ ] **Step 5: `modules/dept/service/SysDeptService.java`**

```java
package com.yiling.modules.dept.service;

import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.dept.dto.SysDeptDTO;
import com.yiling.modules.dept.entity.SysDept;

import java.util.List;

public interface SysDeptService {
    List<SysDept> list(String deptName, Integer status);
    List<TreeSelectVO> deptTree();
    SysDept detailById(Long id);
    void save(SysDeptDTO dto);
    void update(SysDeptDTO dto);
    void delete(Long id);
}
```

- [ ] **Step 6: `modules/dept/service/impl/SysDeptServiceImpl.java`**

```java
package com.yiling.modules.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.dept.dto.SysDeptDTO;
import com.yiling.modules.dept.entity.SysDept;
import com.yiling.modules.dept.mapper.SysDeptMapper;
import com.yiling.modules.dept.service.SysDeptService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper mapper;

    public SysDeptServiceImpl(SysDeptMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<SysDept> list(String deptName, Integer status) {
        LambdaQueryWrapper<SysDept> qw = new LambdaQueryWrapper<>();
        qw.ne(SysDept::getStatus, 0);
        qw.like(deptName != null && !deptName.isBlank(), SysDept::getDeptName, deptName);
        qw.eq(status != null, SysDept::getStatus, status);
        qw.orderByAsc(SysDept::getOrderNum);
        return mapper.selectList(qw);
    }

    @Override
    public List<TreeSelectVO> deptTree() {
        List<SysDept> all = list(null, null);
        return buildTree(0L, all);
    }

    private List<TreeSelectVO> buildTree(Long parentId, List<SysDept> all) {
        return all.stream()
                .filter(d -> parentId.equals(d.getParentId()))
                .map(d -> new TreeSelectVO(d.getId(), d.getDeptName(), buildTree(d.getId(), all)))
                .collect(Collectors.toList());
    }

    @Override
    public SysDept detailById(Long id) {
        SysDept dept = mapper.selectById(id);
        if (dept == null) throw new BusinessException("部门不存在");
        return dept;
    }

    @Override
    public void save(SysDeptDTO dto) {
        SysDept entity = new SysDept();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        entity.setAncestors(buildAncestors(dto.getParentId()));
        mapper.insert(entity);
    }

    @Override
    public void update(SysDeptDTO dto) {
        SysDept entity = new SysDept();
        BeanUtils.copyProperties(dto, entity);
        entity.setAncestors(buildAncestors(dto.getParentId()));
        mapper.updateById(entity);
    }

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0) return "0";
        SysDept parent = mapper.selectById(parentId);
        if (parent == null) return "0";
        return parent.getAncestors() + "," + parent.getId();
    }

    @Override
    public void delete(Long id) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (count != null && count > 0) {
            throw new BusinessException("存在下级部门，不允许删除");
        }
        mapper.deleteById(id);
    }
}
```

- [ ] **Step 7: `modules/dept/controller/SysDeptController.java`**

```java
package com.yiling.modules.dept.controller;

import com.yiling.common.result.Result;
import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.dept.dto.SysDeptDTO;
import com.yiling.modules.dept.entity.SysDept;
import com.yiling.modules.dept.service.SysDeptService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/sysDept")
public class SysDeptController {

    private final SysDeptService service;

    public SysDeptController(SysDeptService service) {
        this.service = service;
    }

    @PostMapping("list")
    public Result<List<SysDept>> list(@RequestBody Map<String, Object> query) {
        String deptName = (String) query.get("deptName");
        Integer status = query.get("status") != null ? Integer.valueOf(query.get("status").toString()) : null;
        return Result.success(service.list(deptName, status));
    }

    @PostMapping("deptTree")
    public Result<List<TreeSelectVO>> deptTree() {
        return Result.success(service.deptTree());
    }

    @PostMapping("detailById")
    public Result<SysDept> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("save")
    public Result<Void> save(@RequestBody SysDeptDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Void> update(@RequestBody SysDeptDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("delete")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        service.delete(Long.valueOf(body.get("id").toString()));
        return Result.success();
    }
}
```

- [ ] **Step 8: integration test**

```java
package com.yiling.modules.dept;

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
class SysDeptControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void deptTree_containsSeededHeadOffice() throws Exception {
        mockMvc.perform(post("/rest/sysDept/deptTree").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].label").value("总公司"));
    }

    @Test
    @WithMockUser
    void delete_blockedWhenChildrenExist() throws Exception {
        Map<String, Object> child = Map.of("deptName", "子部门", "deptCode", "SUB1", "parentId", 100, "orderNum", 1, "status", 1);
        mockMvc.perform(post("/rest/sysDept/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(child)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysDept/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":100}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("存在下级部门，不允许删除"));
    }
}
```

- [ ] **Step 9: run tests**

Run: `cd backend && mvn -q test -Dtest=SysDeptControllerIT`
Expected: both PASS.

- [ ] **Step 10: `frontend/src/api/system/dept.js`**

```js
import request from '../../utils/request'

export function listDept(query) { return request.post('/rest/sysDept/list', query) }
export function deptTree() { return request.post('/rest/sysDept/deptTree', {}) }
export function getDept(id) { return request.post('/rest/sysDept/detailById', { id }) }
export function addDept(data) { return request.post('/rest/sysDept/save', data) }
export function updateDept(data) { return request.post('/rest/sysDept/update', data) }
export function delDept(id) { return request.post('/rest/sysDept/delete', { id }) }
```

- [ ] **Step 11: `frontend/src/views/system/dept/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="部门名称"><el-input v-model="query.deptName" placeholder="请输入部门名称" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
          <el-option label="正常" :value="1" /><el-option label="停用" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:dept:add']" @click="openDialog()">新增</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column prop="deptName" label="部门名称" width="220" />
      <el-table-column prop="orderNum" label="排序" width="80" />
      <el-table-column prop="leader" label="负责人" />
      <el-table-column prop="phone" label="联系电话" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:dept:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="primary" v-hasPermi="['system:dept:add']" @click="openDialog(null, row.id)">新增下级</el-button>
          <el-button link type="danger" v-hasPermi="['system:dept:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改部门' : '新增部门'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级部门" prop="parentId"><dept-tree-select v-model="form.parentId" /></el-form-item>
        <el-form-item label="部门编码" prop="deptCode"><el-input v-model="form.deptCode" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="部门名称" prop="deptName"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="显示顺序" prop="orderNum"><el-input-number v-model="form.orderNum" :min="0" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="form.leader" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDept, addDept, updateDept, delDept } from '../../../api/system/dept'
import RightToolbar from '../../../components/RightToolbar.vue'
import DeptTreeSelect from '../../../components/DeptTreeSelect.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const query = reactive({ deptName: '', status: null })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, parentId: 0, deptCode: '', deptName: '', orderNum: 0, leader: '', phone: '', email: '', status: 1 })
const rules = { deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }], deptCode: [{ required: true, message: '请输入部门编码', trigger: 'blur' }], orderNum: [{ required: true, message: '请输入显示顺序', trigger: 'blur' }] }

function toTree(list) {
  const map = {}
  list.forEach((d) => { map[d.id] = { ...d, children: [] } })
  const roots = []
  list.forEach((d) => {
    if (d.parentId && map[d.parentId]) map[d.parentId].children.push(map[d.id])
    else roots.push(map[d.id])
  })
  return roots
}

async function fetchList() {
  loading.value = true
  try {
    const list = await listDept(query)
    tableData.value = toTree(list)
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.deptName = ''; query.status = null; fetchList() }

function openDialog(row, presetParentId) {
  Object.assign(form, { id: null, parentId: presetParentId || 0, deptCode: '', deptName: '', orderNum: 0, leader: '', phone: '', email: '', status: 1 })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateDept(form)
    else await addDept(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除部门"${row.deptName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDept(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
```

- [ ] **Step 12: Commit**

```bash
git add backend/src frontend/src
git commit -m "feat: dept module (tree CRUD, backend+frontend)"
```

---

### Task 8: Post Module (backend + frontend) — reference pattern for plain CRUD modules

**Files:** (mirrors Task 7's shape minus the tree logic)
- Create: `backend/.../modules/post/entity/SysPost.java`
- Create: `backend/.../modules/post/dto/SysPostDTO.java`
- Create: `backend/.../modules/post/mapper/SysPostMapper.java`
- Create: `backend/.../modules/post/service/SysPostService.java` + `impl/SysPostServiceImpl.java`
- Create: `backend/.../modules/post/controller/SysPostController.java`
- Test: `backend/src/test/java/com/yiling/modules/post/SysPostControllerIT.java`
- Create: `frontend/src/api/system/post.js`
- Create: `frontend/src/views/system/post/index.vue`

**Interfaces:** `POST rest/sysPost/{listPage,detailById,save,update,delete,updateSimple}`. **This is the reference implementation every later plain-CRUD module (Dict data, Config, Notice) in Phase 2 must copy the shape of** — same 5-file backend layout using `IService`/`ServiceImpl` (not hand-rolled like Dept), same frontend page layout (search form → toolbar → paged table → dialog).

- [ ] **Step 1: `entity/SysPost.java`**

```java
package com.yiling.modules.post.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
public class SysPost extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String postCode;
    private String postName;
    private Integer postSort;
    private String remark;
}
```

- [ ] **Step 2: `dto/SysPostDTO.java`**

```java
package com.yiling.modules.post.dto;

import lombok.Data;

@Data
public class SysPostDTO {
    private Long id;
    private String postCode;
    private String postName;
    private Integer postSort;
    private String remark;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

- [ ] **Step 3: `mapper/SysPostMapper.java`**

```java
package com.yiling.modules.post.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.post.entity.SysPost;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysPostMapper extends BaseMapper<SysPost> {
}
```

- [ ] **Step 4: `service/SysPostService.java` + `impl/SysPostServiceImpl.java`** — uses MyBatis-Plus `IService`/`ServiceImpl` to get `page()`/`save()`/`updateById()`/`getById()` for free; this is the pattern to copy for Dict/Config/Notice

```java
package com.yiling.modules.post.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.post.dto.SysPostDTO;
import com.yiling.modules.post.entity.SysPost;

import java.util.List;

public interface SysPostService extends IService<SysPost> {
    PageResult<SysPost> listPage(SysPostDTO query);
    List<SysPost> list(SysPostDTO query);
    SysPost detailById(Long id);
    void save(SysPostDTO dto);
    void update(SysPostDTO dto);
    void delete(List<Long> ids);
    void updateSimple(Long id, Integer status);
}
```

```java
package com.yiling.modules.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.post.dto.SysPostDTO;
import com.yiling.modules.post.entity.SysPost;
import com.yiling.modules.post.mapper.SysPostMapper;
import com.yiling.modules.post.service.SysPostService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements SysPostService {

    private LambdaQueryWrapper<SysPost> baseQuery(SysPostDTO q) {
        LambdaQueryWrapper<SysPost> qw = new LambdaQueryWrapper<>();
        qw.ne(SysPost::getStatus, 0);
        qw.like(q.getPostCode() != null && !q.getPostCode().isBlank(), SysPost::getPostCode, q.getPostCode());
        qw.like(q.getPostName() != null && !q.getPostName().isBlank(), SysPost::getPostName, q.getPostName());
        qw.eq(q.getStatus() != null, SysPost::getStatus, q.getStatus());
        qw.orderByAsc(SysPost::getPostSort);
        return qw;
    }

    @Override
    public PageResult<SysPost> listPage(SysPostDTO query) {
        Page<SysPost> page = new Page<>(query.getPageNo(), query.getPageSize());
        return PageResult.of(page(page, baseQuery(query)));
    }

    @Override
    public List<SysPost> list(SysPostDTO query) {
        return list(baseQuery(query));
    }

    @Override
    public SysPost detailById(Long id) {
        SysPost post = getById(id);
        if (post == null) throw new BusinessException("岗位不存在");
        return post;
    }

    @Override
    public void save(SysPostDTO dto) {
        SysPost entity = new SysPost();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysPostDTO dto) {
        SysPost entity = new SysPost();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysPost entity = new SysPost();
            entity.setId(id);
            entity.setStatus(0);
            updateById(entity);
        }
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysPost entity = new SysPost();
        entity.setId(id);
        entity.setStatus(status);
        updateById(entity);
    }
}
```

- [ ] **Step 5: `controller/SysPostController.java`**

```java
package com.yiling.modules.post.controller;

import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import com.yiling.modules.post.dto.SysPostDTO;
import com.yiling.modules.post.entity.SysPost;
import com.yiling.modules.post.service.SysPostService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/sysPost")
public class SysPostController {

    private final SysPostService service;

    public SysPostController(SysPostService service) {
        this.service = service;
    }

    @PostMapping("listPage")
    public Result<PageResult<SysPost>> listPage(@RequestBody SysPostDTO query) {
        return Result.success(service.listPage(query));
    }

    @PostMapping("list")
    public Result<List<SysPost>> list(@RequestBody SysPostDTO query) {
        return Result.success(service.list(query));
    }

    @PostMapping("detailById")
    public Result<SysPost> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("save")
    public Result<Void> save(@RequestBody SysPostDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Void> update(@RequestBody SysPostDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("delete")
    @SuppressWarnings("unchecked")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        Object idsOrId = body.get("ids") != null ? body.get("ids") : List.of(body.get("id"));
        List<Long> ids = ((List<Object>) idsOrId).stream().map(o -> Long.valueOf(o.toString())).toList();
        service.delete(ids);
        return Result.success();
    }

    @PostMapping("updateSimple")
    public Result<Void> updateSimple(@RequestBody Map<String, Object> body) {
        service.updateSimple(Long.valueOf(body.get("id").toString()), Integer.valueOf(body.get("status").toString()));
        return Result.success();
    }
}
```

- [ ] **Step 6: integration test**

```java
package com.yiling.modules.post;

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
class SysPostControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void crud_roundTrip() throws Exception {
        Map<String, Object> create = Map.of("postCode", "test1", "postName", "测试岗位", "postSort", 1);
        mockMvc.perform(post("/rest/sysPost/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysPost/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"postName\":\"测试\",\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].postCode").value("test1"));
    }
}
```

- [ ] **Step 7: run tests** — `cd backend && mvn -q test -Dtest=SysPostControllerIT` — expect PASS.

- [ ] **Step 8: `frontend/src/api/system/post.js`**

```js
import request from '../../utils/request'

export function listPostPage(query) { return request.post('/rest/sysPost/listPage', query) }
export function listPost(query) { return request.post('/rest/sysPost/list', query || {}) }
export function getPost(id) { return request.post('/rest/sysPost/detailById', { id }) }
export function addPost(data) { return request.post('/rest/sysPost/save', data) }
export function updatePost(data) { return request.post('/rest/sysPost/update', data) }
export function delPost(ids) { return request.post('/rest/sysPost/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
export function updatePostStatus(id, status) { return request.post('/rest/sysPost/updateSimple', { id, status }) }
```

- [ ] **Step 9: `frontend/src/views/system/post/index.vue`** — this is the reference page layout every Phase 2 CRUD page copies (search form → toolbar → table+pagination → dialog)

```vue
<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="岗位编码"><el-input v-model="query.postCode" clearable /></el-form-item>
      <el-form-item label="岗位名称"><el-input v-model="query.postName" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width:120px">
          <el-option label="正常" :value="1" /><el-option label="停用" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:post:add']" @click="openDialog()">新增</el-button>
      <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:post:remove']" @click="handleBatchDelete">批量删除</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="postCode" label="岗位编码" />
      <el-table-column prop="postName" label="岗位名称" />
      <el-table-column prop="postSort" label="显示顺序" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:post:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="danger" v-hasPermi="['system:post:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改岗位' : '新增岗位'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="岗位编码" prop="postCode"><el-input v-model="form.postCode" /></el-form-item>
        <el-form-item label="岗位名称" prop="postName"><el-input v-model="form.postName" /></el-form-item>
        <el-form-item label="显示顺序" prop="postSort"><el-input-number v-model="form.postSort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPostPage, addPost, updatePost, delPost } from '../../../api/system/post'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ postCode: '', postName: '', status: null, pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, postCode: '', postName: '', postSort: 0, status: 1, remark: '' })
const rules = { postCode: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }], postName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }], postSort: [{ required: true, message: '请输入显示顺序', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listPostPage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.postCode = ''; query.postName = ''; query.status = null; query.pageNo = 1; fetchList() }

function openDialog(row) {
  Object.assign(form, { id: null, postCode: '', postName: '', postSort: 0, status: 1, remark: '' })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updatePost(form)
    else await addPost(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除岗位"${row.postName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delPost(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    await delPost(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
```

- [ ] **Step 10: Commit**

```bash
git add backend/src frontend/src
git commit -m "feat: post module (backend+frontend) — reference CRUD pattern"
```

---

### Task 9: Menu Module (backend + frontend)

**Files:**
- Create: `backend/.../modules/menu/entity/SysMenu.java`
- Create: `backend/.../modules/menu/dto/SysMenuDTO.java`
- Create: `backend/.../modules/menu/vo/RoleMenuTreeVO.java`
- Create: `backend/.../modules/menu/mapper/SysMenuMapper.java`
- Create: `backend/.../modules/menu/service/SysMenuService.java` + `impl/SysMenuServiceImpl.java`
- Create: `backend/.../modules/menu/controller/SysMenuController.java`
- Test: `backend/src/test/java/com/yiling/modules/menu/SysMenuControllerIT.java`
- Create: `frontend/src/api/system/menu.js`
- Create: `frontend/src/views/system/menu/index.vue`
- Create: `frontend/src/components/IconSelect.vue`

**Interfaces:**
- Consumes: `TreeSelectVO` from Task 7.
- Produces: `RoleMenuTreeVO{checkedKeys:List<Long>, menus:List<TreeSelectVO>}`. Endpoints: `POST rest/sysMenu/list {menuName?,status?} -> SysMenu[]` (unpaged, tree table); `POST rest/sysMenu/treeSelect {} -> TreeSelectVO[]` (menu picker, used by Task 10 Role dialog... actually Role uses `roleMenuTreeSelect`, `treeSelect` is the generic "pick a parent menu" selector used by this module's own "新增下级"); `POST rest/sysMenu/roleMenuTreeSelect {roleId} -> RoleMenuTreeVO` (Task 10 Role dialog depends on this exact shape); `POST rest/sysMenu/{save,update,delete,detailById}`.

- [ ] **Step 1: `entity/SysMenu.java`**

```java
package com.yiling.modules.menu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType; // 0目录 1菜单 2按钮
    private Integer menuSort;
    private String icon;
    private String menuPath;
    private String menuComponent;
    private String perms;
    private String queryParam;
    private Boolean ifFrame;
    private Boolean ifCache;
}
```

- [ ] **Step 2: `dto/SysMenuDTO.java`**

```java
package com.yiling.modules.menu.dto;

import lombok.Data;

@Data
public class SysMenuDTO {
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private Integer menuSort;
    private String icon;
    private String menuPath;
    private String menuComponent;
    private String perms;
    private String queryParam;
    private Boolean ifFrame;
    private Boolean ifCache;
    private Integer status;
}
```

- [ ] **Step 3: `vo/RoleMenuTreeVO.java`**

```java
package com.yiling.modules.menu.vo;

import com.yiling.common.vo.TreeSelectVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoleMenuTreeVO {
    private List<Long> checkedKeys;
    private List<TreeSelectVO> menus;
}
```

- [ ] **Step 4: `mapper/SysMenuMapper.java`**

```java
package com.yiling.modules.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.menu.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRole(@Param("roleId") Long roleId);
}
```

- [ ] **Step 5: `service/SysMenuService.java` + `impl/SysMenuServiceImpl.java`**

```java
package com.yiling.modules.menu.service;

import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.menu.dto.SysMenuDTO;
import com.yiling.modules.menu.entity.SysMenu;
import com.yiling.modules.menu.vo.RoleMenuTreeVO;

import java.util.List;

public interface SysMenuService {
    List<SysMenu> list(String menuName, Integer status);
    List<TreeSelectVO> treeSelect();
    RoleMenuTreeVO roleMenuTreeSelect(Long roleId);
    SysMenu detailById(Long id);
    void save(SysMenuDTO dto);
    void update(SysMenuDTO dto);
    void delete(Long id);
}
```

```java
package com.yiling.modules.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.menu.dto.SysMenuDTO;
import com.yiling.modules.menu.entity.SysMenu;
import com.yiling.modules.menu.mapper.SysMenuMapper;
import com.yiling.modules.menu.service.SysMenuService;
import com.yiling.modules.menu.vo.RoleMenuTreeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper mapper;

    public SysMenuServiceImpl(SysMenuMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<SysMenu> list(String menuName, Integer status) {
        LambdaQueryWrapper<SysMenu> qw = new LambdaQueryWrapper<>();
        qw.ne(SysMenu::getStatus, 0);
        qw.like(menuName != null && !menuName.isBlank(), SysMenu::getMenuName, menuName);
        qw.eq(status != null, SysMenu::getStatus, status);
        qw.orderByAsc(SysMenu::getMenuSort);
        return mapper.selectList(qw);
    }

    @Override
    public List<TreeSelectVO> treeSelect() {
        List<SysMenu> all = mapper.selectList(new LambdaQueryWrapper<SysMenu>().ne(SysMenu::getStatus, 0).orderByAsc(SysMenu::getMenuSort));
        return buildTree(0L, all);
    }

    private List<TreeSelectVO> buildTree(Long parentId, List<SysMenu> all) {
        return all.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> new TreeSelectVO(m.getId(), m.getMenuName(), buildTree(m.getId(), all)))
                .collect(Collectors.toList());
    }

    @Override
    public RoleMenuTreeVO roleMenuTreeSelect(Long roleId) {
        List<Long> checked = mapper.selectMenuIdsByRole(roleId);
        return new RoleMenuTreeVO(checked, treeSelect());
    }

    @Override
    public SysMenu detailById(Long id) {
        SysMenu menu = mapper.selectById(id);
        if (menu == null) throw new BusinessException("菜单不存在");
        return menu;
    }

    @Override
    public void save(SysMenuDTO dto) {
        SysMenu entity = new SysMenu();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        normalizeUniqueFields(entity);
        mapper.insert(entity);
    }

    @Override
    public void update(SysMenuDTO dto) {
        SysMenu entity = new SysMenu();
        BeanUtils.copyProperties(dto, entity);
        normalizeUniqueFields(entity);
        mapper.updateById(entity);
    }

    // store NULL instead of "" for unique-indexed columns so multiple buttons with no path don't collide
    private void normalizeUniqueFields(SysMenu entity) {
        if (entity.getMenuPath() != null && entity.getMenuPath().isBlank()) entity.setMenuPath(null);
        if (entity.getMenuComponent() != null && entity.getMenuComponent().isBlank()) entity.setMenuComponent(null);
        if (entity.getPerms() != null && entity.getPerms().isBlank()) entity.setPerms(null);
    }

    @Override
    public void delete(Long id) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (count != null && count > 0) {
            throw new BusinessException("存在子菜单，不允许删除");
        }
        mapper.deleteById(id);
    }
}
```

- [ ] **Step 6: `controller/SysMenuController.java`**

```java
package com.yiling.modules.menu.controller;

import com.yiling.common.result.Result;
import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.menu.dto.SysMenuDTO;
import com.yiling.modules.menu.entity.SysMenu;
import com.yiling.modules.menu.service.SysMenuService;
import com.yiling.modules.menu.vo.RoleMenuTreeVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/sysMenu")
public class SysMenuController {

    private final SysMenuService service;

    public SysMenuController(SysMenuService service) {
        this.service = service;
    }

    @PostMapping("list")
    public Result<List<SysMenu>> list(@RequestBody Map<String, Object> query) {
        String menuName = (String) query.get("menuName");
        Integer status = query.get("status") != null ? Integer.valueOf(query.get("status").toString()) : null;
        return Result.success(service.list(menuName, status));
    }

    @PostMapping("treeSelect")
    public Result<List<TreeSelectVO>> treeSelect() {
        return Result.success(service.treeSelect());
    }

    @PostMapping("roleMenuTreeSelect")
    public Result<RoleMenuTreeVO> roleMenuTreeSelect(@RequestBody Map<String, Object> body) {
        return Result.success(service.roleMenuTreeSelect(Long.valueOf(body.get("roleId").toString())));
    }

    @PostMapping("detailById")
    public Result<SysMenu> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("save")
    public Result<Void> save(@RequestBody SysMenuDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Void> update(@RequestBody SysMenuDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("delete")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        service.delete(Long.valueOf(body.get("id").toString()));
        return Result.success();
    }
}
```

- [ ] **Step 7: integration test**

```java
package com.yiling.modules.menu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SysMenuControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    void roleMenuTreeSelect_adminHasAllMenusChecked() throws Exception {
        mockMvc.perform(post("/rest/sysMenu/roleMenuTreeSelect").contentType(MediaType.APPLICATION_JSON).content("{\"roleId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedKeys.length()").value(50));
    }

    @Test
    @WithMockUser
    void delete_blockedWhenChildMenuExists() throws Exception {
        mockMvc.perform(post("/rest/sysMenu/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("存在子菜单，不允许删除"));
    }
}
```

- [ ] **Step 8: run tests** — `mvn -q test -Dtest=SysMenuControllerIT` — expect PASS.

- [ ] **Step 9: `frontend/src/components/IconSelect.vue`** (minimal — a searchable grid over the Element Plus icon set already globally registered in Phase 0)

```vue
<template>
  <el-popover trigger="click" width="320">
    <template #reference>
      <el-input v-model="innerValue" placeholder="点击选择图标" readonly>
        <template #prefix><component :is="innerValue" v-if="innerValue" style="width:16px" /></template>
      </el-input>
    </template>
    <div class="icon-grid">
      <div v-for="name in iconNames" :key="name" class="icon-cell" @click="select(name)">
        <component :is="name" style="width:20px" />
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, watch } from 'vue'
import * as Icons from '@element-plus/icons-vue'

const props = defineProps({ modelValue: String })
const emit = defineEmits(['update:modelValue'])
const innerValue = ref(props.modelValue)
const iconNames = Object.keys(Icons)

watch(() => props.modelValue, (v) => { innerValue.value = v })

function select(name) {
  innerValue.value = name
  emit('update:modelValue', name)
}
</script>

<style scoped>
.icon-grid { display:grid; grid-template-columns:repeat(8,1fr); gap:8px; max-height:240px; overflow-y:auto; }
.icon-cell { cursor:pointer; display:flex; justify-content:center; padding:4px; }
.icon-cell:hover { background:#f0f2f5; }
</style>
```

- [ ] **Step 10: `frontend/src/api/system/menu.js`**

```js
import request from '../../utils/request'

export function listMenu(query) { return request.post('/rest/sysMenu/list', query || {}) }
export function menuTreeSelect() { return request.post('/rest/sysMenu/treeSelect', {}) }
export function getMenu(id) { return request.post('/rest/sysMenu/detailById', { id }) }
export function addMenu(data) { return request.post('/rest/sysMenu/save', data) }
export function updateMenu(data) { return request.post('/rest/sysMenu/update', data) }
export function delMenu(id) { return request.post('/rest/sysMenu/delete', { id }) }
```

- [ ] **Step 11: `frontend/src/views/system/menu/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="菜单名称"><el-input v-model="query.menuName" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width:120px">
          <el-option label="正常" :value="1" /><el-option label="隐藏" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:menu:add']" @click="openDialog()">新增</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column prop="menuName" label="菜单名称" width="220" />
      <el-table-column label="图标" width="70"><template #default="{ row }"><component :is="row.icon" v-if="row.icon" style="width:18px" /></template></el-table-column>
      <el-table-column prop="menuSort" label="排序" width="70" />
      <el-table-column prop="perms" label="权限标识" />
      <el-table-column prop="menuComponent" label="组件路径" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '隐藏' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:menu:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="primary" v-hasPermi="['system:menu:add']" @click="openDialog(null, row.id)">新增下级</el-button>
          <el-button link type="danger" v-hasPermi="['system:menu:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改菜单' : '新增菜单'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单"><el-input :model-value="form.parentId" disabled /></el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.menuType">
            <el-radio value="0">目录</el-radio><el-radio value="1">菜单</el-radio><el-radio value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图标" v-if="form.menuType !== '2'"><icon-select v-model="form.icon" /></el-form-item>
        <el-form-item label="名称" prop="menuName"><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item label="排序" prop="menuSort"><el-input-number v-model="form.menuSort" :min="0" /></el-form-item>
        <template v-if="form.menuType !== '2'">
          <el-form-item label="是否外链"><el-switch v-model="form.ifFrame" /></el-form-item>
          <el-form-item label="路由地址" v-if="form.menuType === '1'"><el-input v-model="form.menuPath" /></el-form-item>
          <el-form-item label="组件路径" v-if="form.menuType === '1'"><el-input v-model="form.menuComponent" placeholder="如 system/user/index" /></el-form-item>
          <el-form-item label="路由参数"><el-input v-model="form.queryParam" /></el-form-item>
          <el-form-item label="是否缓存" v-if="form.menuType === '1'"><el-switch v-model="form.ifCache" /></el-form-item>
        </template>
        <el-form-item label="权限字符" v-if="form.menuType !== '0'"><el-input v-model="form.perms" placeholder="system:user:add" /></el-form-item>
        <el-form-item label="显示状态">
          <el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">隐藏</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMenu, addMenu, updateMenu, delMenu } from '../../../api/system/menu'
import RightToolbar from '../../../components/RightToolbar.vue'
import IconSelect from '../../../components/IconSelect.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const query = reactive({ menuName: '', status: null })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, parentId: 0, menuName: '', menuType: '1', menuSort: 0, icon: '', menuPath: '', menuComponent: '', perms: '', queryParam: '', ifFrame: false, ifCache: false, status: 1 })
const rules = { menuName: [{ required: true, message: '请输入名称', trigger: 'blur' }], menuSort: [{ required: true, message: '请输入排序', trigger: 'blur' }] }

function toTree(list) {
  const map = {}
  list.forEach((m) => { map[m.id] = { ...m, children: [] } })
  const roots = []
  list.forEach((m) => {
    if (m.parentId && map[m.parentId]) map[m.parentId].children.push(map[m.id])
    else roots.push(map[m.id])
  })
  return roots
}

async function fetchList() {
  loading.value = true
  try {
    const list = await listMenu(query)
    tableData.value = toTree(list)
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.menuName = ''; query.status = null; fetchList() }

function openDialog(row, presetParentId) {
  Object.assign(form, { id: null, parentId: presetParentId || 0, menuName: '', menuType: '1', menuSort: 0, icon: '', menuPath: '', menuComponent: '', perms: '', queryParam: '', ifFrame: false, ifCache: false, status: 1 })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateMenu(form)
    else await addMenu(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除菜单"${row.menuName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delMenu(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
```

- [ ] **Step 12: Commit**

```bash
git add backend/src frontend/src
git commit -m "feat: menu module (tree CRUD + roleMenuTreeSelect, backend+frontend)"
```

---

### Task 10: Role Module (backend + frontend, incl. data-scope + user-assignment sub-pages)

**Files:**
- Create: `backend/.../modules/role/entity/SysRole.java`
- Create: `backend/.../modules/role/dto/{SysRoleDTO,DataScopeDTO}.java`
- Create: `backend/.../modules/role/vo/DeptTreeByRoleVO.java`
- Create: `backend/.../modules/role/mapper/{SysRoleMapper,SysRoleMenuMapper}.java`
- Create: `backend/.../modules/role/service/SysRoleService.java` + `impl/SysRoleServiceImpl.java`
- Create: `backend/.../modules/role/controller/{SysRoleController,SysUserRoleController}.java`
- Test: `backend/src/test/java/com/yiling/modules/role/SysRoleControllerIT.java`
- Create: `frontend/src/api/system/role.js`
- Create: `frontend/src/views/system/role/{index.vue,authUser.vue,selectUser.vue}`

**Interfaces:**
- Consumes: `RoleMenuTreeVO`/`treeSelect` from Task 9; `TreeSelectVO`/`deptTree` from Task 7.
- Produces: `POST rest/sysRole/{listPage,list,detailById,save,update,delete,updateSimple}` (standard, `save`/`update` accept `menuIds:List<Long>`); `POST rest/dataPerm/dataScope {id,dataScope,deptIdList}`; `POST rest/dataPerm/deptTreeByRole {roleId} -> DeptTreeByRoleVO{checkedKeys,depts}`; `POST rest/sysUserRole/allocatedList {roleId,pageNo,pageSize,username?,phone?} -> PageResult<UserBrief>`; `POST rest/sysUserRole/unallocatedList` (same shape, users NOT yet in the role); `POST rest/sysUserRole/cancel {userId,roleId}`; `POST rest/sysUserRole/cancelAll {roleId,userIds}`; `POST rest/sysUserRole/selectUserAll {roleId,userIds}`. Task 11 (User module) must NOT duplicate these role-assignment endpoints — it only needs `roleIds`/`postIds` arrays inside `SysUser` save/update.

- [ ] **Step 1: `entity/SysRole.java`**

```java
package com.yiling.modules.role.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String deptIds;
    private Boolean menuCheckStrictly;
    private Boolean deptCheckStrictly;
    private String remark;
}
```

- [ ] **Step 2: `dto/SysRoleDTO.java` + `dto/DataScopeDTO.java`**

```java
package com.yiling.modules.role.dto;

import lombok.Data;

import java.util.List;

@Data
public class SysRoleDTO {
    private Long id;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private Integer status;
    private String remark;
    private List<Long> menuIds;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

```java
package com.yiling.modules.role.dto;

import lombok.Data;

import java.util.List;

@Data
public class DataScopeDTO {
    private Long id;
    private String dataScope;
    private List<Long> deptIdList;
}
```

- [ ] **Step 3: `vo/DeptTreeByRoleVO.java`**

```java
package com.yiling.modules.role.vo;

import com.yiling.common.vo.TreeSelectVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DeptTreeByRoleVO {
    private List<Long> checkedKeys;
    private List<TreeSelectVO> depts;
}
```

- [ ] **Step 4: `mapper/SysRoleMapper.java` + `mapper/SysRoleMenuMapper.java`**

```java
package com.yiling.modules.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.role.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
```

```java
package com.yiling.modules.role.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface SysRoleMenuMapper {
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("<script>INSERT INTO sys_role_menu (role_id, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>(#{roleId}, #{menuId})</foreach></script>")
    void batchInsert(@Param("roleId") Long roleId, @Param("menuIds") java.util.List<Long> menuIds);
}
```

- [ ] **Step 5: `service/SysRoleService.java` + `impl/SysRoleServiceImpl.java`**

```java
package com.yiling.modules.role.service;

import com.yiling.common.result.PageResult;
import com.yiling.modules.role.dto.DataScopeDTO;
import com.yiling.modules.role.dto.SysRoleDTO;
import com.yiling.modules.role.entity.SysRole;

public interface SysRoleService {
    PageResult<SysRole> listPage(SysRoleDTO query);
    SysRole detailById(Long id);
    void save(SysRoleDTO dto);
    void update(SysRoleDTO dto);
    void delete(Long id);
    void updateSimple(Long id, Integer status);
    void saveDataScope(DataScopeDTO dto);
}
```

```java
package com.yiling.modules.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.role.dto.DataScopeDTO;
import com.yiling.modules.role.dto.SysRoleDTO;
import com.yiling.modules.role.entity.SysRole;
import com.yiling.modules.role.mapper.SysRoleMapper;
import com.yiling.modules.role.mapper.SysRoleMenuMapper;
import com.yiling.modules.role.service.SysRoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public SysRoleServiceImpl(SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public PageResult<SysRole> listPage(SysRoleDTO query) {
        LambdaQueryWrapper<SysRole> qw = new LambdaQueryWrapper<>();
        qw.ne(SysRole::getStatus, 0);
        qw.like(query.getRoleName() != null && !query.getRoleName().isBlank(), SysRole::getRoleName, query.getRoleName());
        qw.like(query.getRoleKey() != null && !query.getRoleKey().isBlank(), SysRole::getRoleKey, query.getRoleKey());
        qw.eq(query.getStatus() != null, SysRole::getStatus, query.getStatus());
        qw.orderByAsc(SysRole::getRoleSort);
        Page<SysRole> page = new Page<>(query.getPageNo(), query.getPageSize());
        return PageResult.of(roleMapper.selectPage(page, qw));
    }

    @Override
    public SysRole detailById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        return role;
    }

    @Override
    @Transactional
    public void save(SysRoleDTO dto) {
        SysRole entity = new SysRole();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        entity.setDataScope("1");
        entity.setMenuCheckStrictly(true);
        entity.setDeptCheckStrictly(true);
        roleMapper.insert(entity);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            roleMenuMapper.batchInsert(entity.getId(), dto.getMenuIds());
        }
    }

    @Override
    @Transactional
    public void update(SysRoleDTO dto) {
        if ("admin".equals(roleMapper.selectById(dto.getId()).getRoleKey())) {
            throw new BusinessException("超级管理员角色不允许修改菜单权限之外的关键属性");
        }
        SysRole entity = new SysRole();
        BeanUtils.copyProperties(dto, entity);
        roleMapper.updateById(entity);
        roleMenuMapper.deleteByRoleId(dto.getId());
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            roleMenuMapper.batchInsert(dto.getId(), dto.getMenuIds());
        }
    }

    @Override
    public void delete(Long id) {
        SysRole role = detailById(id);
        if ("admin".equals(role.getRoleKey())) {
            throw new BusinessException("超级管理员角色不允许删除");
        }
        role.setStatus(0);
        roleMapper.updateById(role);
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysRole entity = new SysRole();
        entity.setId(id);
        entity.setStatus(status);
        roleMapper.updateById(entity);
    }

    @Override
    public void saveDataScope(DataScopeDTO dto) {
        SysRole entity = new SysRole();
        entity.setId(dto.getId());
        entity.setDataScope(dto.getDataScope());
        if ("2".equals(dto.getDataScope()) && dto.getDeptIdList() != null) {
            entity.setDeptIds(String.join(",", dto.getDeptIdList().stream().map(String::valueOf).toList()));
        } else {
            entity.setDeptIds(null);
        }
        roleMapper.updateById(entity);
    }
}
```

- [ ] **Step 6: `controller/SysRoleController.java`** (includes the `dataPerm` sub-paths — kept in the same controller since they operate on `SysRole`, just under a different `RequestMapping` prefix per the spec's literal path names)

```java
package com.yiling.modules.role.controller;

import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import com.yiling.modules.role.dto.DataScopeDTO;
import com.yiling.modules.role.dto.SysRoleDTO;
import com.yiling.modules.role.entity.SysRole;
import com.yiling.modules.role.service.SysRoleService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SysRoleController {

    private final SysRoleService service;

    public SysRoleController(SysRoleService service) {
        this.service = service;
    }

    @PostMapping("rest/sysRole/listPage")
    public Result<PageResult<SysRole>> listPage(@RequestBody SysRoleDTO query) {
        return Result.success(service.listPage(query));
    }

    @PostMapping("rest/sysRole/detailById")
    public Result<SysRole> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("rest/sysRole/save")
    public Result<Void> save(@RequestBody SysRoleDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("rest/sysRole/update")
    public Result<Void> update(@RequestBody SysRoleDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("rest/sysRole/delete")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        service.delete(Long.valueOf(body.get("id").toString()));
        return Result.success();
    }

    @PostMapping("rest/sysRole/updateSimple")
    public Result<Void> updateSimple(@RequestBody Map<String, Object> body) {
        service.updateSimple(Long.valueOf(body.get("id").toString()), Integer.valueOf(body.get("status").toString()));
        return Result.success();
    }

    @PostMapping("rest/dataPerm/dataScope")
    public Result<Void> dataScope(@RequestBody DataScopeDTO dto) {
        service.saveDataScope(dto);
        return Result.success();
    }
}
```

`rest/dataPerm/deptTreeByRole` does NOT share `SysDeptController`'s `rest/sysDept` class-level prefix (Spring MVC concatenates class-level and method-level `@RequestMapping` paths — a method-level leading slash does not escape/override the prefix), so it gets its own small controller instead: create `backend/.../modules/role/controller/DataPermController.java`:

```java
package com.yiling.modules.role.controller;

import com.yiling.common.result.Result;
import com.yiling.modules.role.dto.DataScopeDTO;
import com.yiling.modules.role.service.SysRoleService;
import com.yiling.modules.role.vo.DeptTreeByRoleVO;
import com.yiling.modules.dept.service.SysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/dataPerm")
public class DataPermController {

    @Autowired
    private SysRoleService roleService;
    @Autowired
    private SysDeptService deptService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("dataScope")
    public Result<Void> dataScope(@RequestBody DataScopeDTO dto) {
        roleService.saveDataScope(dto);
        return Result.success();
    }

    @PostMapping("deptTreeByRole")
    public Result<DeptTreeByRoleVO> deptTreeByRole(@RequestBody Map<String, Object> body) {
        Long roleId = Long.valueOf(body.get("roleId").toString());
        List<String> rows = jdbcTemplate.queryForList("SELECT dept_ids FROM sys_role WHERE id = ?", String.class, roleId);
        String deptIds = rows.isEmpty() ? null : rows.get(0);
        List<Long> checked = (deptIds == null || deptIds.isBlank())
                ? List.of()
                : java.util.Arrays.stream(deptIds.split(",")).map(Long::valueOf).toList();
        return Result.success(new DeptTreeByRoleVO(checked, deptService.deptTree()));
    }
}
```

This supersedes the `rest/dataPerm/dataScope` mapping inside `SysRoleController` above — **remove** the `dataScope` method from `SysRoleController` (shown earlier in this task) since `DataPermController` now owns both `rest/dataPerm/*` endpoints in one place.

- [ ] **Step 7: `controller/SysUserRoleController.java`** — user-assignment sub-pages. This needs read access to `sys_user`; since the full `SysUser` entity doesn't exist until Task 11, use `JdbcTemplate` here too (a later cleanup task is NOT needed — this is a stable, self-contained implementation)

```java
package com.yiling.modules.role.controller;

import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/sysUserRole")
public class SysUserRoleController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("allocatedList")
    public Result<PageResult<Map<String, Object>>> allocatedList(@RequestBody Map<String, Object> q) {
        return Result.success(queryUsers(q, true));
    }

    @PostMapping("unallocatedList")
    public Result<PageResult<Map<String, Object>>> unallocatedList(@RequestBody Map<String, Object> q) {
        return Result.success(queryUsers(q, false));
    }

    private PageResult<Map<String, Object>> queryUsers(Map<String, Object> q, boolean allocated) {
        Long roleId = Long.valueOf(q.get("roleId").toString());
        int pageNo = q.get("pageNo") != null ? Integer.parseInt(q.get("pageNo").toString()) : 1;
        int pageSize = q.get("pageSize") != null ? Integer.parseInt(q.get("pageSize").toString()) : 10;
        String usernameFilter = (String) q.getOrDefault("username", null);

        String joinClause = allocated ? "JOIN" : "LEFT JOIN";
        String whereClause = allocated
                ? "ur.role_id = ?"
                : "(ur.role_id IS NULL OR ur.role_id != ?)";

        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT u.id, u.username, u.nick_name, u.phone, u.status FROM sys_user u " +
                        joinClause + " sys_user_role ur ON ur.user_id = u.id AND ur.role_id = ? " +
                        "WHERE u.status != 0 AND " + (allocated ? "ur.role_id = ?" : "ur.role_id IS NULL"));
        List<Object> params = new java.util.ArrayList<>(List.of(roleId, roleId));
        if (usernameFilter != null && !usernameFilter.isBlank()) {
            sql.append(" AND u.username LIKE ?");
            params.add("%" + usernameFilter + "%");
        }
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add((pageNo - 1) * pageSize);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT u.id) FROM sys_user u " + joinClause +
                        " sys_user_role ur ON ur.user_id = u.id AND ur.role_id = ? WHERE u.status != 0 AND " +
                        (allocated ? "ur.role_id = ?" : "ur.role_id IS NULL"),
                Long.class, roleId, roleId);

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total != null ? total : 0);
        result.setSize(pageSize);
        result.setCurrent(pageNo);
        result.setPages((long) Math.ceil((double) (total != null ? total : 0) / pageSize));
        return result;
    }

    @PostMapping("cancel")
    public Result<Void> cancel(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ? AND role_id = ?",
                Long.valueOf(body.get("userId").toString()), Long.valueOf(body.get("roleId").toString()));
        return Result.success();
    }

    @PostMapping("cancelAll")
    @SuppressWarnings("unchecked")
    public Result<Void> cancelAll(@RequestBody Map<String, Object> body) {
        Long roleId = Long.valueOf(body.get("roleId").toString());
        for (Object userId : (List<Object>) body.get("userIds")) {
            jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ? AND role_id = ?",
                    Long.valueOf(userId.toString()), roleId);
        }
        return Result.success();
    }

    @PostMapping("selectUserAll")
    @SuppressWarnings("unchecked")
    public Result<Void> selectUserAll(@RequestBody Map<String, Object> body) {
        Long roleId = Long.valueOf(body.get("roleId").toString());
        for (Object userId : (List<Object>) body.get("userIds")) {
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?, ?)",
                    Long.valueOf(userId.toString()), roleId);
        }
        return Result.success();
    }
}
```

- [ ] **Step 8: integration test**

```java
package com.yiling.modules.role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SysRoleControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    void delete_adminRoleIsProtected() throws Exception {
        mockMvc.perform(post("/rest/sysRole/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("超级管理员角色不允许删除"));
    }

    @Test
    @WithMockUser
    void allocatedList_returnsSeededAdminUser() throws Exception {
        mockMvc.perform(post("/rest/sysUserRole/allocatedList").contentType(MediaType.APPLICATION_JSON).content("{\"roleId\":1,\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }
}
```

- [ ] **Step 9: run tests** — `mvn -q test -Dtest=SysRoleControllerIT` — expect PASS.

- [ ] **Step 10: `frontend/src/api/system/role.js`**

```js
import request from '../../utils/request'

export function listRolePage(query) { return request.post('/rest/sysRole/listPage', query) }
export function getRole(id) { return request.post('/rest/sysRole/detailById', { id }) }
export function addRole(data) { return request.post('/rest/sysRole/save', data) }
export function updateRole(data) { return request.post('/rest/sysRole/update', data) }
export function delRole(id) { return request.post('/rest/sysRole/delete', { id }) }
export function updateRoleStatus(id, status) { return request.post('/rest/sysRole/updateSimple', { id, status }) }
export function roleMenuTreeSelect(roleId) { return request.post('/rest/sysMenu/roleMenuTreeSelect', { roleId }) }
export function deptTreeByRole(roleId) { return request.post('/rest/dataPerm/deptTreeByRole', { roleId }) }
export function saveDataScope(data) { return request.post('/rest/dataPerm/dataScope', data) }
export function allocatedUserList(query) { return request.post('/rest/sysUserRole/allocatedList', query) }
export function unallocatedUserList(query) { return request.post('/rest/sysUserRole/unallocatedList', query) }
export function cancelUserRole(userId, roleId) { return request.post('/rest/sysUserRole/cancel', { userId, roleId }) }
export function cancelUserRoleAll(roleId, userIds) { return request.post('/rest/sysUserRole/cancelAll', { roleId, userIds }) }
export function selectUserRoleAll(roleId, userIds) { return request.post('/rest/sysUserRole/selectUserAll', { roleId, userIds }) }
```

- [ ] **Step 11: `frontend/src/views/system/role/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="角色名称"><el-input v-model="query.roleName" clearable /></el-form-item>
      <el-form-item label="权限字符"><el-input v-model="query.roleKey" clearable /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width:120px"><el-option label="正常" :value="1" /><el-option label="停用" :value="2" /></el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:role:add']" @click="openDialog()">新增</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData">
      <el-table-column prop="id" label="角色编号" width="90" />
      <el-table-column prop="roleName" label="角色名称" />
      <el-table-column prop="roleKey" label="权限字符" />
      <el-table-column prop="roleSort" label="显示顺序" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" :disabled="row.roleKey === 'admin'" @change="(v) => toggleStatus(row, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:role:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="primary" v-hasPermi="['system:role:edit']" @click="openDataScope(row)">数据权限</el-button>
          <el-button link type="primary" @click="goAuthUser(row)">分配用户</el-button>
          <el-button link type="danger" v-hasPermi="['system:role:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改角色' : '新增角色'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="权限字符" prop="roleKey"><el-input v-model="form.roleKey" /></el-form-item>
        <el-form-item label="显示顺序" prop="roleSort"><el-input-number v-model="form.roleSort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="菜单权限">
          <el-tree ref="menuTreeRef" :data="menuTree" show-checkbox node-key="id" :props="{ label: 'label', children: 'children' }" style="max-height:260px; overflow-y:auto; border:1px solid #dcdfe6; width:100%; padding:8px;" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="dataScopeVisible" title="分配数据权限" width="500px">
      <el-form label-width="90px">
        <el-form-item label="数据范围">
          <el-radio-group v-model="dataScopeForm.dataScope">
            <el-radio value="1">全部数据权限</el-radio><el-radio value="2">自定义数据权限</el-radio>
            <el-radio value="3">本部门数据权限</el-radio><el-radio value="4">本部门及以下数据权限</el-radio><el-radio value="5">仅本人数据权限</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数据权限" v-if="dataScopeForm.dataScope === '2'">
          <el-tree ref="deptTreeRef" :data="deptTree" show-checkbox node-key="id" :props="{ label: 'label', children: 'children' }" style="max-height:260px; overflow-y:auto; border:1px solid #dcdfe6; width:100%; padding:8px;" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dataScopeVisible = false">取消</el-button><el-button type="primary" @click="submitDataScope">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRolePage, addRole, updateRole, delRole, updateRoleStatus, roleMenuTreeSelect, deptTreeByRole, saveDataScope } from '../../../api/system/role'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const router = useRouter()
const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ roleName: '', roleKey: '', status: null, pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const menuTreeRef = ref()
const menuTree = ref([])
const form = reactive({ id: null, roleName: '', roleKey: '', roleSort: 0, status: 1, remark: '' })
const rules = { roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }], roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }] }

const dataScopeVisible = ref(false)
const deptTreeRef = ref()
const deptTree = ref([])
const dataScopeForm = reactive({ id: null, dataScope: '1' })

async function fetchList() {
  loading.value = true
  try {
    const data = await listRolePage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.roleName = ''; query.roleKey = ''; query.status = null; query.pageNo = 1; fetchList() }

async function openDialog(row) {
  Object.assign(form, { id: null, roleName: '', roleKey: '', roleSort: 0, status: 1, remark: '' })
  if (row) Object.assign(form, row)
  const treeData = await roleMenuTreeSelect(row ? row.id : 0)
  menuTree.value = treeData.menus
  dialogVisible.value = true
  setTimeout(() => menuTreeRef.value?.setCheckedKeys(treeData.checkedKeys || []), 0)
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    const menuIds = [...(menuTreeRef.value?.getCheckedKeys() || []), ...(menuTreeRef.value?.getHalfCheckedKeys() || [])]
    const payload = { ...form, menuIds }
    if (form.id) await updateRole(payload)
    else await addRole(payload)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

async function openDataScope(row) {
  dataScopeForm.id = row.id
  dataScopeForm.dataScope = row.dataScope || '1'
  const data = await deptTreeByRole(row.id)
  deptTree.value = data.depts
  dataScopeVisible.value = true
  setTimeout(() => deptTreeRef.value?.setCheckedKeys(data.checkedKeys || []), 0)
}

async function submitDataScope() {
  const deptIdList = dataScopeForm.dataScope === '2' ? (deptTreeRef.value?.getCheckedKeys() || []) : []
  await saveDataScope({ id: dataScopeForm.id, dataScope: dataScopeForm.dataScope, deptIdList })
  ElMessage.success('保存成功')
  dataScopeVisible.value = false
  fetchList()
}

function toggleStatus(row, checked) {
  updateRoleStatus(row.id, checked ? 1 : 2).then(() => { row.status = checked ? 1 : 2; ElMessage.success('状态已更新') })
}

function goAuthUser(row) { router.push(`/system/role-auth-user/${row.id}`) }

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除角色"${row.roleName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delRole(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
```

- [ ] **Step 12: `frontend/src/views/system/role/authUser.vue`**

```vue
<template>
  <div class="app-container">
    <el-form inline>
      <el-form-item label="用户名称"><el-input v-model="query.username" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button></el-form-item>
    </el-form>
    <el-row class="mb8">
      <el-button type="primary" @click="showSelectDialog = true">添加用户</el-button>
      <el-button type="danger" :disabled="!selected.length" @click="batchCancel">批量取消授权</el-button>
    </el-row>
    <el-table :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="username" label="用户名称" />
      <el-table-column prop="nick_name" label="用户昵称" />
      <el-table-column prop="phone" label="手机号码" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }"><el-button link type="danger" @click="cancel(row)">取消授权</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <select-user v-if="showSelectDialog" :role-id="roleId" @close="showSelectDialog = false" @confirm="onSelectConfirm" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { allocatedUserList, cancelUserRole, cancelUserRoleAll } from '../../../api/system/role'
import Pagination from '../../../components/Pagination.vue'
import SelectUser from './selectUser.vue'

const route = useRoute()
const roleId = Number(route.params.roleId)
const query = reactive({ roleId, username: '', pageNo: 1, pageSize: 10 })
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const showSelectDialog = ref(false)

async function fetchList() {
  const data = await allocatedUserList(query)
  tableData.value = data.records
  total.value = data.total
}

function cancel(row) {
  ElMessageBox.confirm(`确定取消用户"${row.username}"的角色授权吗？`, '提示', { type: 'warning' }).then(async () => {
    await cancelUserRole(row.id, roleId)
    ElMessage.success('已取消授权')
    fetchList()
  })
}

function batchCancel() {
  ElMessageBox.confirm('确定批量取消选中用户的角色授权吗？', '提示', { type: 'warning' }).then(async () => {
    await cancelUserRoleAll(roleId, selected.value)
    ElMessage.success('已取消授权')
    fetchList()
  })
}

function onSelectConfirm() { showSelectDialog.value = false; fetchList() }

onMounted(fetchList)
</script>
```

- [ ] **Step 13: `frontend/src/views/system/role/selectUser.vue`**

```vue
<template>
  <el-dialog model-value title="选择用户" width="700px" @close="$emit('close')">
    <el-form inline><el-form-item label="用户名称"><el-input v-model="query.username" clearable /></el-form-item><el-form-item><el-button type="primary" @click="fetchList">搜索</el-button></el-form-item></el-form>
    <el-table :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="username" label="用户名称" />
      <el-table-column prop="nick_name" label="用户昵称" />
      <el-table-column prop="phone" label="手机号码" />
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />
    <template #footer><el-button @click="$emit('close')">取消</el-button><el-button type="primary" @click="confirm">确定</el-button></template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { unallocatedUserList, selectUserRoleAll } from '../../../api/system/role'
import Pagination from '../../../components/Pagination.vue'

const props = defineProps({ roleId: Number })
const emit = defineEmits(['close', 'confirm'])
const query = reactive({ roleId: props.roleId, username: '', pageNo: 1, pageSize: 10 })
const tableData = ref([])
const total = ref(0)
const selected = ref([])

async function fetchList() {
  const data = await unallocatedUserList(query)
  tableData.value = data.records
  total.value = data.total
}

async function confirm() {
  if (!selected.value.length) { ElMessage.warning('请至少选择一个用户'); return }
  await selectUserRoleAll(props.roleId, selected.value)
  ElMessage.success('添加成功')
  emit('confirm')
}

onMounted(fetchList)
</script>
```

- [ ] **Step 14: register the `authUser` route** — add to `frontend/src/router/index.js` `constantRoutes`' `Layout` children (alongside `dashboard`):

```js
{ path: 'system/role-auth-user/:roleId', name: 'RoleAuthUser', component: () => import('../views/system/role/authUser.vue'), meta: { title: '分配用户' } }
```

- [ ] **Step 15: Commit**

```bash
git add backend/src frontend/src
git commit -m "feat: role module (menu-tree assignment, data-scope, user assignment sub-pages)"
```

---

### Task 11: User Module (backend + frontend, incl. authRole + profile pages)

**Files:**
- Create: `backend/.../modules/user/entity/SysUser.java`
- Create: `backend/.../modules/user/dto/{SysUserDTO,ResetPwdDTO,UpdateSelfDTO,UpdatePwdDTO}.java`
- Create: `backend/.../modules/user/vo/{SysUserVO,SysUserDetailVO}.java`
- Create: `backend/.../modules/user/mapper/{SysUserMapper,SysUserRoleMapper,SysUserPostMapper}.java`
- Create: `backend/.../modules/user/service/SysUserService.java` + `impl/SysUserServiceImpl.java`
- Create: `backend/.../modules/user/controller/SysUserController.java`
- Test: `backend/src/test/java/com/yiling/modules/user/SysUserControllerIT.java`
- Create: `frontend/src/api/system/user.js`
- Create: `frontend/src/views/system/user/{index.vue,authRole.vue}`
- Create: `frontend/src/views/system/user/profile/{index.vue,userInfo.vue,resetPwd.vue,userAvatar.vue}`

**Interfaces:**
- Consumes: `DeptTreeSelect` (Task 6), `sys_role`/`sys_post` tables for the multi-select pickers in the save/edit dialog.
- Produces: the endpoint table below is copied **verbatim** from `docs/系统管理功能提示词.md` §模块1 — do not rename any path.

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | rest/sysUser/listPage | 分页列表 |
| POST | rest/sysUser/list | 不分页列表 |
| POST | rest/sysUser/detailById | 详情（含 roles/posts） |
| POST | rest/sysUser/save | 新增 |
| POST | rest/sysUser/update | 修改 |
| POST | rest/sysUser/updateSimple | 局部更新（状态切换） |
| POST | rest/sysUser/delete | 删除 |
| POST | rest/sysUser/resetPwd | 重置密码 |
| POST | rest/sysUser/profile | 当前登录人信息 |
| POST | rest/sysUser/updateSelfSimple | 个人中心改资料 |
| POST | rest/sysUser/updatePwd | 修改密码 |
| POST | rest/sysUser/avatar | 头像上传 |

- [ ] **Step 1: `entity/SysUser.java`**

```java
package com.yiling.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deptId;
    private String username;
    private String nickName;
    private String passwd;
    private String sex;
    private String email;
    private String phone;
    private String avatar;
    private String remark;
}
```

- [ ] **Step 2: DTOs**

```java
package com.yiling.modules.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class SysUserDTO {
    private Long id;
    private Long deptId;
    private String username;
    private String nickName;
    private String passwd;
    private String sex;
    private String email;
    private String phone;
    private Integer status;
    private String remark;
    private List<Long> roleIds;
    private List<Long> postIds;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

```java
package com.yiling.modules.user.dto;

import lombok.Data;

@Data
public class ResetPwdDTO {
    private Long id;
    private String passwd;
}
```

```java
package com.yiling.modules.user.dto;

import lombok.Data;

@Data
public class UpdateSelfDTO {
    private String nickName;
    private String email;
    private String phone;
    private String sex;
}
```

```java
package com.yiling.modules.user.dto;

import lombok.Data;

@Data
public class UpdatePwdDTO {
    private String oldPassword;
    private String newPassword;
}
```

- [ ] **Step 3: VOs**

```java
package com.yiling.modules.user.vo;

import com.yiling.modules.user.entity.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserVO extends SysUser {
    private String deptName;
}
```

```java
package com.yiling.modules.user.vo;

import com.yiling.modules.user.entity.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserDetailVO extends SysUser {
    private List<Long> roleIds;
    private List<Long> postIds;
}
```

- [ ] **Step 4: Mappers**

```java
package com.yiling.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.user.entity.SysUser;
import com.yiling.modules.user.vo.SysUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("<script>SELECT u.*, d.dept_name deptName FROM sys_user u LEFT JOIN sys_dept d ON d.id = u.dept_id " +
            "WHERE u.status != 0 " +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if> " +
            "<if test='username != null and username != \"\"'>AND u.username LIKE CONCAT('%',#{username},'%')</if> " +
            "<if test='phone != null and phone != \"\"'>AND u.phone LIKE CONCAT('%',#{phone},'%')</if> " +
            "<if test='status != null'>AND u.status = #{status}</if> " +
            "ORDER BY u.create_time DESC</script>")
    List<SysUserVO> selectUserVOList(@Param("deptId") Long deptId, @Param("username") String username,
                                      @Param("phone") String phone, @Param("status") Integer status);
}
```

```java
package com.yiling.modules.user.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUser(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Insert("<script>INSERT INTO sys_user_role (user_id, role_id) VALUES " +
            "<foreach collection='roleIds' item='roleId' separator=','>(#{userId}, #{roleId})</foreach></script>")
    void batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
```

```java
package com.yiling.modules.user.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserPostMapper {
    @Select("SELECT post_id FROM sys_user_post WHERE user_id = #{userId}")
    List<Long> selectPostIdsByUser(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_post WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Insert("<script>INSERT INTO sys_user_post (user_id, post_id) VALUES " +
            "<foreach collection='postIds' item='postId' separator=','>(#{userId}, #{postId})</foreach></script>")
    void batchInsert(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
```

- [ ] **Step 5: `service/SysUserService.java`**

```java
package com.yiling.modules.user.service;

import com.yiling.common.result.PageResult;
import com.yiling.modules.user.dto.*;
import com.yiling.modules.user.entity.SysUser;
import com.yiling.modules.user.vo.SysUserDetailVO;
import com.yiling.modules.user.vo.SysUserVO;

import java.util.List;

public interface SysUserService {
    PageResult<SysUserVO> listPage(SysUserDTO query);
    List<SysUserVO> list(SysUserDTO query);
    SysUserDetailVO detailById(Long id);
    void save(SysUserDTO dto);
    void update(SysUserDTO dto);
    void delete(Long id);
    void updateSimple(Long id, Integer status);
    void resetPwd(ResetPwdDTO dto);
    SysUserDetailVO profile(String username);
    void updateSelfSimple(String username, UpdateSelfDTO dto);
    void updatePwd(String username, UpdatePwdDTO dto);
    String updateAvatar(String username, String avatarUrl);
}
```

- [ ] **Step 6: `service/impl/SysUserServiceImpl.java`**

```java
package com.yiling.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.user.dto.*;
import com.yiling.modules.user.entity.SysUser;
import com.yiling.modules.user.mapper.SysUserMapper;
import com.yiling.modules.user.mapper.SysUserPostMapper;
import com.yiling.modules.user.mapper.SysUserRoleMapper;
import com.yiling.modules.user.service.SysUserService;
import com.yiling.modules.user.vo.SysUserDetailVO;
import com.yiling.modules.user.vo.SysUserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper,
                               SysUserPostMapper userPostMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.userPostMapper = userPostMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<SysUserVO> listPage(SysUserDTO query) {
        List<SysUserVO> all = userMapper.selectUserVOList(query.getDeptId(), query.getUsername(), query.getPhone(), query.getStatus());
        int pageNo = query.getPageNo() == null ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        int from = Math.min((pageNo - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        PageResult<SysUserVO> result = new PageResult<>();
        result.setRecords(all.subList(from, to));
        result.setTotal(all.size());
        result.setSize(pageSize);
        result.setCurrent(pageNo);
        result.setPages((long) Math.ceil((double) all.size() / pageSize));
        return result;
    }

    @Override
    public List<SysUserVO> list(SysUserDTO query) {
        return userMapper.selectUserVOList(query.getDeptId(), query.getUsername(), query.getPhone(), query.getStatus());
    }

    @Override
    public SysUserDetailVO detailById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        SysUserDetailVO vo = new SysUserDetailVO();
        BeanUtils.copyProperties(user, vo);
        vo.setPasswd(null);
        vo.setRoleIds(userRoleMapper.selectRoleIdsByUser(id));
        vo.setPostIds(userPostMapper.selectPostIdsByUser(id));
        return vo;
    }

    @Override
    @Transactional
    public void save(SysUserDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) throw new BusinessException("用户名称不能为空");
        if (dto.getPasswd() == null || dto.getPasswd().isBlank()) throw new BusinessException("密码不能为空");
        Long existing = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (existing != null && existing > 0) throw new BusinessException("用户名称已存在");

        SysUser entity = new SysUser();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        entity.setPasswd(passwordEncoder.encode(dto.getPasswd()));
        userMapper.insert(entity);
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) userRoleMapper.batchInsert(entity.getId(), dto.getRoleIds());
        if (dto.getPostIds() != null && !dto.getPostIds().isEmpty()) userPostMapper.batchInsert(entity.getId(), dto.getPostIds());
    }

    @Override
    @Transactional
    public void update(SysUserDTO dto) {
        SysUser entity = new SysUser();
        BeanUtils.copyProperties(dto, entity);
        entity.setPasswd(null); // password unchanged via this endpoint
        userMapper.updateById(entity);
        userRoleMapper.deleteByUserId(dto.getId());
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) userRoleMapper.batchInsert(dto.getId(), dto.getRoleIds());
        userPostMapper.deleteByUserId(dto.getId());
        if (dto.getPostIds() != null && !dto.getPostIds().isEmpty()) userPostMapper.batchInsert(dto.getId(), dto.getPostIds());
    }

    @Override
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if ("admin".equals(user.getUsername())) throw new BusinessException("超级管理员账号不允许删除");
        user.setStatus(0);
        userMapper.updateById(user);
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysUser entity = new SysUser();
        entity.setId(id);
        entity.setStatus(status);
        userMapper.updateById(entity);
    }

    @Override
    public void resetPwd(ResetPwdDTO dto) {
        if (dto.getPasswd() == null || dto.getPasswd().length() < 5 || dto.getPasswd().length() > 20) {
            throw new BusinessException("密码长度必须在5-20位之间");
        }
        SysUser entity = new SysUser();
        entity.setId(dto.getId());
        entity.setPasswd(passwordEncoder.encode(dto.getPasswd()));
        userMapper.updateById(entity);
    }

    @Override
    public SysUserDetailVO profile(String username) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) throw new BusinessException("用户不存在");
        return detailById(user.getId());
    }

    @Override
    public void updateSelfSimple(String username, UpdateSelfDTO dto) {
        if (dto.getNickName() == null || dto.getNickName().isBlank()) throw new BusinessException("昵称不能为空");
        if (dto.getEmail() == null || dto.getEmail().isBlank()) throw new BusinessException("邮箱不能为空");
        if (dto.getPhone() == null || dto.getPhone().isBlank()) throw new BusinessException("手机号不能为空");
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        SysUser entity = new SysUser();
        entity.setId(user.getId());
        entity.setNickName(dto.getNickName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setSex(dto.getSex());
        userMapper.updateById(entity);
    }

    @Override
    public void updatePwd(String username, UpdatePwdDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswd())) throw new BusinessException("原密码不正确");
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 5 || dto.getNewPassword().length() > 20) {
            throw new BusinessException("密码长度必须在5-20位之间");
        }
        SysUser entity = new SysUser();
        entity.setId(user.getId());
        entity.setPasswd(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(entity);
    }

    @Override
    public String updateAvatar(String username, String avatarUrl) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        SysUser entity = new SysUser();
        entity.setId(user.getId());
        entity.setAvatar(avatarUrl);
        userMapper.updateById(entity);
        return avatarUrl;
    }
}
```

- [ ] **Step 7: `controller/SysUserController.java`** — avatar upload writes under `backend/uploads/avatar/` and is served statically; add a `WebMvcConfigurer` resource-handler inline in this same file for simplicity

```java
package com.yiling.modules.user.controller;

import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import com.yiling.modules.user.dto.*;
import com.yiling.modules.user.entity.SysUser;
import com.yiling.modules.user.service.SysUserService;
import com.yiling.modules.user.vo.SysUserDetailVO;
import com.yiling.modules.user.vo.SysUserVO;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("rest/sysUser")
public class SysUserController {

    private final SysUserService service;

    public SysUserController(SysUserService service) {
        this.service = service;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping("listPage")
    public Result<PageResult<SysUserVO>> listPage(@RequestBody SysUserDTO query) {
        return Result.success(service.listPage(query));
    }

    @PostMapping("list")
    public Result<List<SysUserVO>> list(@RequestBody SysUserDTO query) {
        return Result.success(service.list(query));
    }

    @PostMapping("detailById")
    public Result<SysUserDetailVO> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("save")
    public Result<Void> save(@RequestBody SysUserDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Void> update(@RequestBody SysUserDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("updateSimple")
    public Result<Void> updateSimple(@RequestBody Map<String, Object> body) {
        service.updateSimple(Long.valueOf(body.get("id").toString()), Integer.valueOf(body.get("status").toString()));
        return Result.success();
    }

    @PostMapping("delete")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        service.delete(Long.valueOf(body.get("id").toString()));
        return Result.success();
    }

    @PostMapping("resetPwd")
    public Result<Void> resetPwd(@RequestBody ResetPwdDTO dto) {
        service.resetPwd(dto);
        return Result.success();
    }

    @PostMapping("profile")
    public Result<SysUserDetailVO> profile() {
        return Result.success(service.profile(currentUsername()));
    }

    @PostMapping("updateSelfSimple")
    public Result<Void> updateSelfSimple(@RequestBody UpdateSelfDTO dto) {
        service.updateSelfSimple(currentUsername(), dto);
        return Result.success();
    }

    @PostMapping("updatePwd")
    public Result<Void> updatePwd(@RequestBody UpdatePwdDTO dto) {
        service.updatePwd(currentUsername(), dto);
        return Result.success();
    }

    @PostMapping("avatar")
    public Result<Map<String, String>> avatar(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new BusinessException("请选择头像文件");
        String ext = java.util.Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf('.'));
        String filename = UUID.randomUUID() + ext;
        Path dir = Path.of("uploads", "avatar");
        Files.createDirectories(dir);
        file.transferTo(new File(dir.toFile(), filename));
        String url = "/uploads/avatar/" + filename;
        service.updateAvatar(currentUsername(), url);
        return Result.success(Map.of("url", url));
    }
}
```

Add a static-resource mapping so `/uploads/**` is servable — create `backend/.../common/config/StaticResourceConfig.java`:

```java
package com.yiling.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:uploads/");
    }
}
```

Also add `/uploads/**` to `SecurityConfig`'s `permitAll()` matchers (Phase 0 Task 3) since avatars must be viewable without a fresh token check on every `<img>` tag: append `"/uploads/**"` to the `.requestMatchers(...)` permitAll list.

- [ ] **Step 8: integration test**

```java
package com.yiling.modules.user;

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
class SysUserControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void save_thenListPage_findsNewUser() throws Exception {
        Map<String, Object> create = Map.of("username", "zhangsan", "nickName", "张三", "passwd", "abc12345", "deptId", 100, "sex", "0");
        mockMvc.perform(post("/rest/sysUser/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysUser/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"zhangsan\",\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].deptName").value("总公司"));
    }

    @Test
    @WithMockUser
    void delete_adminIsProtected() throws Exception {
        mockMvc.perform(post("/rest/sysUser/delete").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("超级管理员账号不允许删除"));
    }
}
```

- [ ] **Step 9: run tests** — `mvn -q test -Dtest=SysUserControllerIT` — expect PASS.

- [ ] **Step 10: `frontend/src/api/system/user.js`**

```js
import request from '../../utils/request'

export function listUserPage(query) { return request.post('/rest/sysUser/listPage', query) }
export function getUser(id) { return request.post('/rest/sysUser/detailById', { id }) }
export function addUser(data) { return request.post('/rest/sysUser/save', data) }
export function updateUser(data) { return request.post('/rest/sysUser/update', data) }
export function delUser(id) { return request.post('/rest/sysUser/delete', { id }) }
export function updateUserStatus(id, status) { return request.post('/rest/sysUser/updateSimple', { id, status }) }
export function resetUserPwd(id, passwd) { return request.post('/rest/sysUser/resetPwd', { id, passwd }) }
export function getProfile() { return request.post('/rest/sysUser/profile', {}) }
export function updateSelfSimple(data) { return request.post('/rest/sysUser/updateSelfSimple', data) }
export function updateSelfPwd(data) { return request.post('/rest/sysUser/updatePwd', data) }
export function uploadAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/rest/sysUser/avatar', form, { headers: { 'Content-Type': 'multipart/form-data' } })
}
```

- [ ] **Step 11: `frontend/src/views/system/user/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-row :gutter="16">
      <el-col :span="4">
        <dept-tree-select v-model="query.deptId" @update:model-value="fetchList" />
      </el-col>
      <el-col :span="20">
        <el-form v-show="showSearch" :model="query" inline>
          <el-form-item label="用户名称"><el-input v-model="query.username" clearable /></el-form-item>
          <el-form-item label="手机号码"><el-input v-model="query.phone" clearable /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable style="width:120px"><el-option label="正常" :value="1" /><el-option label="停用" :value="2" /></el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
        </el-form>

        <el-row class="mb8">
          <el-button type="primary" v-hasPermi="['system:user:add']" @click="openDialog()">新增</el-button>
          <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:user:remove']" @click="handleBatchDelete">批量删除</el-button>
          <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
        </el-row>

        <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="id" label="编号" width="80" />
          <el-table-column prop="username" label="用户名称" />
          <el-table-column prop="nickName" label="用户昵称" />
          <el-table-column prop="deptName" label="部门" />
          <el-table-column prop="phone" label="手机号码" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-switch :model-value="row.status === 1" :disabled="row.username === 'admin'" @change="(v) => toggleStatus(row, v)" /></template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button link type="primary" v-hasPermi="['system:user:edit']" @click="openDialog(row)">修改</el-button>
              <el-button link type="danger" v-hasPermi="['system:user:remove']" @click="handleDelete(row)">删除</el-button>
              <el-dropdown>
                <el-button link type="primary">更多<el-icon><arrow-down /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="openResetPwd(row)">重置密码</el-dropdown-item>
                    <el-dropdown-item @click="goAuthRole(row)">分配角色</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />
      </el-col>
    </el-row>

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改用户' : '新增用户'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户昵称" prop="nickName"><el-input v-model="form.nickName" /></el-form-item>
        <el-form-item label="归属部门" prop="deptId"><dept-tree-select v-model="form.deptId" /></el-form-item>
        <el-form-item label="手机号码"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="用户名称" prop="username"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="密码" prop="passwd" v-if="!form.id"><el-input v-model="form.passwd" type="password" show-password /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.sex" style="width:120px"><el-option label="男" value="0" /><el-option label="女" value="1" /><el-option label="未知" value="2" /></el-select>
        </el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="岗位">
          <el-select v-model="form.postIds" multiple style="width:100%"><el-option v-for="p in postOptions" :key="p.id" :label="p.postName" :value="p.id" /></el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width:100%"><el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" /></el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { listUserPage, addUser, updateUser, delUser, updateUserStatus, resetUserPwd } from '../../../api/system/user'
import { listPost } from '../../../api/system/post'
import request from '../../../utils/request'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'
import DeptTreeSelect from '../../../components/DeptTreeSelect.vue'

const router = useRouter()
const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const postOptions = ref([])
const roleOptions = ref([])
const query = reactive({ deptId: null, username: '', phone: '', status: null, pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, deptId: null, username: '', nickName: '', passwd: '', sex: '2', email: '', phone: '', status: 1, remark: '', roleIds: [], postIds: [] })
const rules = { username: [{ required: true, message: '请输入用户名称', trigger: 'blur' }], nickName: [{ required: true, message: '请输入用户昵称', trigger: 'blur' }], passwd: [{ required: true, message: '请输入密码', trigger: 'blur' }], deptId: [{ required: true, message: '请选择归属部门', trigger: 'change' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listUserPage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.username = ''; query.phone = ''; query.status = null; query.pageNo = 1; fetchList() }

async function openDialog(row) {
  Object.assign(form, { id: null, deptId: null, username: '', nickName: '', passwd: '', sex: '2', email: '', phone: '', status: 1, remark: '', roleIds: [], postIds: [] })
  if (row) {
    const detail = await request.post('/rest/sysUser/detailById', { id: row.id })
    Object.assign(form, detail, { roleIds: detail.roleIds, postIds: detail.postIds })
  }
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateUser(form)
    else await addUser(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function toggleStatus(row, checked) {
  updateUserStatus(row.id, checked ? 1 : 2).then(() => { row.status = checked ? 1 : 2; ElMessage.success('状态已更新') })
}

function openResetPwd(row) {
  ElMessageBox.prompt('请输入新密码（5-20位）', '重置密码', {
    inputPattern: /^.{5,20}$/,
    inputErrorMessage: '密码长度必须在5-20位之间'
  }).then(async ({ value }) => {
    await resetUserPwd(row.id, value)
    ElMessage.success('密码重置成功')
  })
}

function goAuthRole(row) { router.push(`/system/user-auth-role/${row.id}`) }

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除用户"${row.username}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delUser(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    for (const id of selected.value) await delUser(id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(async () => {
  fetchList()
  postOptions.value = await listPost()
  roleOptions.value = (await request.post('/rest/sysRole/listPage', { pageNo: 1, pageSize: 100 })).records
})
</script>
```

- [ ] **Step 12: `frontend/src/views/system/user/authRole.vue`**

```vue
<template>
  <div class="app-container">
    <el-card v-if="user">
      <p>用户：{{ user.username }}（{{ user.nickName }}）</p>
    </el-card>
    <el-checkbox-group v-model="checkedRoleIds" style="margin-top:16px;">
      <el-checkbox v-for="r in allRoles" :key="r.id" :value="r.id" :label="r.roleName" />
    </el-checkbox-group>
    <div style="margin-top:16px;"><el-button type="primary" @click="submit">保存</el-button></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../../utils/request'

const route = useRoute()
const userId = Number(route.params.userId)
const user = ref(null)
const allRoles = ref([])
const checkedRoleIds = ref([])

onMounted(async () => {
  const detail = await request.post('/rest/sysUser/detailById', { id: userId })
  user.value = detail
  checkedRoleIds.value = detail.roleIds || []
  allRoles.value = (await request.post('/rest/sysRole/listPage', { pageNo: 1, pageSize: 100 })).records
})

async function submit() {
  const dto = { id: userId, username: user.value.username, nickName: user.value.nickName, deptId: user.value.deptId, roleIds: checkedRoleIds.value, postIds: user.value.postIds }
  await request.post('/rest/sysUser/update', dto)
  ElMessage.success('保存成功')
}
</script>
```

- [ ] **Step 13: Personal Center pages**

`frontend/src/views/system/user/profile/index.vue`:

```vue
<template>
  <el-row :gutter="16">
    <el-col :span="8">
      <el-card>
        <user-avatar />
        <p>用户名：{{ profile?.username }}</p>
        <p>手机号：{{ profile?.phone }}</p>
        <p>邮箱：{{ profile?.email }}</p>
        <p>创建日期：{{ profile?.createTime }}</p>
      </el-card>
    </el-col>
    <el-col :span="16">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本资料" name="info"><user-info :profile="profile" @updated="fetchProfile" /></el-tab-pane>
        <el-tab-pane label="修改密码" name="pwd"><reset-pwd /></el-tab-pane>
      </el-tabs>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getProfile } from '../../../../api/system/user'
import UserAvatar from './userAvatar.vue'
import UserInfo from './userInfo.vue'
import ResetPwd from './resetPwd.vue'

const activeTab = ref('info')
const profile = ref(null)

async function fetchProfile() { profile.value = await getProfile() }
onMounted(fetchProfile)
</script>
```

`frontend/src/views/system/user/profile/userInfo.vue`:

```vue
<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
    <el-form-item label="昵称" prop="nickName"><el-input v-model="form.nickName" /></el-form-item>
    <el-form-item label="手机号" prop="phone"><el-input v-model="form.phone" /></el-form-item>
    <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" /></el-form-item>
    <el-form-item label="性别"><el-select v-model="form.sex" style="width:120px"><el-option label="男" value="0" /><el-option label="女" value="1" /></el-select></el-form-item>
    <el-form-item><el-button type="primary" @click="submit">保存</el-button></el-form-item>
  </el-form>
</template>

<script setup>
import { reactive, watch, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateSelfSimple } from '../../../../api/system/user'

const props = defineProps({ profile: Object })
const emit = defineEmits(['updated'])
const formRef = ref()
const form = reactive({ nickName: '', phone: '', email: '', sex: '2' })
const rules = { nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }], phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }], email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }] }

watch(() => props.profile, (v) => { if (v) Object.assign(form, v) }, { immediate: true })

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    await updateSelfSimple(form)
    ElMessage.success('保存成功')
    emit('updated')
  })
}
</script>
```

`frontend/src/views/system/user/profile/resetPwd.vue`:

```vue
<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
    <el-form-item label="旧密码" prop="oldPassword"><el-input v-model="form.oldPassword" type="password" show-password /></el-form-item>
    <el-form-item label="新密码" prop="newPassword"><el-input v-model="form.newPassword" type="password" show-password /></el-form-item>
    <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="form.confirmPassword" type="password" show-password /></el-form-item>
    <el-form-item><el-button type="primary" @click="submit">保存</el-button></el-form-item>
  </el-form>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateSelfPwd } from '../../../../api/system/user'

const formRef = ref()
const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 5, max: 20, message: '密码长度必须在5-20位之间', trigger: 'blur' }],
  confirmPassword: [{ required: true, validator: (r, v, cb) => (v !== form.newPassword ? cb(new Error('两次输入的密码不一致')) : cb()), trigger: 'blur' }]
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    await updateSelfPwd({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功')
    form.oldPassword = ''; form.newPassword = ''; form.confirmPassword = ''
  })
}
</script>
```

`frontend/src/views/system/user/profile/userAvatar.vue` (uses `cropperjs` — add `"cropperjs": "^1.6.2"` to `frontend/package.json` dependencies as part of this step):

```vue
<template>
  <div>
    <img :src="userStore.avatar || defaultAvatar" style="width:100px;height:100px;border-radius:50%;object-fit:cover;cursor:pointer;" @click="fileInput.click()" />
    <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
    <el-dialog v-model="cropVisible" title="裁剪头像" width="500px">
      <img ref="cropImg" :src="rawImage" style="max-width:100%;display:block;" />
      <template #footer><el-button @click="cropVisible = false">取消</el-button><el-button type="primary" @click="confirmCrop">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'
import { ElMessage } from 'element-plus'
import { uploadAvatar } from '../../../../api/system/user'
import { useUserStore } from '../../../../stores/user'

const userStore = useUserStore()
const fileInput = ref()
const cropImg = ref()
const cropVisible = ref(false)
const rawImage = ref('')
const defaultAvatar = '/uploads/avatar/default.png'
let cropper = null

function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  rawImage.value = URL.createObjectURL(file)
  cropVisible.value = true
  nextTick(() => {
    if (cropper) cropper.destroy()
    cropper = new Cropper(cropImg.value, { aspectRatio: 1, viewMode: 1 })
  })
}

function confirmCrop() {
  cropper.getCroppedCanvas().toBlob(async (blob) => {
    const file = new File([blob], 'avatar.png', { type: 'image/png' })
    const data = await uploadAvatar(file)
    userStore.avatar = data.url
    ElMessage.success('头像更新成功')
    cropVisible.value = false
  })
}
</script>
```

- [ ] **Step 14: register routes** — add to `frontend/src/router/index.js` `Layout` children:

```js
{ path: 'system/user-auth-role/:userId', name: 'UserAuthRole', component: () => import('../views/system/user/authRole.vue'), meta: { title: '分配角色' } },
{ path: 'user/profile', name: 'Profile', component: () => import('../views/system/user/profile/index.vue'), meta: { title: '个人中心' } }
```

- [ ] **Step 15: `npm install` the two new deps and Commit**

```bash
cd frontend && npm install cropperjs
cd ..
git add backend/src frontend/src frontend/package.json frontend/package-lock.json
git commit -m "feat: user module (CRUD, resetPwd, authRole, profile pages incl. avatar cropper)"
```

---

## Phase 1 Definition of Done

- All 5 module test classes (`SysDeptControllerIT`, `SysPostControllerIT`, `SysMenuControllerIT`, `SysRoleControllerIT`, `SysUserControllerIT`) pass with `mvn test`.
- In the browser: `/system/dept`, `/system/post`, `/system/menu`, `/system/role`, `/system/user` all load, support query→create→edit→delete, and the User dialog's dept/role/post pickers work end-to-end. Role's menu-tree checkbox assignment persists and is reflected next login's sidebar for a non-admin test role.

Next: `docs/superpowers/plans/2026-08-27-phase2-simple-modules.md` (Dict, Config, Notice).
