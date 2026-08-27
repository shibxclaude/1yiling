package com.yiling.modules.dept.dto;

import lombok.Data;

@Data
public class SysDeptDTO {
    private Long id;
    private Long parentId;
    private String deptCode;
    private String deptName;
    private Integer orderNum;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
}
