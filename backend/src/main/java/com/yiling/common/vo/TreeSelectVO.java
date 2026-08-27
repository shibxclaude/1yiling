package com.yiling.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TreeSelectVO {
    private Long id;
    private String label;
    private List<TreeSelectVO> children;
}
