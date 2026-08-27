package com.yiling.modules.role.dto;

import lombok.Data;

import java.util.List;

@Data
public class SysRoleDTO {
    private Long id;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private Integer status;
    private String remark;
    private List<Long> menuIds;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
