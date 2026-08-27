package com.yiling.modules.user.vo;

import com.yiling.modules.user.entity.SysUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserDetailVO extends SysUser {
    private List<Long> roleIds;
    private List<Long> postIds;
}
