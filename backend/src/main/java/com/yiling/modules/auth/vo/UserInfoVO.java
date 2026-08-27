package com.yiling.modules.auth.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {
    private UserBrief user;
    private List<String> roles;
    private List<String> permissions;

    @Data
    public static class UserBrief {
        private Long id;
        private String username;
        private String nickName;
        private Long deptId;
        private String avatar;
    }
}
