package com.yiling.modules.user.vo;

import com.yiling.modules.user.entity.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserVO extends SysUser {
    private String deptName;
}
