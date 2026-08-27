package com.yiling.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yiling.modules.user.entity.SysUser;
import com.yiling.modules.user.vo.SysUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("<script>SELECT u.*, d.dept_name deptName FROM sys_user u LEFT JOIN sys_dept d ON d.id = u.dept_id " +
            "WHERE u.status != 0 " +
            "<if test='deptId != null'>AND u.dept_id = #{deptId}</if> " +
            "<if test='username != null and username != \"\"'>AND u.username LIKE CONCAT('%',#{username},'%')</if> " +
            "<if test='phone != null and phone != \"\"'>AND u.phone LIKE CONCAT('%',#{phone},'%')</if> " +
            "<if test='status != null'>AND u.status = #{status}</if> " +
            "ORDER BY u.create_time DESC</script>")
    List<SysUserVO> selectUserVOList(@Param("deptId") Long deptId, @Param("username") String username,
                                      @Param("phone") String phone, @Param("status") Integer status);
}
