package com.example.studywithme.service;

import com.example.studywithme.entity.User;
import com.example.studywithme.entity.UserActivity;
import com.example.studywithme.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;

    @Transactional
    public void logSearch(User user, String keyword) {
        if (user == null || keyword == null || keyword.trim().isEmpty()) return;
        UserActivity ua = new UserActivity();
        ua.setUser(user);
        ua.setRealName(user.getRealName());
        ua.setActionType(UserActivity.ActionType.SEARCH);
        ua.setTargetKeyword(keyword.trim());
        userActivityRepository.save(ua);
    }

    @Transactional
    public void logViewPost(User user, Long postId, String title, String tags) {
        if (user == null || postId == null) return;
        UserActivity ua = new UserActivity();
        ua.setUser(user);
        ua.setRealName(user.getRealName());
        ua.setActionType(UserActivity.ActionType.CLICK);
        ua.setTargetId(postId);
        ua.setTargetKeyword(tags);
        ua.setActionDetail(title);
        userActivityRepository.save(ua);
    }

    @Transactional
    public void logLikePost(User user, Long postId) {
        if (user == null || postId == null) return;
        UserActivity ua = new UserActivity();
        ua.setUser(user);
        ua.setRealName(user.getRealName());
        ua.setActionType(UserActivity.ActionType.LIKE);
        ua.setTargetId(postId);
        userActivityRepository.save(ua);
    }
}


