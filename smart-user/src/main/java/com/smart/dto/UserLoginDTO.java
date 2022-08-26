package com.smart.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class UserLoginDTO implements Serializable {
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 验证码
     */
    private String captcha;

    /**
     * 登录类型0账户密码1扫码登录
     */
    private Integer loginType;
}
