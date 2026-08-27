package com.yiling.modules.role.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    // IGNORED: default NOT_NULL strategy would silently skip clearing this column when
    // saveDataScope sets it back to null after switching away from custom (dataScope=2) scope.
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private String deptIds;
    private Boolean menuCheckStrictly;
    private Boolean deptCheckStrictly;
    private String remark;
}
