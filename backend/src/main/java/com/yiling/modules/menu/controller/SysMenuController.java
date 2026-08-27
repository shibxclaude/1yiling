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
