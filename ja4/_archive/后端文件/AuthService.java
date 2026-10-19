package com.fenglin.springboottest.service;

import com.fenglin.springboottest.dto.LoginRequest;
import com.fenglin.springboottest.dto.RegisterRequest;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.entity.VerifyCode;
import com.fenglin.springboottest.exception.BusinessException;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.mapper.VerifyCodeMapper;
import com.fenglin.springboottest.config.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 注册 / 登录 业务
 */
@Service
public class AuthService {

    private static final Pattern RE_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[a-zA-Z]{2,}$");
    private static final int CODE_TTL_SECONDS = 300;

    private final UserMapper userMapper;
    private final VerifyCodeMapper verifyCodeMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${app.auth.return-dev-code:true}")
    private boolean returnDevCode;

    public AuthService(UserMapper userMapper, VerifyCodeMapper verifyCodeMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.verifyCodeMapper = verifyCodeMapper;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 发送邮箱验证码（注册场景）。本地演示直接返回 devCode，生产应改为邮件下发且不回传明文。
     */
    @Transactional
    public Map<String, Object> sendCode(String email) {
        if (email == null || !RE_EMAIL.matcher(email.trim()).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        String code = String.format("%06d", (int) (Math.random() * 1_000_000));
        VerifyCode vc = new VerifyCode(email.trim(), code, "register",
                LocalDateTime.now().plusSeconds(CODE_TTL_SECONDS));
        verifyCodeMapper.insert(vc);

        Map<String, Object> result = new HashMap<>();
        result.put("code", "OK");
        result.put("expiresIn", CODE_TTL_SECONDS);
        if (returnDevCode) {
            result.put("devCode", code);
        }
        return result;
    }

    /**
     * 注册：校验验证码 -> 校验邮箱唯一 -> BCrypt 加密密码 -> 落库 -> 返回 token
     */
    @Transactional
    public Map<String, String> register(RegisterRequest req) {
        String name = req.getName() == null ? "" : req.getName().trim();
        String email = req.getEmail() == null ? "" : req.getEmail().trim();
        String password = req.getPassword() == null ? "" : req.getPassword();
        String code = req.getCode() == null ? "" : req.getCode().trim();

        if (name.length() < 2) {
            throw new BusinessException("昵称至少 2 个字符");
        }
        if (!RE_EMAIL.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        boolean pwdOk = password.length() >= 8
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*");
        if (!pwdOk) {
            throw new BusinessException("密码至少 8 位，且需包含字母和数字");
        }
        if (code.isEmpty()) {
            throw new BusinessException("请输入验证码");
        }

        Optional<VerifyCode> opt = verifyCodeMapper.findLatestValid(email, "register");
        if (opt.isEmpty()) {
            throw new BusinessException("验证码无效或已过期，请重新获取");
        }
        VerifyCode vc = opt.get();
        if (!vc.getCode().equals(code)) {
            throw new BusinessException("验证码不正确");
        }
        vc.setUsed(1);
        verifyCodeMapper.updateById(vc);

        if (userMapper.findByEmail(email).isPresent()) {
            throw new BusinessException("该邮箱已注册，请直接登录");
        }

        User user = new User(name, email, encoder.encode(password));
        userMapper.insert(user);

        return tokenResult(user.getId(), email);
    }

    /**
     * 登录：校验邮箱 + 密码 -> 返回 token
     */
    @Transactional(readOnly = true)
    public Map<String, String> login(LoginRequest req) {
        String email = req.getEmail() == null ? "" : req.getEmail().trim();
        String password = req.getPassword() == null ? "" : req.getPassword();

        User user = userMapper.findByEmail(email)
                .orElseThrow(() -> new BusinessException("邮箱或密码错误"));
        if (!encoder.matches(password, user.getPassword())) {
            throw new BusinessException("邮箱或密码错误");
        }
        return tokenResult(user.getId(), email);
    }

    /** 签发 JWT 令牌并返回 userId */
    private Map<String, String> tokenResult(Long userId, String email) {
        Map<String, String> result = new HashMap<>();
        result.put("token", jwtUtil.createToken(userId, email));
        result.put("userId", String.valueOf(userId));
        return result;
    }
}
