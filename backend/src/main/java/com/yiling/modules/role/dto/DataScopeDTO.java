package com.yiling.modules.role.dto;

import lombok.Data;

import java.util.List;

@Data
public class DataScopeDTO {
    private Long id;
    private String dataScope;
    private List<Long> deptIdList;
}
