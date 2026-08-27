package com.yiling.modules.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class SysUserDTO {
    private Long id;
    private Long deptId;
    private String username;
    private String nickName;
    private String passwd;
    private String sex;
    private String email;
    private String phone;
    private Integer status;
    private String remark;
    private List<Long> roleIds;
    private List<Long> postIds;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
