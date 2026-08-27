package com.yiling.modules.dict.dto;

import lombok.Data;

@Data
public class SysDictTypeDTO {
    private Long id;
    private String dictName;
    private String dictType;
    private String remark;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
