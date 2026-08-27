package com.yiling.modules.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.dict.dto.SysDictTypeDTO;
import com.yiling.modules.dict.entity.SysDictType;

public interface SysDictTypeService extends IService<SysDictType> {
    PageResult<SysDictType> listPage(SysDictTypeDTO query);
    SysDictType detailById(Long id);
    void save(SysDictTypeDTO dto);
    void update(SysDictTypeDTO dto);
    void delete(java.util.List<Long> ids);
    void updateSimple(Long id, Integer status);
}
