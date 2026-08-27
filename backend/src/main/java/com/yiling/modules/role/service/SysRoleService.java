package com.yiling.modules.role.service;

import com.yiling.common.result.PageResult;
import com.yiling.modules.role.dto.DataScopeDTO;
import com.yiling.modules.role.dto.SysRoleDTO;
import com.yiling.modules.role.entity.SysRole;

public interface SysRoleService {
    PageResult<SysRole> listPage(SysRoleDTO query);
    SysRole detailById(Long id);
    void save(SysRoleDTO dto);
    void update(SysRoleDTO dto);
    void delete(Long id);
    void updateSimple(Long id, Integer status);
    void saveDataScope(DataScopeDTO dto);
}
