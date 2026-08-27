package com.yiling.modules.auth.service.impl;

import com.yiling.common.exception.BusinessException;
import com.yiling.modules.auth.service.AuthService;
import com.yiling.modules.auth.vo.RouterVO;
import com.yiling.modules.auth.vo.UserInfoVO;
import com.yiling.security.JwtUtil;
import com.yiling.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public String login(String username, String rawPassword) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, passwd, status FROM sys_user WHERE username = ?", username);
        if (rows.isEmpty()) {
            throw new BusinessException("用户名或密码错误");
        }
        Map<String, Object> row = rows.get(0);
        if (((Number) row.get("status")).intValue() != 1) {
            throw new BusinessException("账号已停用或不存在");
        }
        if (!passwordEncoder.matches(rawPassword, (String) row.get("passwd"))) {
            throw new BusinessException("用户名或密码错误");
        }
        Long userId = ((Number) row.get("id")).longValue();
        return jwtUtil.generateToken(userId, username);
    }

    @Override
    public LoginUser loadLoginUser(Long userId) {
        List<Map<String, Object>> userRows = jdbcTemplate.queryForList(
                "SELECT username FROM sys_user WHERE id = ? AND status = 1", userId);
        if (userRows.isEmpty()) {
            throw new BusinessException("用户不存在或已停用");
        }
        String username = (String) userRows.get(0).get("username");
        List<String> roleKeys = jdbcTemplate.queryForList(
                "SELECT r.role_key FROM sys_role r JOIN sys_user_role ur ON ur.role_id = r.id " +
                        "WHERE ur.user_id = ? AND r.status = 1", String.class, userId);
        List<String> perms = jdbcTemplate.queryForList(
                "SELECT DISTINCT m.perms FROM sys_menu m JOIN sys_role_menu rm ON rm.menu_id = m.id " +
                        "JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
                        "WHERE ur.user_id = ? AND m.status = 1 AND m.perms IS NOT NULL", String.class, userId);
        return new LoginUser(userId, username, roleKeys, perms);
    }

    @Override
    public UserInfoVO getInfo(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username, nick_name, dept_id, avatar FROM sys_user WHERE username = ?", username);
        if (rows.isEmpty()) {
            throw new BusinessException("用户不存在");
        }
        Map<String, Object> row = rows.get(0);
        Long userId = ((Number) row.get("id")).longValue();

        UserInfoVO vo = new UserInfoVO();
        UserInfoVO.UserBrief brief = new UserInfoVO.UserBrief();
        brief.setId(userId);
        brief.setUsername((String) row.get("username"));
        brief.setNickName((String) row.get("nick_name"));
        brief.setDeptId(row.get("dept_id") != null ? ((Number) row.get("dept_id")).longValue() : null);
        brief.setAvatar((String) row.get("avatar"));
        vo.setUser(brief);

        LoginUser lu = loadLoginUser(userId);
        vo.setRoles(lu.getRoleKeys());
        vo.setPermissions(lu.getPermissions());
        return vo;
    }

    @Override
    public List<RouterVO> getRouters(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username FROM sys_user WHERE username = ?", username);
        Long userId = ((Number) rows.get(0).get("id")).longValue();

        List<Map<String, Object>> menus = jdbcTemplate.queryForList(
                "SELECT DISTINCT m.id, m.parent_id, m.menu_name, m.menu_type, m.menu_sort, m.icon, " +
                        "m.menu_path, m.menu_component, m.status " +
                        "FROM sys_menu m JOIN sys_role_menu rm ON rm.menu_id = m.id " +
                        "JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
                        "WHERE ur.user_id = ? AND m.menu_type IN ('0','1') AND m.status != 0 " +
                        "ORDER BY m.menu_sort", userId);

        Map<Long, List<Map<String, Object>>> byParent = new HashMap<>();
        for (Map<String, Object> m : menus) {
            long parentId = ((Number) m.get("parent_id")).longValue();
            byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(m);
        }
        return buildTree(0L, byParent);
    }

    private List<RouterVO> buildTree(Long parentId, Map<Long, List<Map<String, Object>>> byParent) {
        List<Map<String, Object>> children = byParent.getOrDefault(parentId, List.of());
        return children.stream().map(m -> {
            RouterVO vo = new RouterVO();
            vo.setName((String) m.get("menu_name"));
            vo.setPath((String) m.get("menu_path"));
            vo.setComponent((String) m.get("menu_component"));
            vo.setHidden("2".equals(String.valueOf(m.get("status"))));
            vo.setMeta(new RouterVO.Meta((String) m.get("menu_name"), (String) m.get("icon"), false));
            long id = ((Number) m.get("id")).longValue();
            List<RouterVO> children2 = buildTree(id, byParent);
            vo.setChildren(children2.isEmpty() ? null : children2);
            return vo;
        }).collect(Collectors.toList());
    }
}
