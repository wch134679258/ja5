package com.fenglin.springboottest.exception;

/**
 * 业务异常：统一以 HTTP 400 + {message:"..."} 返回给前端
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
