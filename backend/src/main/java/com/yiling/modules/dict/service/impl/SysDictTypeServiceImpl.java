package com.yiling.modules.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.dict.dto.SysDictTypeDTO;
import com.yiling.modules.dict.entity.SysDictType;
import com.yiling.modules.dict.mapper.SysDictTypeMapper;
import com.yiling.modules.dict.service.SysDictTypeService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictTypeService {

    @Override
    public PageResult<SysDictType> listPage(SysDictTypeDTO q) {
        LambdaQueryWrapper<SysDictType> qw = new LambdaQueryWrapper<>();
        qw.ne(SysDictType::getStatus, 0);
        qw.like(q.getDictName() != null && !q.getDictName().isBlank(), SysDictType::getDictName, q.getDictName());
        qw.like(q.getDictType() != null && !q.getDictType().isBlank(), SysDictType::getDictType, q.getDictType());
        qw.eq(q.getStatus() != null, SysDictType::getStatus, q.getStatus());
        Page<SysDictType> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public SysDictType detailById(Long id) {
        SysDictType d = getById(id);
        if (d == null) throw new BusinessException("字典类型不存在");
        return d;
    }

    @Override
    public void save(SysDictTypeDTO dto) {
        SysDictType entity = new SysDictType();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysDictTypeDTO dto) {
        SysDictType entity = new SysDictType();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysDictType e = new SysDictType();
            e.setId(id);
            e.setStatus(0);
            updateById(e);
        }
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysDictType e = new SysDictType();
        e.setId(id);
        e.setStatus(status);
        updateById(e);
    }
}
