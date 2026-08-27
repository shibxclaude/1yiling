package com.yiling.modules.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.vo.TreeSelectVO;
import com.yiling.modules.menu.dto.SysMenuDTO;
import com.yiling.modules.menu.entity.SysMenu;
import com.yiling.modules.menu.mapper.SysMenuMapper;
import com.yiling.modules.menu.service.SysMenuService;
import com.yiling.modules.menu.vo.RoleMenuTreeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper mapper;

    public SysMenuServiceImpl(SysMenuMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<SysMenu> list(String menuName, Integer status) {
        LambdaQueryWrapper<SysMenu> qw = new LambdaQueryWrapper<>();
        qw.ne(SysMenu::getStatus, 0);
        qw.like(menuName != null && !menuName.isBlank(), SysMenu::getMenuName, menuName);
        qw.eq(status != null, SysMenu::getStatus, status);
        qw.orderByAsc(SysMenu::getMenuSort);
        return mapper.selectList(qw);
    }

    @Override
    public List<TreeSelectVO> treeSelect() {
        List<SysMenu> all = mapper.selectList(new LambdaQueryWrapper<SysMenu>().ne(SysMenu::getStatus, 0).orderByAsc(SysMenu::getMenuSort));
        return buildTree(0L, all);
    }

    private List<TreeSelectVO> buildTree(Long parentId, List<SysMenu> all) {
        return all.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> new TreeSelectVO(m.getId(), m.getMenuName(), buildTree(m.getId(), all)))
                .collect(Collectors.toList());
    }

    @Override
    public RoleMenuTreeVO roleMenuTreeSelect(Long roleId) {
        List<Long> checked = mapper.selectMenuIdsByRole(roleId);
        return new RoleMenuTreeVO(checked, treeSelect());
    }

    @Override
    public SysMenu detailById(Long id) {
        SysMenu menu = mapper.selectById(id);
        if (menu == null) throw new BusinessException("菜单不存在");
        return menu;
    }

    @Override
    public void save(SysMenuDTO dto) {
        SysMenu entity = new SysMenu();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        normalizeUniqueFields(entity);
        mapper.insert(entity);
    }

    @Override
    public void update(SysMenuDTO dto) {
        SysMenu entity = new SysMenu();
        BeanUtils.copyProperties(dto, entity);
        normalizeUniqueFields(entity);
        mapper.updateById(entity);
    }

    // store NULL instead of "" for unique-indexed columns so multiple buttons with no path don't collide
    private void normalizeUniqueFields(SysMenu entity) {
        if (entity.getMenuPath() != null && entity.getMenuPath().isBlank()) entity.setMenuPath(null);
        if (entity.getMenuComponent() != null && entity.getMenuComponent().isBlank()) entity.setMenuComponent(null);
        if (entity.getPerms() != null && entity.getPerms().isBlank()) entity.setPerms(null);
    }

    @Override
    public void delete(Long id) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (count != null && count > 0) {
            throw new BusinessException("存在子菜单，不允许删除");
        }
        mapper.deleteById(id);
    }
}
