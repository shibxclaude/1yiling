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
