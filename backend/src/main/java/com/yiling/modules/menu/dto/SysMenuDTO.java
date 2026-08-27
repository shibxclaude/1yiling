package com.yiling.modules.menu.dto;

import lombok.Data;

@Data
public class SysMenuDTO {
    private Long id;
    private Long parentId;
    private String menuName;
    private String menuType;
    private Integer menuSort;
    private String icon;
    private String menuPath;
    private String menuComponent;
    private String perms;
    private String queryParam;
    private Boolean ifFrame;
    private Boolean ifCache;
    private Integer status;
}
