package com.yiling.modules.notice.dto;

import lombok.Data;

@Data
public class SysNoticeDTO {
    private Long id;
    private String noticeTitle;
    private String noticeType;
    private String noticeContent;
    private Integer status;
    private Integer pageNo = 1;
    private Integer pageSize = 10;
}
