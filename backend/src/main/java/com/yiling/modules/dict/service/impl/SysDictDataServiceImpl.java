package com.yiling.modules.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.dict.dto.SysDictDataDTO;
import com.yiling.modules.dict.entity.SysDictData;
import com.yiling.modules.dict.mapper.SysDictDataMapper;
import com.yiling.modules.dict.service.SysDictDataService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> implements SysDictDataService {

    @Override
    public PageResult<SysDictData> listPage(SysDictDataDTO q) {
        LambdaQueryWrapper<SysDictData> qw = new LambdaQueryWrapper<>();
        qw.ne(SysDictData::getStatus, 0);
        qw.eq(SysDictData::getDictType, q.getDictType());
        qw.like(q.getDictLabel() != null && !q.getDictLabel().isBlank(), SysDictData::getDictLabel, q.getDictLabel());
        qw.eq(q.getStatus() != null, SysDictData::getStatus, q.getStatus());
        qw.orderByAsc(SysDictData::getDictSort);
        Page<SysDictData> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public List<SysDictData> listByType(String dictType) {
        LambdaQueryWrapper<SysDictData> qw = new LambdaQueryWrapper<>();
        qw.eq(SysDictData::getDictType, dictType);
        qw.eq(SysDictData::getStatus, 1);
        qw.orderByAsc(SysDictData::getDictSort);
        return list(qw);
    }

    @Override
    public SysDictData detailById(Long id) {
        SysDictData d = getById(id);
        if (d == null) throw new BusinessException("字典数据不存在");
        return d;
    }

    @Override
    public void save(SysDictDataDTO dto) {
        SysDictData entity = new SysDictData();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysDictDataDTO dto) {
        SysDictData entity = new SysDictData();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysDictData e = new SysDictData();
            e.setId(id);
            e.setStatus(0);
            updateById(e);
        }
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysDictData e = new SysDictData();
        e.setId(id);
        e.setStatus(status);
        updateById(e);
    }
}
