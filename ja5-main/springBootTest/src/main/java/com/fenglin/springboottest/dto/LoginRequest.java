package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求（昵称 + 密码，remember 由前端用于决定 token 是否本地持久化）
 */
public class LoginRequest {

    @NotBlank(message = "请输入昵称")
    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String name;

    @NotBlank(message = "请输入密码")
    @Size(max = 64, message = "密码长度不能超过 64 位")
    private String password;

    private Boolean remember;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getRemember() {
        return remember;
    }

    public void setRemember(Boolean remember) {
        this.remember = remember;
    }
}
