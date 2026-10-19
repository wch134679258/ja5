package com.fenglin.springboottest.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 工具：签发与校验（HS384）。
 * 演示用固定密钥，生产环境请改为强随机密钥并通过环境变量注入，且定期轮换。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-ms:86400000}") long expireMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expireMs = expireMs;
    }

    /** 签发令牌：subject 存邮箱，claim 存 userId */
    public String createToken(Long userId, String email) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(key, Jwts.SIG.HS384)
                .compact();
    }

    public Claims parse(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("令牌为空");
        }
        // 严格校验结构：必须正好 3 段
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("令牌格式非法");
        }
        // 严格 base64url 解码签名段：拒绝尾部多余/不完整字符（防验签绕过）。
        // 注意：HS384 签名固定 48 字节；这里只做“严格解码”即可拦截追加/截断类攻击，
        // 不应拿它与密钥长度比较（密钥可长于签名）。
        try {
            Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("签名编码非法");
        }
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        return parse(token).get("uid", Long.class);
    }
}
