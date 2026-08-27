package com.yiling.modules.role.controller;

import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
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
}
