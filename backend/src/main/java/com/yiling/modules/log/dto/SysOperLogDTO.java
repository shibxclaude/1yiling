package com.yiling.modules.log.dto;

import lombok.Data;

@Data
public class SysOperLogDTO {
    private String title;
    private Integer businessType;
    private String operName;
    private String operIp;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
