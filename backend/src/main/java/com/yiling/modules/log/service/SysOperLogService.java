package com.yiling.modules.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.log.dto.SysOperLogDTO;
import com.yiling.modules.log.entity.SysOperLog;

import java.util.List;

public interface SysOperLogService extends IService<SysOperLog> {
    PageResult<SysOperLog> listPage(SysOperLogDTO query);
    SysOperLog detailById(Long id);
    void delete(List<Long> ids);
}
