package com.yiling.modules.auth.controller;

import com.yiling.common.result.Result;
import com.yiling.modules.auth.dto.LoginDTO;
import com.yiling.modules.auth.service.AuthService;
import com.yiling.modules.auth.vo.LoginVO;
import com.yiling.modules.auth.vo.RouterVO;
import com.yiling.modules.auth.vo.UserInfoVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        String token = authService.login(dto.getUsername(), dto.getPasswd());
        return Result.success(new LoginVO(token));
    }

    @GetMapping("/getInfo")
    public Result<UserInfoVO> getInfo() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Result.success(authService.getInfo(username));
    }

    @GetMapping("/getRouters")
    public Result<List<RouterVO>> getRouters() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Result.success(authService.getRouters(username));
    }
}
