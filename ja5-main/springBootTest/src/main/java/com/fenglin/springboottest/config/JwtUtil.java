package com.fenglin.springboottest.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 工具：签发与校验（HS384）。
 *
 * <p>演示用固定密钥，生产环境请改为强随机密钥（≥48 字节，建议 64 字节随机串）
 * 并通过环境变量注入，且定期轮换。</p>
 *
 * <p>实现要点：</p>
 * <ul>
 *   <li>密钥字节固定按 UTF-8 编码：String#getBytes() 用的是平台默认编码，
 *       同一密钥在不同 locale（中文 Windows 的 GBK vs Linux 的 UTF-8）下
 *       会得到不同字节，导致签名互不相认——换成 UTF-8 消除这个隐患。</li>
 *   <li>JwtParser 实例缓存复用：它是线程安全的，每次 parse 都重建纯属浪费。</li>
 *   <li>密钥长度提前校验：HS384 要求 ≥384 位（48 字节），密钥太短时
 *       Keys.hmacShaKeyFor 抛的 WeakKeyException 信息不直观，这里先给一句人话。</li>
 * </ul>
 */
@Component
public class JwtUtil {

    /** HS384 算法要求的最小密钥长度（字节） */
    private static final int MIN_KEY_BYTES = 48;

    private final SecretKey key;
    private final long expireMs;
    private final JwtParser parser;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-ms:86400000}") long expireMs) {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(String.format(
                    "JWT 密钥长度不足：HS384 要求至少 %d 字节，当前只有 %d 字节。"
                            + "请把 app.jwt.secret 换成更长的强随机串。",
                    MIN_KEY_BYTES, keyBytes.length));
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expireMs = expireMs;
        // parser 构建后即可复用（线程安全）
        this.parser = Jwts.parser().verifyWith(key).build();
    }

    /** 签发令牌：subject 存昵称，claim 存 userId */
    public String createToken(Long userId, String name) {
        Date now = new Date();
        return Jwts.builder()
                .subject(name)
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
        return parser.parseSignedClaims(token).getPayload();
    }

    public Long getUserId(String token) {
        return parse(token).get("uid", Long.class);
    }

    /**
     * 尝试解析令牌，失败返回 null。
     * 用于「可选登录」的公开接口（如文章详情：登录了多返回收藏态，未登录照常访问）。
     */
    public Long tryGetUserId(String token) {
        try {
            return getUserId(token);
        } catch (Exception e) {
            return null;
        }
    }
}
