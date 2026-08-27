package com.yiling.modules.user.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserPostMapper {
    @Select("SELECT post_id FROM sys_user_post WHERE user_id = #{userId}")
    List<Long> selectPostIdsByUser(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_post WHERE user_id = #{userId}")
    void deleteByUserId(@Param("userId") Long userId);

    @Insert("<script>INSERT INTO sys_user_post (user_id, post_id) VALUES " +
            "<foreach collection='postIds' item='postId' separator=','>(#{userId}, #{postId})</foreach></script>")
    void batchInsert(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
