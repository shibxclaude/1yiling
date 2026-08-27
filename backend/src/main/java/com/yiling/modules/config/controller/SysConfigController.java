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
