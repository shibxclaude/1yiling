package com.yiling.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deptId;
    private String username;
    private String nickName;
    private String passwd;
    private String sex;
    private String email;
    private String phone;
    private String avatar;
    private String remark;
}
