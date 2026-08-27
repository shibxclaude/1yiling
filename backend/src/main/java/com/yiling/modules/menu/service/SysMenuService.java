package com.yiling.modules.menu.service;

import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.menu.dto.SysMenuDTO;
import com.yiling.modules.menu.entity.SysMenu;
import com.yiling.modules.menu.vo.RoleMenuTreeVO;

import java.util.List;

public interface SysMenuService {
    List<SysMenu> list(String menuName, Integer status);
    List<TreeSelectVO> treeSelect();
    RoleMenuTreeVO roleMenuTreeSelect(Long roleId);
    SysMenu detailById(Long id);
    void save(SysMenuDTO dto);
    void update(SysMenuDTO dto);
    void delete(Long id);
}
