package com.example.studywithme.service;

import com.example.studywithme.repository.BookmarkRepository;
import com.example.studywithme.repository.PostApplicationRepository;
import com.example.studywithme.repository.PostLikeRepository;
import com.example.studywithme.repository.PostRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PostApplicationRepository postApplicationRepository;

    /**
     * 사용자 활동 통계를 조회합니다.
     * 게시글 수, 받은 좋아요 수, 좋아요 한 횟수, 북마크 수, 지원 횟수 등을 기반으로
     * 간단한 레벨/경험치를 계산합니다.
     */
    public UserStats getUserStats(Integer userId) {
        long postCount = postRepository.countByUser_Id(userId);
        long likesGiven = postLikeRepository.countByUserId(userId);
        long likesReceived = postRepository.sumLikeCountByUserId(userId);
        long bookmarks = bookmarkRepository.countByUser_Id(userId);
        long applications = postApplicationRepository.countByUser_Id(userId);

        // 간단한 활동 점수 계산식 (운영 단계에서 조정 가능)
        long activityScore =
                postCount * 5L +
                likesGiven * 2L +
                likesReceived * 3L +
                bookmarks +
                applications * 4L;

        int level = (int) (activityScore / 50L) + 1; // 50점 당 레벨 1 상승
        long remainder = activityScore % 50L;
        int expPercent = (int) Math.min(100, Math.round((remainder / 50.0) * 100));

        UserStats stats = new UserStats();
        stats.postCount = postCount;
        stats.likesGiven = likesGiven;
        stats.likesReceived = likesReceived;
        stats.bookmarks = bookmarks;
        stats.applications = applications;
        stats.level = level;
        stats.expPercent = expPercent;
        stats.activityScore = activityScore;
        return stats;
    }

    @Getter
    public static class UserStats {
        private long postCount;
        private long likesGiven;
        private long likesReceived;
        private long bookmarks;
        private long applications;

        private int level;
        private int expPercent;
        private long activityScore;
    }
}


