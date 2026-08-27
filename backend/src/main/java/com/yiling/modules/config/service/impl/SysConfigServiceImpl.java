package com.yiling.modules.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.config.dto.SysConfigDTO;
import com.yiling.modules.config.entity.SysConfig;
import com.yiling.modules.config.mapper.SysConfigMapper;
import com.yiling.modules.config.service.SysConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public PageResult<SysConfig> listPage(SysConfigDTO q) {
        LambdaQueryWrapper<SysConfig> qw = new LambdaQueryWrapper<>();
        qw.ne(SysConfig::getStatus, 0);
        qw.like(q.getConfigName() != null && !q.getConfigName().isBlank(), SysConfig::getConfigName, q.getConfigName());
        qw.like(q.getConfigKey() != null && !q.getConfigKey().isBlank(), SysConfig::getConfigKey, q.getConfigKey());
        qw.eq(q.getConfigType() != null && !q.getConfigType().isBlank(), SysConfig::getConfigType, q.getConfigType());
        Page<SysConfig> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public SysConfig detailById(Long id) {
        SysConfig c = getById(id);
        if (c == null) throw new BusinessException("参数不存在");
        return c;
    }

    @Override
    public void save(SysConfigDTO dto) {
        SysConfig entity = new SysConfig();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysConfigDTO dto) {
        SysConfig entity = new SysConfig();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysConfig e = new SysConfig();
            e.setId(id);
            e.setStatus(0);
            updateById(e);
        }
    }
}
