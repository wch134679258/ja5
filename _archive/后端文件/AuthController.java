package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.JwtUtil;
import com.fenglin.springboottest.dto.LoginRequest;
import com.fenglin.springboottest.dto.RegisterRequest;
import com.fenglin.springboottest.dto.SendCodeRequest;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.exception.BusinessException;
import com.fenglin.springboottest.exception.UnauthorizedException;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证接口：对接 aurora-vue-2 注册/登录页
 *   POST /api/auth/send-code  {email}            -> {code:"OK", expiresIn, devCode?}
 *   POST /api/auth/register    {name,email,password,code} -> {token, userId}
 *   POST /api/auth/login       {email,password,remember}  -> {token, userId}
 *   GET  /api/auth/me          Authorization: Bearer <token> -> {id, name, email}
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, JwtUtil jwtUtil, UserMapper userMapper) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody SendCodeRequest req) {
        return authService.sendCode(req.getEmail());
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /**
     * 受保护接口：返回当前登录用户信息。令牌非法/过期/缺失均返回 401。
     */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        String token = auth.substring(7).trim();
        Long uid;
        try {
            uid = jwtUtil.getUserId(token);
        } catch (Exception e) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new UnauthorizedException("用户不存在或已被注销");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getName());
        result.put("email", user.getEmail());
        return result;
    }
}
