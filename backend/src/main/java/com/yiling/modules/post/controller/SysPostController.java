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
