package com.yiling.modules.user.dto;

import lombok.Data;

@Data
public class UpdateSelfDTO {
    private String nickName;
    private String email;
    private String phone;
    private String sex;
}
