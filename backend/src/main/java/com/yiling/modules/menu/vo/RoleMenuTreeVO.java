package com.yiling.modules.menu.vo;

import com.yiling.common.vo.TreeSelectVO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoleMenuTreeVO {
    private List<Long> checkedKeys;
    private List<TreeSelectVO> menus;
}
