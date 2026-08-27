package com.yiling.modules.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.post.dto.SysPostDTO;
import com.yiling.modules.post.entity.SysPost;
import com.yiling.modules.post.mapper.SysPostMapper;
import com.yiling.modules.post.service.SysPostService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysPostServiceImpl extends ServiceImpl<SysPostMapper, SysPost> implements SysPostService {

    private LambdaQueryWrapper<SysPost> baseQuery(SysPostDTO q) {
        LambdaQueryWrapper<SysPost> qw = new LambdaQueryWrapper<>();
        qw.ne(SysPost::getStatus, 0);
        qw.like(q.getPostCode() != null && !q.getPostCode().isBlank(), SysPost::getPostCode, q.getPostCode());
        qw.like(q.getPostName() != null && !q.getPostName().isBlank(), SysPost::getPostName, q.getPostName());
        qw.eq(q.getStatus() != null, SysPost::getStatus, q.getStatus());
        qw.orderByAsc(SysPost::getPostSort);
        return qw;
    }

    @Override
    public PageResult<SysPost> listPage(SysPostDTO query) {
        Page<SysPost> page = new Page<>(query.getPageNo(), query.getPageSize());
        return PageResult.of(page(page, baseQuery(query)));
    }

    @Override
    public List<SysPost> list(SysPostDTO query) {
        return list(baseQuery(query));
    }

    @Override
    public SysPost detailById(Long id) {
        SysPost post = getById(id);
        if (post == null) throw new BusinessException("岗位不存在");
        return post;
    }

    @Override
    public void save(SysPostDTO dto) {
        SysPost entity = new SysPost();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        save(entity);
    }

    @Override
    public void update(SysPostDTO dto) {
        SysPost entity = new SysPost();
        BeanUtils.copyProperties(dto, entity);
        updateById(entity);
    }

    @Override
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysPost entity = new SysPost();
            entity.setId(id);
            entity.setStatus(0);
            updateById(entity);
        }
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysPost entity = new SysPost();
        entity.setId(id);
        entity.setStatus(status);
        updateById(entity);
    }
}
