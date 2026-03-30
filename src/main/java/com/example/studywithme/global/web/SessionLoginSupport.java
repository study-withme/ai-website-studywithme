package com.example.studywithme.global.web;

import com.example.studywithme.global.exception.UnauthorizedException;
import com.example.studywithme.user.entity.User;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

/**
 * 세션 기반 로그인 사용자 조회 공통 유틸.
 */
public final class SessionLoginSupport {

    public static final String SESSION_LOGIN_USER = "loginUser";

    private SessionLoginSupport() {
    }

    public static Optional<User> currentUser(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object attr = session.getAttribute(SESSION_LOGIN_USER);
        if (attr instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static User requireUser(HttpSession session) {
        return currentUser(session)
                .orElseThrow(() -> new UnauthorizedException("로그인이 필요합니다."));
    }
}
