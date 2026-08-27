package com.yiling.modules.dept.service;

import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.dept.dto.SysDeptDTO;
import com.yiling.modules.dept.entity.SysDept;

import java.util.List;

public interface SysDeptService {
    List<SysDept> list(String deptName, Integer status);
    List<TreeSelectVO> deptTree();
    SysDept detailById(Long id);
    void save(SysDeptDTO dto);
    void update(SysDeptDTO dto);
    void delete(Long id);
}
