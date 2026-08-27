# Phase 2: Dict / Config / Notice Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Task numbering continues from Phase 1 (ended at Task 11).

**Goal:** Build the three standalone CRUD modules — Dict (type+data), Config, Notice — plus the `useDict`/`<dict-tag>` frontend infrastructure every other module's status/sex/yes-no columns render through.

**Architecture:** Identical shape to Phase 1 Task 8 (Post) — this is the reference pattern, copy it directly: `IService`/`ServiceImpl` backend, search-form→toolbar→table+pagination→dialog frontend page.

**Tech Stack:** same as prior phases; adds `@wangeditor/editor`/`editor-for-vue` (already in `frontend/package.json` from Phase 0) for Notice's rich-text field.

**Spec:** `docs/superpowers/specs/2026-08-27-system-management-design.md`

## Global Constraints

(Same as Phase 0/1.) `status`: `0=deleted,1=normal,2=disabled` (soft-delete via `UPDATE status=0` for all three modules in this phase — none of them are trees). Responses `{code,message,data}`; pagination `{pageNo,pageSize}`→`{records,total,size,current,pages}`; `POST rest/{entity}/{action}`.

---

## File Structure (this phase's additions)

```
backend/src/main/java/com/yiling/modules/
├── dict/{entity/{SysDictType,SysDictData},dto/{SysDictTypeDTO,SysDictDataDTO},mapper/...,service/...,controller/{SysDictTypeController,SysDictDataController}}.java
├── config/{entity/SysConfig,dto/SysConfigDTO,mapper/SysConfigMapper,service/...,controller/SysConfigController}.java
└── notice/{entity/SysNotice,dto/SysNoticeDTO,mapper/SysNoticeMapper,service/...,controller/SysNoticeController}.java

frontend/src/
├── composables/useDict.js
├── components/{DictTag.vue, Editor.vue}
├── api/system/{dict,config,notice}.js
└── views/system/{dict/{index.vue,data.vue}, config/index.vue, notice/index.vue}
```

---

### Task 12: Dict Module (type + data, backend + frontend) + `useDict`/`DictTag` infrastructure

**Files:**
- Create: `backend/.../modules/dict/entity/SysDictType.java`, `SysDictData.java`
- Create: `backend/.../modules/dict/dto/SysDictTypeDTO.java`, `SysDictDataDTO.java`
- Create: `backend/.../modules/dict/mapper/SysDictTypeMapper.java`, `SysDictDataMapper.java`
- Create: `backend/.../modules/dict/service/{SysDictTypeService,SysDictDataService}.java` + impls
- Create: `backend/.../modules/dict/controller/{SysDictTypeController,SysDictDataController}.java`
- Test: `backend/src/test/java/com/yiling/modules/dict/SysDictControllerIT.java`
- Create: `frontend/src/composables/useDict.js`
- Create: `frontend/src/components/DictTag.vue`
- Create: `frontend/src/api/system/dict.js`
- Create: `frontend/src/views/system/dict/index.vue`, `data.vue`

**Interfaces:**
- Produces: `POST rest/sysDictType/{listPage,detailById,save,update,delete,updateSimple}`; `POST rest/sysDictData/{listPage,list,detailById,save,update,delete,updateSimple}` where `list {dictType} -> SysDictData[]` (unpaged, only `status=1`, sorted by `dictSort`) is what `useDict(...)` calls. `useDict('sys_user_sex', 'sys_normal_disable')` returns `{ sys_user_sex: Ref<[{label,value,elTagType}]>, sys_normal_disable: Ref<[...]> }`; every later page needing a dict-backed `<el-select>`/`<dict-tag>` uses this composable — don't hand-roll a separate dict fetch.

- [ ] **Step 1: `entity/SysDictType.java`**

```java
package com.yiling.modules.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dictName;
    private String dictType;
    private String remark;
}
```

- [ ] **Step 2: `entity/SysDictData.java`**

```java
package com.yiling.modules.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer dictSort;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String remark;
}
```

- [ ] **Step 3: DTOs**

```java
package com.yiling.modules.dict.dto;

import lombok.Data;

@Data
public class SysDictTypeDTO {
    private Long id;
    private String dictName;
    private String dictType;
    private String remark;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

```java
package com.yiling.modules.dict.dto;

import lombok.Data;

@Data
public class SysDictDataDTO {
    private Long id;
    private String dictType;
    private String dictLabel;
    private String dictValue;
    private Integer dictSort;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String remark;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

- [ ] **Step 4: Mappers** (both plain `BaseMapper`, no custom SQL needed)

```java
package com.yiling.modules.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.dict.entity.SysDictType;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {
}
```

```java
package com.yiling.modules.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.dict.entity.SysDictData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {
}
```

- [ ] **Step 5: `SysDictTypeService`/`Impl`** — same `IService`/`ServiceImpl` shape as Phase 1 Task 8's `SysPostService`

```java
package com.yiling.modules.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.dict.dto.SysDictTypeDTO;
import com.yiling.modules.dict.entity.SysDictType;

public interface SysDictTypeService extends IService<SysDictType> {
    PageResult<SysDictType> listPage(SysDictTypeDTO query);
    SysDictType detailById(Long id);
    void save(SysDictTypeDTO dto);
    void update(SysDictTypeDTO dto);
    void delete(java.util.List<Long> ids);
    void updateSimple(Long id, Integer status);
}
```

```java
package com.yiling.modules.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.dict.dto.SysDictTypeDTO;
import com.yiling.modules.dict.entity.SysDictType;
import com.yiling.modules.dict.mapper.SysDictTypeMapper;
import com.yiling.modules.dict.service.SysDictTypeService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

    @Override
    public PageResult<SysDictType> listPage(SysDictTypeDTO q) {
        LambdaQueryWrapper<SysDictType> qw = new LambdaQueryWrapper<>();
        qw.ne(SysDictType::getStatus, 0);
        qw.like(q.getDictName() != null && !q.getDictName().isBlank(), SysDictType::getDictName, q.getDictName());
        qw.like(q.getDictType() != null && !q.getDictType().isBlank(), SysDictType::getDictType, q.getDictType());
        qw.eq(q.getStatus() != null, SysDictType::getStatus, q.getStatus());
        Page<SysDictType> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public SysDictType detailById(Long id) {
        SysDictType d = getById(id);
        if (d == null) throw new BusinessException("字典类型不存在");
        return d;
    }

    @Override
    public void save(SysDictTypeDTO dto) {
        SysDictType entity = new SysDictType();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysDictTypeDTO dto) {
        SysDictType entity = new SysDictType();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysDictType e = new SysDictType();
            e.setId(id);
            e.setStatus(0);
            updateById(e);
        }
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysDictType e = new SysDictType();
        e.setId(id);
        e.setStatus(status);
        updateById(e);
    }
}
```

- [ ] **Step 6: `SysDictDataService`/`Impl`** — adds the unpaged `listByType` the frontend `useDict` composable calls

```java
package com.yiling.modules.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.dict.dto.SysDictDataDTO;
import com.yiling.modules.dict.entity.SysDictData;

import java.util.List;

public interface SysDictDataService extends IService<SysDictData> {
    PageResult<SysDictData> listPage(SysDictDataDTO query);
    List<SysDictData> listByType(String dictType);
    SysDictData detailById(Long id);
    void save(SysDictDataDTO dto);
    void update(SysDictDataDTO dto);
    void delete(List<Long> ids);
    void updateSimple(Long id, Integer status);
}
```

```java
package com.yiling.modules.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.dict.dto.SysDictDataDTO;
import com.yiling.modules.dict.entity.SysDictData;
import com.yiling.modules.dict.mapper.SysDictDataMapper;
import com.yiling.modules.dict.service.SysDictDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

    @Override
    public PageResult<SysDictData> listPage(SysDictDataDTO q) {
        LambdaQueryWrapper<SysDictData> qw = new LambdaQueryWrapper<>();
        qw.ne(SysDictData::getStatus, 0);
        qw.eq(SysDictData::getDictType, q.getDictType());
        qw.like(q.getDictLabel() != null && !q.getDictLabel().isBlank(), SysDictData::getDictLabel, q.getDictLabel());
        qw.eq(q.getStatus() != null, SysDictData::getStatus, q.getStatus());
        qw.orderByAsc(SysDictData::getDictSort);
        Page<SysDictData> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public List<SysDictData> listByType(String dictType) {
        LambdaQueryWrapper<SysDictData> qw = new LambdaQueryWrapper<>();
        qw.eq(SysDictData::getDictType, dictType);
        qw.eq(SysDictData::getStatus, 1);
        qw.orderByAsc(SysDictData::getDictSort);
        return list(qw);
    }

    @Override
    public SysDictData detailById(Long id) {
        SysDictData d = getById(id);
        if (d == null) throw new BusinessException("字典数据不存在");
        return d;
    }

    @Override
    public void save(SysDictDataDTO dto) {
        SysDictData entity = new SysDictData();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysDictDataDTO dto) {
        SysDictData entity = new SysDictData();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysDictData e = new SysDictData();
            e.setId(id);
            e.setStatus(0);
            updateById(e);
        }
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysDictData e = new SysDictData();
        e.setId(id);
        e.setStatus(status);
        updateById(e);
    }
}
```

- [ ] **Step 7: Controllers**

```java
package com.yiling.modules.dict.controller;

import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import com.yiling.modules.dict.dto.SysDictTypeDTO;
import com.yiling.modules.dict.entity.SysDictType;
import com.yiling.modules.dict.service.SysDictTypeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/sysDictType")
public class SysDictTypeController {

    private final SysDictTypeService service;

    public SysDictTypeController(SysDictTypeService service) {
        this.service = service;
    }

    @PostMapping("listPage")
    public Result<PageResult<SysDictType>> listPage(@RequestBody SysDictTypeDTO query) {
        return Result.success(service.listPage(query));
    }

    @PostMapping("detailById")
    public Result<SysDictType> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("save")
    public Result<Void> save(@RequestBody SysDictTypeDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Void> update(@RequestBody SysDictTypeDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("delete")
    @SuppressWarnings("unchecked")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        Object idsOrId = body.get("ids") != null ? body.get("ids") : List.of(body.get("id"));
        service.delete(((List<Object>) idsOrId).stream().map(o -> Long.valueOf(o.toString())).toList());
        return Result.success();
    }

    @PostMapping("updateSimple")
    public Result<Void> updateSimple(@RequestBody Map<String, Object> body) {
        service.updateSimple(Long.valueOf(body.get("id").toString()), Integer.valueOf(body.get("status").toString()));
        return Result.success();
    }
}
```

```java
package com.yiling.modules.dict.controller;

import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import com.yiling.modules.dict.dto.SysDictDataDTO;
import com.yiling.modules.dict.entity.SysDictData;
import com.yiling.modules.dict.service.SysDictDataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/sysDictData")
public class SysDictDataController {

    private final SysDictDataService service;

    public SysDictDataController(SysDictDataService service) {
        this.service = service;
    }

    @PostMapping("listPage")
    public Result<PageResult<SysDictData>> listPage(@RequestBody SysDictDataDTO query) {
        return Result.success(service.listPage(query));
    }

    @PostMapping("list")
    public Result<List<SysDictData>> list(@RequestBody Map<String, Object> body) {
        return Result.success(service.listByType((String) body.get("dictType")));
    }

    @PostMapping("detailById")
    public Result<SysDictData> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("save")
    public Result<Void> save(@RequestBody SysDictDataDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Void> update(@RequestBody SysDictDataDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("delete")
    @SuppressWarnings("unchecked")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        Object idsOrId = body.get("ids") != null ? body.get("ids") : List.of(body.get("id"));
        service.delete(((List<Object>) idsOrId).stream().map(o -> Long.valueOf(o.toString())).toList());
        return Result.success();
    }

    @PostMapping("updateSimple")
    public Result<Void> updateSimple(@RequestBody Map<String, Object> body) {
        service.updateSimple(Long.valueOf(body.get("id").toString()), Integer.valueOf(body.get("status").toString()));
        return Result.success();
    }
}
```

- [ ] **Step 8: integration test**

```java
package com.yiling.modules.dict;

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
class SysDictControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    void listByType_returnsSeededUserSexOptions() throws Exception {
        mockMvc.perform(post("/rest/sysDictData/list").contentType(MediaType.APPLICATION_JSON).content("{\"dictType\":\"sys_user_sex\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].dictLabel").value("男"));
    }
}
```

- [ ] **Step 9: run tests** — `mvn -q test -Dtest=SysDictControllerIT` — expect PASS.

- [ ] **Step 10: `frontend/src/composables/useDict.js`**

```js
import { reactive } from 'vue'
import request from '../utils/request'

const cache = {}

function normalize(rows) {
  return rows.map((r) => ({ label: r.dictLabel, value: r.dictValue, elTagType: r.listClass || '' }))
}

export function useDict(...types) {
  const result = {}
  types.forEach((type) => {
    result[type] = reactive({ value: [] })
    if (cache[type]) {
      result[type].value = cache[type]
      return
    }
    request.post('/rest/sysDictData/list', { dictType: type }).then((rows) => {
      const normalized = normalize(rows)
      cache[type] = normalized
      result[type].value = normalized
    })
  })
  return result
}
```

- [ ] **Step 11: `frontend/src/components/DictTag.vue`**

```vue
<template>
  <el-tag v-if="matched" :type="matched.elTagType || undefined">{{ matched.label }}</el-tag>
  <span v-else>{{ value }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ options: { type: Array, default: () => [] }, value: [String, Number] })
const matched = computed(() => props.options.find((o) => String(o.value) === String(props.value)))
</script>
```

- [ ] **Step 12: `frontend/src/components/Editor.vue`** (wangEditor v5 wrapper, used by Notice in Task 14)

```vue
<template>
  <div style="border:1px solid #ccc;">
    <Toolbar :editor="editorRef" :default-config="toolbarConfig" style="border-bottom:1px solid #ccc;" />
    <Editor v-model="content" :default-config="editorConfig" style="height:300px; overflow-y:auto;" @on-created="(e) => (editorRef = e)" />
  </div>
</template>

<script setup>
import { ref, shallowRef, watch, onBeforeUnmount } from 'vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const props = defineProps({ modelValue: { type: String, default: '' } })
const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()
const content = ref(props.modelValue)
const toolbarConfig = {}
const editorConfig = { placeholder: '请输入内容...' }

watch(() => props.modelValue, (v) => { if (v !== content.value) content.value = v })
watch(content, (v) => emit('update:modelValue', v))

onBeforeUnmount(() => {
  if (editorRef.value) editorRef.value.destroy()
})
</script>
```

- [ ] **Step 13: `frontend/src/api/system/dict.js`**

```js
import request from '../../utils/request'

export function listDictTypePage(query) { return request.post('/rest/sysDictType/listPage', query) }
export function getDictType(id) { return request.post('/rest/sysDictType/detailById', { id }) }
export function addDictType(data) { return request.post('/rest/sysDictType/save', data) }
export function updateDictType(data) { return request.post('/rest/sysDictType/update', data) }
export function delDictType(ids) { return request.post('/rest/sysDictType/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }

export function listDictDataPage(query) { return request.post('/rest/sysDictData/listPage', query) }
export function listDictDataByType(dictType) { return request.post('/rest/sysDictData/list', { dictType }) }
export function getDictData(id) { return request.post('/rest/sysDictData/detailById', { id }) }
export function addDictData(data) { return request.post('/rest/sysDictData/save', data) }
export function updateDictData(data) { return request.post('/rest/sysDictData/update', data) }
export function delDictData(ids) { return request.post('/rest/sysDictData/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
```

- [ ] **Step 14: `frontend/src/views/system/dict/index.vue`** (type page — copies the Post reference layout)

```vue
<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="字典名称"><el-input v-model="query.dictName" clearable /></el-form-item>
      <el-form-item label="字典类型"><el-input v-model="query.dictType" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:dict:add']" @click="openDialog()">新增</el-button>
      <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:dict:remove']" @click="handleBatchDelete">批量删除</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="dictName" label="字典名称" />
      <el-table-column prop="dictType" label="字典类型">
        <template #default="{ row }"><el-link type="primary" @click="goData(row)">{{ row.dictType }}</el-link></template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:dict:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="danger" v-hasPermi="['system:dict:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改字典类型' : '新增字典类型'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName"><el-input v-model="form.dictName" /></el-form-item>
        <el-form-item label="字典类型" prop="dictType"><el-input v-model="form.dictType" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="状态" v-if="form.id"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group></el-form-item>
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
import { listDictTypePage, addDictType, updateDictType, delDictType } from '../../../api/system/dict'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const router = useRouter()
const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ dictName: '', dictType: '', pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, dictName: '', dictType: '', status: 1, remark: '' })
const rules = { dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }], dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listDictTypePage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.dictName = ''; query.dictType = ''; query.pageNo = 1; fetchList() }

function openDialog(row) {
  Object.assign(form, { id: null, dictName: '', dictType: '', status: 1, remark: '' })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateDictType(form)
    else await addDictType(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function goData(row) { router.push(`/system/dict-data/${row.dictType}`) }

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除字典"${row.dictName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDictType(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDictType(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
```

- [ ] **Step 15: `frontend/src/views/system/dict/data.vue`** (data page)

```vue
<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="字典标签"><el-input v-model="query.dictLabel" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:dict:add']" @click="openDialog()">新增</el-button>
      <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:dict:remove']" @click="handleBatchDelete">批量删除</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="dictLabel" label="字典标签" />
      <el-table-column prop="dictValue" label="字典键值" />
      <el-table-column prop="dictSort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:dict:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="danger" v-hasPermi="['system:dict:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改字典数据' : '新增字典数据'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="字典标签" prop="dictLabel"><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="字典键值" prop="dictValue"><el-input v-model="form.dictValue" /></el-form-item>
        <el-form-item label="排序" prop="dictSort"><el-input-number v-model="form.dictSort" :min="0" /></el-form-item>
        <el-form-item label="样式属性"><el-input v-model="form.cssClass" /></el-form-item>
        <el-form-item label="回显样式"><el-input v-model="form.listClass" placeholder="primary/success/warning/danger/info" /></el-form-item>
        <el-form-item label="是否默认"><el-radio-group v-model="form.isDefault"><el-radio value="Y">是</el-radio><el-radio value="N">否</el-radio></el-radio-group></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDictDataPage, addDictData, updateDictData, delDictData } from '../../../api/system/dict'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const route = useRoute()
const dictType = route.params.dictType
const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ dictType, dictLabel: '', pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, dictType, dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', listClass: '', isDefault: 'N', status: 1, remark: '' })
const rules = { dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }], dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }], dictSort: [{ required: true, message: '请输入排序', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listDictDataPage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.dictLabel = ''; query.pageNo = 1; fetchList() }

function openDialog(row) {
  Object.assign(form, { id: null, dictType, dictLabel: '', dictValue: '', dictSort: 0, cssClass: '', listClass: '', isDefault: 'N', status: 1, remark: '' })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateDictData(form)
    else await addDictData(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除字典数据"${row.dictLabel}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDictData(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    await delDictData(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

onMounted(fetchList)
</script>
```

- [ ] **Step 16: register the data-page route** — add to `frontend/src/router/index.js` `Layout` children:

```js
{ path: 'system/dict-data/:dictType', name: 'DictData', component: () => import('../views/system/dict/data.vue'), meta: { title: '字典数据' } }
```

- [ ] **Step 17: Commit**

```bash
git add backend/src frontend/src
git commit -m "feat: dict module (type+data) plus useDict/DictTag/Editor shared infra"
```

---

### Task 13: Config Module (backend + frontend)

**Files:** mirrors Task 8 (Post) exactly.
- Create: `backend/.../modules/config/entity/SysConfig.java`
- Create: `backend/.../modules/config/dto/SysConfigDTO.java`
- Create: `backend/.../modules/config/mapper/SysConfigMapper.java`
- Create: `backend/.../modules/config/service/SysConfigService.java` + `impl/SysConfigServiceImpl.java`
- Create: `backend/.../modules/config/controller/SysConfigController.java`
- Test: `backend/src/test/java/com/yiling/modules/config/SysConfigControllerIT.java`
- Create: `frontend/src/api/system/config.js`
- Create: `frontend/src/views/system/config/index.vue`

**Interfaces:** `POST rest/sysConfig/{listPage,detailById,save,update,delete,updateSimple,refreshCache}`. `refreshCache` is a no-op (`Result.success()`) since this build has no Redis/cache layer — the button exists per spec, the backend endpoint is honest about doing nothing.

- [ ] **Step 1: `entity/SysConfig.java`**

```java
package com.yiling.modules.config.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configName;
    private String configKey;
    private String configValue;
    private String configType;
    private String remark;
}
```

- [ ] **Step 2: `dto/SysConfigDTO.java`**

```java
package com.yiling.modules.config.dto;

import lombok.Data;

@Data
public class SysConfigDTO {
    private Long id;
    private String configName;
    private String configKey;
    private String configValue;
    private String configType;
    private String remark;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

- [ ] **Step 3: `mapper/SysConfigMapper.java`**

```java
package com.yiling.modules.config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.config.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
```

- [ ] **Step 4: `service/SysConfigService.java` + `impl/SysConfigServiceImpl.java`**

```java
package com.yiling.modules.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.config.dto.SysConfigDTO;
import com.yiling.modules.config.entity.SysConfig;

import java.util.List;

public interface SysConfigService extends IService<SysConfig> {
    PageResult<SysConfig> listPage(SysConfigDTO query);
    SysConfig detailById(Long id);
    void save(SysConfigDTO dto);
    void update(SysConfigDTO dto);
    void delete(List<Long> ids);
}
```

```java
package com.yiling.modules.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.config.dto.SysConfigDTO;
import com.yiling.modules.config.entity.SysConfig;
import com.yiling.modules.config.mapper.SysConfigMapper;
import com.yiling.modules.config.service.SysConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public PageResult<SysConfig> listPage(SysConfigDTO q) {
        LambdaQueryWrapper<SysConfig> qw = new LambdaQueryWrapper<>();
        qw.ne(SysConfig::getStatus, 0);
        qw.like(q.getConfigName() != null && !q.getConfigName().isBlank(), SysConfig::getConfigName, q.getConfigName());
        qw.like(q.getConfigKey() != null && !q.getConfigKey().isBlank(), SysConfig::getConfigKey, q.getConfigKey());
        qw.eq(q.getConfigType() != null && !q.getConfigType().isBlank(), SysConfig::getConfigType, q.getConfigType());
        Page<SysConfig> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public SysConfig detailById(Long id) {
        SysConfig c = getById(id);
        if (c == null) throw new BusinessException("参数不存在");
        return c;
    }

    @Override
    public void save(SysConfigDTO dto) {
        SysConfig entity = new SysConfig();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysConfigDTO dto) {
        SysConfig entity = new SysConfig();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysConfig e = new SysConfig();
            e.setId(id);
            e.setStatus(0);
            updateById(e);
        }
    }
}
```

- [ ] **Step 5: `controller/SysConfigController.java`**

```java
package com.yiling.modules.config.controller;

import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import com.yiling.modules.config.dto.SysConfigDTO;
import com.yiling.modules.config.entity.SysConfig;
import com.yiling.modules.config.service.SysConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/sysConfig")
public class SysConfigController {

    private final SysConfigService service;

    public SysConfigController(SysConfigService service) {
        this.service = service;
    }

    @PostMapping("listPage")
    public Result<PageResult<SysConfig>> listPage(@RequestBody SysConfigDTO query) {
        return Result.success(service.listPage(query));
    }

    @PostMapping("detailById")
    public Result<SysConfig> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("save")
    public Result<Void> save(@RequestBody SysConfigDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Void> update(@RequestBody SysConfigDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("delete")
    @SuppressWarnings("unchecked")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        Object idsOrId = body.get("ids") != null ? body.get("ids") : List.of(body.get("id"));
        service.delete(((List<Object>) idsOrId).stream().map(o -> Long.valueOf(o.toString())).toList());
        return Result.success();
    }

    @PostMapping("refreshCache")
    public Result<Void> refreshCache() {
        return Result.success();
    }
}
```

- [ ] **Step 6: integration test**

```java
package com.yiling.modules.config;

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
class SysConfigControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @WithMockUser
    void listPage_findsSeededSkinConfig() throws Exception {
        mockMvc.perform(post("/rest/sysConfig/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"configKey\":\"sys.index.skinName\",\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].configValue").value("skin-blue"));
    }
}
```

- [ ] **Step 7: run tests** — `mvn -q test -Dtest=SysConfigControllerIT` — expect PASS.

- [ ] **Step 8: `frontend/src/api/system/config.js`**

```js
import request from '../../utils/request'

export function listConfigPage(query) { return request.post('/rest/sysConfig/listPage', query) }
export function getConfig(id) { return request.post('/rest/sysConfig/detailById', { id }) }
export function addConfig(data) { return request.post('/rest/sysConfig/save', data) }
export function updateConfig(data) { return request.post('/rest/sysConfig/update', data) }
export function delConfig(ids) { return request.post('/rest/sysConfig/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
export function refreshConfigCache() { return request.post('/rest/sysConfig/refreshCache', {}) }
```

- [ ] **Step 9: `frontend/src/views/system/config/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="参数名称"><el-input v-model="query.configName" clearable /></el-form-item>
      <el-form-item label="参数键名"><el-input v-model="query.configKey" clearable /></el-form-item>
      <el-form-item label="内置">
        <el-select v-model="query.configType" clearable style="width:120px"><el-option label="是" value="Y" /><el-option label="否" value="N" /></el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:config:add']" @click="openDialog()">新增</el-button>
      <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:config:remove']" @click="handleBatchDelete">批量删除</el-button>
      <el-button @click="handleRefresh">刷新缓存</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="id" label="编号" width="80" />
      <el-table-column prop="configName" label="参数名称" />
      <el-table-column prop="configKey" label="参数键名" />
      <el-table-column prop="configValue" label="参数键值" />
      <el-table-column label="内置" width="80"><template #default="{ row }">{{ row.configType === 'Y' ? '是' : '否' }}</template></el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:config:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="danger" v-hasPermi="['system:config:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改参数' : '新增参数'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="参数名称" prop="configName"><el-input v-model="form.configName" /></el-form-item>
        <el-form-item label="参数键名" prop="configKey"><el-input v-model="form.configKey" /></el-form-item>
        <el-form-item label="参数键值" prop="configValue"><el-input v-model="form.configValue" /></el-form-item>
        <el-form-item label="是否内置"><el-radio-group v-model="form.configType"><el-radio value="Y">是</el-radio><el-radio value="N">否</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listConfigPage, addConfig, updateConfig, delConfig, refreshConfigCache } from '../../../api/system/config'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ configName: '', configKey: '', configType: null, pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, configName: '', configKey: '', configValue: '', configType: 'N', remark: '' })
const rules = { configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }], configKey: [{ required: true, message: '请输入参数键名', trigger: 'blur' }], configValue: [{ required: true, message: '请输入参数键值', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listConfigPage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.configName = ''; query.configKey = ''; query.configType = null; query.pageNo = 1; fetchList() }

function openDialog(row) {
  Object.assign(form, { id: null, configName: '', configKey: '', configValue: '', configType: 'N', remark: '' })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateConfig(form)
    else await addConfig(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除参数"${row.configName}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delConfig(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    await delConfig(selected.value)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleRefresh() {
  refreshConfigCache().then(() => ElMessage.success('刷新成功'))
}

onMounted(fetchList)
</script>
```

- [ ] **Step 10: Commit**

```bash
git add backend/src frontend/src
git commit -m "feat: config module (backend+frontend)"
```

---

### Task 14: Notice Module (backend + frontend, with rich-text Editor)

**Files:** mirrors Task 13 (Config) with a `notice_content` rich-text field instead of key/value.
- Create: `backend/.../modules/notice/entity/SysNotice.java`
- Create: `backend/.../modules/notice/dto/SysNoticeDTO.java`
- Create: `backend/.../modules/notice/mapper/SysNoticeMapper.java`
- Create: `backend/.../modules/notice/service/SysNoticeService.java` + `impl/SysNoticeServiceImpl.java`
- Create: `backend/.../modules/notice/controller/SysNoticeController.java`
- Test: `backend/src/test/java/com/yiling/modules/notice/SysNoticeControllerIT.java`
- Create: `frontend/src/api/system/notice.js`
- Create: `frontend/src/views/system/notice/index.vue`

**Interfaces:** `POST rest/sysNotice/{listPage,detailById,save,update,delete}` (no `updateSimple` — no status-switch UI requested for this module per spec).

- [ ] **Step 1: `entity/SysNotice.java`**

```java
package com.yiling.modules.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notice")
public class SysNotice extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String noticeTitle;
    private String noticeType;
    private String noticeContent;
    private String noticeParam;
}
```

- [ ] **Step 2: `dto/SysNoticeDTO.java`**

```java
package com.yiling.modules.notice.dto;

import lombok.Data;

@Data
public class SysNoticeDTO {
    private Long id;
    private String noticeTitle;
    private String noticeType;
    private String noticeContent;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
```

- [ ] **Step 3: `mapper/SysNoticeMapper.java`**

```java
package com.yiling.modules.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.notice.entity.SysNotice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNotice> {
}
```

- [ ] **Step 4: `service/SysNoticeService.java` + `impl/SysNoticeServiceImpl.java`**

```java
package com.yiling.modules.notice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.notice.dto.SysNoticeDTO;
import com.yiling.modules.notice.entity.SysNotice;

import java.util.List;

public interface SysNoticeService extends IService<SysNotice> {
    PageResult<SysNotice> listPage(SysNoticeDTO query);
    SysNotice detailById(Long id);
    void save(SysNoticeDTO dto);
    void update(SysNoticeDTO dto);
    void delete(List<Long> ids);
}
```

```java
package com.yiling.modules.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.notice.dto.SysNoticeDTO;
import com.yiling.modules.notice.entity.SysNotice;
import com.yiling.modules.notice.mapper.SysNoticeMapper;
import com.yiling.modules.notice.service.SysNoticeService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {

    @Override
    public PageResult<SysNotice> listPage(SysNoticeDTO q) {
        LambdaQueryWrapper<SysNotice> qw = new LambdaQueryWrapper<>();
        qw.ne(SysNotice::getStatus, 0);
        qw.like(q.getNoticeTitle() != null && !q.getNoticeTitle().isBlank(), SysNotice::getNoticeTitle, q.getNoticeTitle());
        qw.eq(q.getNoticeType() != null && !q.getNoticeType().isBlank(), SysNotice::getNoticeType, q.getNoticeType());
        qw.orderByDesc(SysNotice::getCreateTime);
        Page<SysNotice> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public SysNotice detailById(Long id) {
        SysNotice n = getById(id);
        if (n == null) throw new BusinessException("通知公告不存在");
        return n;
    }

    @Override
    public void save(SysNoticeDTO dto) {
        SysNotice entity = new SysNotice();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysNoticeDTO dto) {
        SysNotice entity = new SysNotice();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysNotice e = new SysNotice();
            e.setId(id);
            e.setStatus(0);
            updateById(e);
        }
    }
}
```

- [ ] **Step 5: `controller/SysNoticeController.java`**

```java
package com.yiling.modules.notice.controller;

import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import com.yiling.modules.notice.dto.SysNoticeDTO;
import com.yiling.modules.notice.entity.SysNotice;
import com.yiling.modules.notice.service.SysNoticeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("rest/sysNotice")
public class SysNoticeController {

    private final SysNoticeService service;

    public SysNoticeController(SysNoticeService service) {
        this.service = service;
    }

    @PostMapping("listPage")
    public Result<PageResult<SysNotice>> listPage(@RequestBody SysNoticeDTO query) {
        return Result.success(service.listPage(query));
    }

    @PostMapping("detailById")
    public Result<SysNotice> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("save")
    public Result<Void> save(@RequestBody SysNoticeDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Void> update(@RequestBody SysNoticeDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("delete")
    @SuppressWarnings("unchecked")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        Object idsOrId = body.get("ids") != null ? body.get("ids") : List.of(body.get("id"));
        service.delete(((List<Object>) idsOrId).stream().map(o -> Long.valueOf(o.toString())).toList());
        return Result.success();
    }
}
```

- [ ] **Step 6: integration test**

```java
package com.yiling.modules.notice;

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
class SysNoticeControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void crud_roundTrip() throws Exception {
        Map<String, Object> create = Map.of("noticeTitle", "系统维护通知", "noticeType", "1", "noticeContent", "<p>今晚维护</p>");
        mockMvc.perform(post("/rest/sysNotice/save").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rest/sysNotice/listPage").contentType(MediaType.APPLICATION_JSON).content("{\"noticeTitle\":\"维护\",\"pageNo\":1,\"pageSize\":10}"))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
```

- [ ] **Step 7: run tests** — `mvn -q test -Dtest=SysNoticeControllerIT` — expect PASS.

- [ ] **Step 8: `frontend/src/api/system/notice.js`**

```js
import request from '../../utils/request'

export function listNoticePage(query) { return request.post('/rest/sysNotice/listPage', query) }
export function getNotice(id) { return request.post('/rest/sysNotice/detailById', { id }) }
export function addNotice(data) { return request.post('/rest/sysNotice/save', data) }
export function updateNotice(data) { return request.post('/rest/sysNotice/update', data) }
export function delNotice(ids) { return request.post('/rest/sysNotice/delete', { ids: Array.isArray(ids) ? ids : [ids] }) }
```

- [ ] **Step 9: `frontend/src/views/system/notice/index.vue`**

```vue
<template>
  <div class="app-container">
    <el-form v-show="showSearch" :model="query" inline>
      <el-form-item label="公告标题"><el-input v-model="query.noticeTitle" clearable /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.noticeType" clearable style="width:120px"><el-option label="通知" value="1" /><el-option label="公告" value="2" /></el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" @click="fetchList">搜索</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-button type="primary" v-hasPermi="['system:notice:add']" @click="openDialog()">新增</el-button>
      <el-button type="danger" :disabled="!selected.length" v-hasPermi="['system:notice:remove']" @click="handleBatchDelete">批量删除</el-button>
      <right-toolbar v-model:show-search="showSearch" @query-table="fetchList" />
    </el-row>

    <el-table v-loading="loading" :data="tableData" @selection-change="(v) => (selected = v.map((r) => r.id))">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="noticeTitle" label="公告标题" />
      <el-table-column label="类型" width="90"><template #default="{ row }"><el-tag :type="row.noticeType === '1' ? 'warning' : 'success'">{{ row.noticeType === '1' ? '通知' : '公告' }}</el-tag></template></el-table-column>
      <el-table-column prop="createUser" label="创建者" width="120" />
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['system:notice:edit']" @click="openDialog(row)">修改</el-button>
          <el-button link type="danger" v-hasPermi="['system:notice:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-model:page="query.pageNo" v-model:limit="query.pageSize" :total="total" @pagination="fetchList" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '修改公告' : '新增公告'" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="noticeTitle"><el-input v-model="form.noticeTitle" /></el-form-item>
        <el-form-item label="类型" prop="noticeType"><el-radio-group v-model="form.noticeType"><el-radio value="1">通知</el-radio><el-radio value="2">公告</el-radio></el-radio-group></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">正常</el-radio><el-radio :value="2">关闭</el-radio></el-radio-group></el-form-item>
        <el-form-item label="内容" prop="noticeContent"><editor v-model="form.noticeContent" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listNoticePage, addNotice, updateNotice, delNotice } from '../../../api/system/notice'
import Pagination from '../../../components/Pagination.vue'
import RightToolbar from '../../../components/RightToolbar.vue'
import Editor from '../../../components/Editor.vue'

const showSearch = ref(true)
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const selected = ref([])
const query = reactive({ noticeTitle: '', noticeType: null, pageNo: 1, pageSize: 10 })
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({ id: null, noticeTitle: '', noticeType: '1', status: 1, noticeContent: '' })
const rules = { noticeTitle: [{ required: true, message: '请输入公告标题', trigger: 'blur' }], noticeType: [{ required: true, message: '请选择类型', trigger: 'change' }], noticeContent: [{ required: true, message: '请输入内容', trigger: 'blur' }] }

async function fetchList() {
  loading.value = true
  try {
    const data = await listNoticePage(query)
    tableData.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetQuery() { query.noticeTitle = ''; query.noticeType = null; query.pageNo = 1; fetchList() }

function openDialog(row) {
  Object.assign(form, { id: null, noticeTitle: '', noticeType: '1', status: 1, noticeContent: '' })
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) await updateNotice(form)
    else await addNotice(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchList()
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除公告"${row.noticeTitle}"吗？`, '提示', { type: 'warning' }).then(async () => {
    await delNotice(row.id)
    ElMessage.success('删除成功')
    fetchList()
  })
}

function handleBatchDelete() {
  ElMessageBox.confirm(`确定删除选中的 ${selected.value.length} 条数据吗？`, '提示', { type: 'warning' }).then(async () => {
    await delNotice(selected.value)
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
git commit -m "feat: notice module with rich-text editor (backend+frontend)"
```

---

## Phase 2 Definition of Done

- `SysDictControllerIT`, `SysConfigControllerIT`, `SysNoticeControllerIT` all pass.
- In the browser: `/system/dict` → click a type → `/system/dict-data/:type` shows its entries; `/system/config` and `/system/notice` (with working rich-text editor) support full CRUD+pagination.
- Some other module's status column (e.g. User's 性别/sex column, if added) can consume `useDict('sys_user_sex')` + `<dict-tag>` without hand-rolled fetch code — verify by wiring the Sex select in `frontend/src/views/system/user/index.vue`'s table to use `useDict('sys_user_sex')` + `DictTag` instead of hardcoded 男/女/未知 labels, as a quick retrofit at the start of this task's testing.

Next: `docs/superpowers/plans/2026-08-27-phase3-log-and-verify.md` (Operation Log AOP + final E2E verification).
