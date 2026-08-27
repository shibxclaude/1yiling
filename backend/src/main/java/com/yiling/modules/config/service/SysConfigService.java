package com.yiling.modules.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.config.dto.SysConfigDTO;
import com.yiling.modules.config.entity.SysConfig;

import java.util.List;

public interface SysConfigService extends IService<SysConfig> {
    PageResult<SysConfig> listPage(SysConfigDTO query);
    SysConfig detailById(Long id);
    void save(SysConfigDTO dto);
    void update(SysConfigDTO dto);
    void delete(List<Long> ids);
}
