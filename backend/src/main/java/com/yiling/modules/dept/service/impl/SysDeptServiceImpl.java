package com.yiling.modules.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.dept.dto.SysDeptDTO;
import com.yiling.modules.dept.entity.SysDept;
import com.yiling.modules.dept.mapper.SysDeptMapper;
import com.yiling.modules.dept.service.SysDeptService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper mapper;

    public SysDeptServiceImpl(SysDeptMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<SysDept> list(String deptName, Integer status) {
        LambdaQueryWrapper<SysDept> qw = new LambdaQueryWrapper<>();
        qw.ne(SysDept::getStatus, 0);
        qw.like(deptName != null && !deptName.isBlank(), SysDept::getDeptName, deptName);
        qw.eq(status != null, SysDept::getStatus, status);
        qw.orderByAsc(SysDept::getOrderNum);
        return mapper.selectList(qw);
    }

    @Override
    public List<TreeSelectVO> deptTree() {
        List<SysDept> all = list(null, null);
        return buildTree(0L, all);
    }

    private List<TreeSelectVO> buildTree(Long parentId, List<SysDept> all) {
        return all.stream()
                .filter(d -> parentId.equals(d.getParentId()))
                .map(d -> new TreeSelectVO(d.getId(), d.getDeptName(), buildTree(d.getId(), all)))
                .collect(Collectors.toList());
    }

    @Override
    public SysDept detailById(Long id) {
        SysDept dept = mapper.selectById(id);
        if (dept == null) throw new BusinessException("部门不存在");
        return dept;
    }

    @Override
    public void save(SysDeptDTO dto) {
        SysDept entity = new SysDept();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        entity.setAncestors(buildAncestors(dto.getParentId()));
        mapper.insert(entity);
    }

    @Override
    public void update(SysDeptDTO dto) {
        SysDept entity = new SysDept();
        BeanUtils.copyProperties(dto, entity);
        entity.setAncestors(buildAncestors(dto.getParentId()));
        mapper.updateById(entity);
    }

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0) return "0";
        SysDept parent = mapper.selectById(parentId);
        if (parent == null) return "0";
        return parent.getAncestors() + "," + parent.getId();
    }

    @Override
    public void delete(Long id) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (count != null && count > 0) {
            throw new BusinessException("存在下级部门，不允许删除");
        }
        mapper.deleteById(id);
    }
}
