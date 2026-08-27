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
