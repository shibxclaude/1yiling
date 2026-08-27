package com.yiling.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Integer businessType;
    private String method;
    private String requestMethod;
    private Integer operatorType;
    private Long operId;
    private String operName;
    private Long deptId;
    private String deptName;
    private String operUrl;
    private String operIp;
    private String operLocation;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operTime;
    private Long costTime;
}
