package com.yiling.modules.user.service;

import com.yiling.common.result.PageResult;
import com.yiling.modules.user.dto.*;
import com.yiling.modules.user.entity.SysUser;
import com.yiling.modules.user.vo.SysUserDetailVO;
import com.yiling.modules.user.vo.SysUserVO;

import java.util.List;

public interface SysUserService {
    PageResult<SysUserVO> listPage(SysUserDTO query);
    List<SysUserVO> list(SysUserDTO query);
    SysUserDetailVO detailById(Long id);
    void save(SysUserDTO dto);
    void update(SysUserDTO dto);
    void delete(Long id);
    void updateSimple(Long id, Integer status);
    void resetPwd(ResetPwdDTO dto);
    SysUserDetailVO profile(String username);
    void updateSelfSimple(String username, UpdateSelfDTO dto);
    void updatePwd(String username, UpdatePwdDTO dto);
    String updateAvatar(String username, String avatarUrl);
}
