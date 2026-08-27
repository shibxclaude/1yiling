package com.yiling.modules.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yiling.common.result.PageResult;
import com.yiling.modules.dict.dto.SysDictDataDTO;
import com.yiling.modules.dict.entity.SysDictData;

import java.util.List;

public interface SysDictDataService extends IService<SysDictData> {
    PageResult<SysDictData> listPage(SysDictDataDTO query);
    List<SysDictData> listByType(String dictType);
    SysDictData detailById(Long id);
    void save(SysDictDataDTO dto);
    void update(SysDictDataDTO dto);
    void delete(List<Long> ids);
    void updateSimple(Long id, Integer status);
}
