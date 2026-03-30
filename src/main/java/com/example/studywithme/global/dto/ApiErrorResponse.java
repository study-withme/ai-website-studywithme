package com.example.studywithme.global.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * REST API 공통 오류 본문.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiErrorResponse of(HttpStatus httpStatus, String message, String path) {
        return new ApiErrorResponse(
                Instant.now(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                message,
                path
        );
    }
}
