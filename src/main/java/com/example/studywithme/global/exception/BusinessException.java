package com.example.studywithme.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 도메인/비즈니스 규칙 위반 시 사용하는 기본 예외. HTTP 상태와 메시지를 함께 둡니다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public BusinessException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
