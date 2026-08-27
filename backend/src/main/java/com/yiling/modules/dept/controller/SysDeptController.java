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
