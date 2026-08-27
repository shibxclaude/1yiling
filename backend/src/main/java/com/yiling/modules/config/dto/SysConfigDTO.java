package com.yiling.modules.config.dto;

import lombok.Data;

@Data
public class SysConfigDTO {
    private Long id;
    private String configName;
    private String configKey;
    private String configValue;
    private String configType;
    private String remark;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
