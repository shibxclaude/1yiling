package com.yiling.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.modules.user.dto.*;
import com.yiling.modules.user.entity.SysUser;
import com.yiling.modules.user.mapper.SysUserMapper;
import com.yiling.modules.user.mapper.SysUserPostMapper;
import com.yiling.modules.user.mapper.SysUserRoleMapper;
import com.yiling.modules.user.service.SysUserService;
import com.yiling.modules.user.vo.SysUserDetailVO;
import com.yiling.modules.user.vo.SysUserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUserServiceImpl(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper,
                               SysUserPostMapper userPostMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.userPostMapper = userPostMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<SysUserVO> listPage(SysUserDTO query) {
        List<SysUserVO> all = userMapper.selectUserVOList(query.getDeptId(), query.getUsername(), query.getPhone(), query.getStatus());
        int pageNo = query.getPageNo() == null ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        int from = Math.min((pageNo - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        PageResult<SysUserVO> result = new PageResult<>();
        result.setRecords(all.subList(from, to));
        result.setTotal(all.size());
        result.setSize(pageSize);
        result.setCurrent(pageNo);
        result.setPages((long) Math.ceil((double) all.size() / pageSize));
        return result;
    }

    @Override
    public List<SysUserVO> list(SysUserDTO query) {
        return userMapper.selectUserVOList(query.getDeptId(), query.getUsername(), query.getPhone(), query.getStatus());
    }

    @Override
    public SysUserDetailVO detailById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        SysUserDetailVO vo = new SysUserDetailVO();
        BeanUtils.copyProperties(user, vo);
        vo.setPasswd(null);
        vo.setRoleIds(userRoleMapper.selectRoleIdsByUser(id));
        vo.setPostIds(userPostMapper.selectPostIdsByUser(id));
        return vo;
    }

    @Override
    @Transactional
    public void save(SysUserDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) throw new BusinessException("用户名称不能为空");
        if (dto.getPasswd() == null || dto.getPasswd().isBlank()) throw new BusinessException("密码不能为空");
        Long existing = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (existing != null && existing > 0) throw new BusinessException("用户名称已存在");

        SysUser entity = new SysUser();
        BeanUtils.copyProperties(dto, entity);
        entity.setId(null);
        entity.setPasswd(passwordEncoder.encode(dto.getPasswd()));
        userMapper.insert(entity);
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) userRoleMapper.batchInsert(entity.getId(), dto.getRoleIds());
        if (dto.getPostIds() != null && !dto.getPostIds().isEmpty()) userPostMapper.batchInsert(entity.getId(), dto.getPostIds());
    }

    @Override
    @Transactional
    public void update(SysUserDTO dto) {
        SysUser entity = new SysUser();
        BeanUtils.copyProperties(dto, entity);
        entity.setPasswd(null); // password unchanged via this endpoint
        userMapper.updateById(entity);
        userRoleMapper.deleteByUserId(dto.getId());
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) userRoleMapper.batchInsert(dto.getId(), dto.getRoleIds());
        userPostMapper.deleteByUserId(dto.getId());
        if (dto.getPostIds() != null && !dto.getPostIds().isEmpty()) userPostMapper.batchInsert(dto.getId(), dto.getPostIds());
    }

    @Override
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if ("admin".equals(user.getUsername())) throw new BusinessException("超级管理员账号不允许删除");
        user.setStatus(0);
        userMapper.updateById(user);
    }

    @Override
    public void updateSimple(Long id, Integer status) {
        SysUser entity = new SysUser();
        entity.setId(id);
        entity.setStatus(status);
        userMapper.updateById(entity);
    }

    @Override
    public void resetPwd(ResetPwdDTO dto) {
        if (dto.getPasswd() == null || dto.getPasswd().length() < 5 || dto.getPasswd().length() > 20) {
            throw new BusinessException("密码长度必须在5-20位之间");
        }
        SysUser entity = new SysUser();
        entity.setId(dto.getId());
        entity.setPasswd(passwordEncoder.encode(dto.getPasswd()));
        userMapper.updateById(entity);
    }

    @Override
    public SysUserDetailVO profile(String username) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) throw new BusinessException("用户不存在");
        return detailById(user.getId());
    }

    @Override
    public void updateSelfSimple(String username, UpdateSelfDTO dto) {
        if (dto.getNickName() == null || dto.getNickName().isBlank()) throw new BusinessException("昵称不能为空");
        if (dto.getEmail() == null || dto.getEmail().isBlank()) throw new BusinessException("邮箱不能为空");
        if (dto.getPhone() == null || dto.getPhone().isBlank()) throw new BusinessException("手机号不能为空");
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        SysUser entity = new SysUser();
        entity.setId(user.getId());
        entity.setNickName(dto.getNickName());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setSex(dto.getSex());
        userMapper.updateById(entity);
    }

    @Override
    public void updatePwd(String username, UpdatePwdDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswd())) throw new BusinessException("原密码不正确");
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 5 || dto.getNewPassword().length() > 20) {
            throw new BusinessException("密码长度必须在5-20位之间");
        }
        SysUser entity = new SysUser();
        entity.setId(user.getId());
        entity.setPasswd(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(entity);
    }

    @Override
    public String updateAvatar(String username, String avatarUrl) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        SysUser entity = new SysUser();
        entity.setId(user.getId());
        entity.setAvatar(avatarUrl);
        userMapper.updateById(entity);
        return avatarUrl;
    }
}
