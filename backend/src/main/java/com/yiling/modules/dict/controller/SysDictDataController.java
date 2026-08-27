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
