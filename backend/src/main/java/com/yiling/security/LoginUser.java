package com.yiling.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginUser {
    private Long userId;
    private String username;
    private List<String> roleKeys;
    private List<String> permissions;
}
