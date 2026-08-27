package com.yiling.modules.post.dto;

import lombok.Data;

@Data
public class SysPostDTO {
    private Long id;
    private String postCode;
    private String postName;
    private Integer postSort;
    private String remark;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
