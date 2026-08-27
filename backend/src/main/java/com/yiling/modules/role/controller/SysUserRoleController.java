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
