package com.yiling.modules.menu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yiling.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType; // 0目录 1菜单 2按钮
    private Integer menuSort;
    private String icon;
    private String menuPath;
    private String menuComponent;
    private String perms;
    private String queryParam;
    private Boolean ifFrame;
    private Boolean ifCache;
}
