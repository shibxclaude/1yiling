package com.yiling.modules.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.role.dto.DataScopeDTO;
import com.yiling.modules.role.dto.SysRoleDTO;
import com.yiling.modules.role.entity.SysRole;
import com.yiling.modules.role.mapper.SysRoleMapper;
import com.yiling.modules.role.mapper.SysRoleMenuMapper;
import com.yiling.modules.role.service.SysRoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public SysRoleServiceImpl(SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Override
    public PageResult<SysRole> listPage(SysRoleDTO query) {
        LambdaQueryWrapper<SysRole> qw = new LambdaQueryWrapper<>();
        qw.ne(SysRole::getStatus, 0);
        qw.like(query.getRoleName() != null && !query.getRoleName().isBlank(), SysRole::getRoleName, query.getRoleName());
        qw.like(query.getRoleKey() != null && !query.getRoleKey().isBlank(), SysRole::getRoleKey, query.getRoleKey());
        qw.eq(query.getStatus() != null, SysRole::getStatus, query.getStatus());
        qw.orderByAsc(SysRole::getRoleSort);
        Page<SysRole> page = new Page<>(query.getPageNo(), query.getPageSize());
        return PageResult.of(roleMapper.selectPage(page, qw));
    }

    @Override
    public SysRole detailById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        return role;
    }

    @Override
    @Transactional
    public void save(SysRoleDTO dto) {
        SysRole entity = new SysRole();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        entity.setDataScope("1");
        entity.setMenuCheckStrictly(true);
        entity.setDeptCheckStrictly(true);
        roleMapper.insert(entity);
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            roleMenuMapper.batchInsert(entity.getId(), dto.getMenuIds());
        }
    }

    @Override
    @Transactional
    public void update(SysRoleDTO dto) {
        if ("admin".equals(roleMapper.selectById(dto.getId()).getRoleKey())) {
            throw new BusinessException("超级管理员角色不允许修改菜单权限之外的关键属性");
        }
        SysRole entity = new SysRole();
        BeanUtils.copyProperties(dto, entity);
        roleMapper.updateById(entity);
        roleMenuMapper.deleteByRoleId(dto.getId());
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            roleMenuMapper.batchInsert(dto.getId(), dto.getMenuIds());
        }
    }

    @Override
    public void delete(Long id) {
        SysRole role = detailById(id);
        if ("admin".equals(role.getRoleKey())) {
            throw new BusinessException("超级管理员角色不允许删除");
        }
        role.setStatus(0);
        roleMapper.updateById(role);
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysRole entity = new SysRole();
        entity.setId(id);
        entity.setStatus(status);
        roleMapper.updateById(entity);
    }

    @Override
    public void saveDataScope(DataScopeDTO dto) {
        SysRole entity = new SysRole();
        entity.setId(dto.getId());
        entity.setDataScope(dto.getDataScope());
        if ("2".equals(dto.getDataScope()) && dto.getDeptIdList() != null) {
            entity.setDeptIds(String.join(",", dto.getDeptIdList().stream().map(String::valueOf).toList()));
        } else {
            entity.setDeptIds(null);
        }
        roleMapper.updateById(entity);
    }
}
