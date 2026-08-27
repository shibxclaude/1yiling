package com.yiling.modules.auth.service;

import com.yiling.modules.auth.vo.RouterVO;
import com.yiling.modules.auth.vo.UserInfoVO;
import com.yiling.security.LoginUser;

import java.util.List;

public interface AuthService {
    String login(String username, String rawPassword);
    LoginUser loadLoginUser(Long userId);
    UserInfoVO getInfo(String username);
    List<RouterVO> getRouters(String username);
}
