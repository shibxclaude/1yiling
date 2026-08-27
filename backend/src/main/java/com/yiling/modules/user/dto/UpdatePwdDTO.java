package com.yiling.modules.user.dto;

import lombok.Data;

@Data
public class UpdatePwdDTO {
    private String oldPassword;
    private String newPassword;
}
