package com.yiling.modules.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.notice.dto.SysNoticeDTO;
import com.yiling.modules.notice.entity.SysNotice;
import com.yiling.modules.notice.mapper.SysNoticeMapper;
import com.yiling.modules.notice.service.SysNoticeService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {

    @Override
    public PageResult<SysNotice> listPage(SysNoticeDTO q) {
        LambdaQueryWrapper<SysNotice> qw = new LambdaQueryWrapper<>();
        qw.ne(SysNotice::getStatus, 0);
        qw.like(q.getNoticeTitle() != null && !q.getNoticeTitle().isBlank(), SysNotice::getNoticeTitle, q.getNoticeTitle());
        qw.eq(q.getNoticeType() != null && !q.getNoticeType().isBlank(), SysNotice::getNoticeType, q.getNoticeType());
        qw.orderByDesc(SysNotice::getCreateTime);
        Page<SysNotice> page = new Page<>(q.getPageNo(), q.getPageSize());
        return PageResult.of(page(page, qw));
    }

    @Override
    public SysNotice detailById(Long id) {
        SysNotice n = getById(id);
        if (n == null) throw new BusinessException("通知公告不存在");
        return n;
    }

    @Override
    public void save(SysNoticeDTO dto) {
        SysNotice entity = new SysNotice();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysNoticeDTO dto) {
        SysNotice entity = new SysNotice();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysNotice e = new SysNotice();
            e.setId(id);
            e.setStatus(0);
            updateById(e);
        }
    }
}
