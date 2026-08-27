package com.yiling.modules.user.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUser(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Insert("<script>INSERT INTO sys_user_role (user_id, role_id) VALUES " +
            "<foreach collection='roleIds' item='roleId' separator=','>(#{userId}, #{roleId})</foreach></script>")
    void batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
