package com.yiling.modules.role.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface SysRoleMenuMapper {
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("<script>INSERT INTO sys_role_menu (role_id, menu_id) VALUES " +
            "<foreach collection='menuIds' item='menuId' separator=','>(#{roleId}, #{menuId})</foreach></script>")
    void batchInsert(@Param("roleId") Long roleId, @Param("menuIds") java.util.List<Long> menuIds);
}
