package com.fenglin.springboottest.config;

import com.fenglin.springboottest.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

/**
 * Authorization 头解析工具：把「Bearer 令牌」变成用户 id。
 *
 * <p>原来 AuthController / FavoriteController / ArticleController 各自复制了一份
 * 解析逻辑（三段 if + tryGetUserId），行为容易漂移。收敛到这里统一维护：</p>
 * <ul>
 *   <li>{@link #requireUserId(String)} —— 必须登录，缺失 / 格式错 / 验签失败一律 401</li>
 *   <li>{@link #optionalUserId(String)} —— 可选登录，拿不到就返回 null，不拦截请求</li>
 * </ul>
 */
@Component
public class AuthSupport {

    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public AuthSupport(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /** 必须登录：令牌缺失 / 格式错误 / 验签失败 / 过期，全部抛 401 */
    public Long requireUserId(String authorizationHeader) {
        String token = extract(authorizationHeader);
        if (token == null) {
            throw new UnauthorizedException("请先登录");
        }
        Long uid = jwtUtil.tryGetUserId(token);
        if (uid == null) {
            throw new UnauthorizedException("登录已过期，请重新登录");
        }
        return uid;
    }

    /** 可选登录：没有令牌或令牌无效都返回 null，请求照常继续 */
    public Long optionalUserId(String authorizationHeader) {
        String token = extract(authorizationHeader);
        return token == null ? null : jwtUtil.tryGetUserId(token);
    }

    /** 取出 Bearer 后面的 token；头为空 / 不以 Bearer 开头返回 null */
    private String extract(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) {
            return null;
        }
        String token = authorizationHeader.substring(PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
