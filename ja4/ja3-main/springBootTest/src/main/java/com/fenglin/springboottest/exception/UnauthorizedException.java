package com.fenglin.springboottest.exception;

/**
 * 未认证/令牌失效：统一以 HTTP 401 + {message:"..."} 返回
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
