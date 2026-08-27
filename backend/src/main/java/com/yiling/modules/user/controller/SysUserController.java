package com.yiling.modules.user.controller;

import com.yiling.common.exception.BusinessException;
import com.yiling.common.result.PageResult;
import com.yiling.common.result.Result;
import com.yiling.modules.user.dto.*;
import com.yiling.modules.user.service.SysUserService;
import com.yiling.modules.user.vo.SysUserDetailVO;
import com.yiling.modules.user.vo.SysUserVO;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("rest/sysUser")
public class SysUserController {

    private final SysUserService service;

    public SysUserController(SysUserService service) {
        this.service = service;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @PostMapping("listPage")
    public Result<PageResult<SysUserVO>> listPage(@RequestBody SysUserDTO query) {
        return Result.success(service.listPage(query));
    }

    @PostMapping("list")
    public Result<List<SysUserVO>> list(@RequestBody SysUserDTO query) {
        return Result.success(service.list(query));
    }

    @PostMapping("detailById")
    public Result<SysUserDetailVO> detailById(@RequestBody Map<String, Object> body) {
        return Result.success(service.detailById(Long.valueOf(body.get("id").toString())));
    }

    @PostMapping("save")
    public Result<Void> save(@RequestBody SysUserDTO dto) {
        service.save(dto);
        return Result.success();
    }

    @PostMapping("update")
    public Result<Void> update(@RequestBody SysUserDTO dto) {
        service.update(dto);
        return Result.success();
    }

    @PostMapping("updateSimple")
    public Result<Void> updateSimple(@RequestBody Map<String, Object> body) {
        service.updateSimple(Long.valueOf(body.get("id").toString()), Integer.valueOf(body.get("status").toString()));
        return Result.success();
    }

    @PostMapping("delete")
    public Result<Void> delete(@RequestBody Map<String, Object> body) {
        service.delete(Long.valueOf(body.get("id").toString()));
        return Result.success();
    }

    @PostMapping("resetPwd")
    public Result<Void> resetPwd(@RequestBody ResetPwdDTO dto) {
        service.resetPwd(dto);
        return Result.success();
    }

    @PostMapping("profile")
    public Result<SysUserDetailVO> profile() {
        return Result.success(service.profile(currentUsername()));
    }

    @PostMapping("updateSelfSimple")
    public Result<Void> updateSelfSimple(@RequestBody UpdateSelfDTO dto) {
        service.updateSelfSimple(currentUsername(), dto);
        return Result.success();
    }

    @PostMapping("updatePwd")
    public Result<Void> updatePwd(@RequestBody UpdatePwdDTO dto) {
        service.updatePwd(currentUsername(), dto);
        return Result.success();
    }

    @PostMapping("avatar")
    public Result<Map<String, String>> avatar(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new BusinessException("请选择头像文件");
        String ext = java.util.Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf('.'));
        String filename = UUID.randomUUID() + ext;
        Path dir = Path.of("uploads", "avatar");
        Files.createDirectories(dir);
        file.transferTo(new File(dir.toFile(), filename));
        String url = "/uploads/avatar/" + filename;
        service.updateAvatar(currentUsername(), url);
        return Result.success(Map.of("url", url));
    }
}
