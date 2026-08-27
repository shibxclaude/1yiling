package com.yiling.modules.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.menu.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRole(@Param("roleId") Long roleId);
}
