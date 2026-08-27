package com.yiling.modules.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.log.dto.SysOperLogDTO;
import com.yiling.modules.log.entity.SysOperLog;
import com.yiling.modules.log.mapper.SysOperLogMapper;
import com.yiling.modules.log.service.SysOperLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Override
    public PageResult<SysOperLog> listPage(SysOperLogDTO q) {
        LambdaQueryWrapper<SysOperLog> qw = new LambdaQueryWrapper<>();
        qw.like(q.getTitle() != null && !q.getTitle().isBlank(), SysOperLog::getTitle, q.getTitle());
        qw.eq(q.getBusinessType() != null, SysOperLog::getBusinessType, q.getBusinessType());
        qw.like(q.getOperName() != null && !q.getOperName().isBlank(), SysOperLog::getOperName, q.getOperName());
        qw.like(q.getOperIp() != null && !q.getOperIp().isBlank(), SysOperLog::getOperIp, q.getOperIp());
        qw.orderByDesc(SysOperLog::getOperTime);
        Page<SysOperLog> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public SysOperLog detailById(Long id) {
        SysOperLog log = getById(id);
        if (log == null) throw new BusinessException("日志不存在");
        return log;
    }

    @Override
    public void delete(List<Long> ids) {
        removeByIds(ids);
    }
}
