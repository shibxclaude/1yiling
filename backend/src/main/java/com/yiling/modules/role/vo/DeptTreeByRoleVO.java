package com.yiling.modules.role.vo;

import com.yiling.common.vo.TreeSelectVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DeptTreeByRoleVO {
    private List<Long> checkedKeys;
    private List<TreeSelectVO> depts;
}
