package com.fenglin.springboottest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求（图形验证码方式）
 *
 * <p>用 jakarta.validation 注解做前置校验：Controller 加 @Valid 后，
 * 不合法请求会在进入 Service 前被拦下，由 GlobalExceptionHandler 统一转成
 * {@code 400 {message}}。Service 内部仍保留同样的兜底判断，防止其它入口绕过。</p>
 */
public class RegisterRequest {

    @NotBlank(message = "请输入昵称")
    @Size(min = 2, max = 32, message = "昵称长度需在 2-32 个字符之间")
    private String name;

    @NotBlank(message = "请输入密码")
    @Size(min = 8, max = 64, message = "密码长度需在 8-64 位之间")
    // 至少一位字母 + 至少一位数字
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需同时包含字母和数字")
    private String password;

    private String captchaId;   // 图形验证码 id（来自 /api/auth/captcha）

    @NotBlank(message = "请输入图形验证码")
    private String captcha;     // 用户输入的验证码文本

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

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }
}
