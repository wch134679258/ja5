package com.fenglin.springboottest.dto;

/**
 * 注册请求（图形验证码方式）
 */
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String captchaId;   // 图形验证码 id（来自 /api/auth/captcha）
    private String captcha;     // 用户输入的验证码文本

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
