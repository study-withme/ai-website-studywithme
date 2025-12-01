package com.example.studywithme.service;

import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.PostApplication;
import com.example.studywithme.entity.StudyGroup;
import com.example.studywithme.entity.User;
import com.example.studywithme.repository.PostApplicationRepository;
import com.example.studywithme.repository.PostRepository;
import com.example.studywithme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostApplicationService {

    private final PostApplicationRepository applicationRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final StudyGroupService studyGroupService;

    @Transactional
    public PostApplication applyToPost(Integer userId, Long postId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 이미 지원했는지 확인 (PENDING 또는 ACCEPTED 상태만 체크, CANCELLED는 재지원 가능)
        Optional<PostApplication> existingApp = applicationRepository.findByPost_IdAndUser_Id(postId, userId);
        if (existingApp.isPresent()) {
            PostApplication existing = existingApp.get();
            // PENDING 또는 ACCEPTED 상태면 재지원 불가
            if (existing.getStatus() == PostApplication.ApplicationStatus.PENDING ||
                existing.getStatus() == PostApplication.ApplicationStatus.ACCEPTED) {
            throw new RuntimeException("이미 지원한 게시글입니다.");
            }
            // CANCELLED 상태면 기존 지원서를 재활성화
            if (existing.getStatus() == PostApplication.ApplicationStatus.CANCELLED) {
                existing.setStatus(PostApplication.ApplicationStatus.PENDING);
                existing.setMessage(message);
                // createdAt은 DB에서 자동 설정되므로 업데이트 불가
                // 재지원은 updatedAt으로 추적 가능
                PostApplication saved = applicationRepository.save(existing);
                
                // 알림: 게시글 작성자에게 새로운 지원 도착
                try {
                    notificationService.notify(
                            post.getUser().getId(),
                            "NEW_APPLICATION",
                            "새로운 스터디 지원 도착",
                            user.getRealName() + " 님이 '" + post.getTitle() + "' 스터디에 지원했습니다.",
                            "/posts/" + postId
                    );
                } catch (Exception ignored) {}
                
                return saved;
            }
        }

        // 본인 게시글에는 지원 불가
        if (post.getUser().getId().equals(userId)) {
            throw new RuntimeException("본인의 게시글에는 지원할 수 없습니다.");
        }

        PostApplication application = new PostApplication();
        application.setPost(post);
        application.setUser(user);
        application.setMessage(message);
        application.setStatus(PostApplication.ApplicationStatus.PENDING);

        PostApplication saved = applicationRepository.save(application);

        // 알림: 게시글 작성자에게 새로운 지원 도착
        try {
            notificationService.notify(
                    post.getUser().getId(),
                    "NEW_APPLICATION",
                    "새로운 스터디 지원 도착",
                    user.getRealName() + " 님이 '" + post.getTitle() + "' 스터디에 지원했습니다.",
                    "/posts/" + postId
            );
        } catch (Exception ignored) {}

        return saved;
    }

    @Transactional
    public void cancelApplication(Integer userId, Long postId) {
        PostApplication application = applicationRepository.findByPost_IdAndUser_Id(postId, userId)
                .orElseThrow(() -> new RuntimeException("지원 내역을 찾을 수 없습니다."));

        if (!application.getUser().getId().equals(userId)) {
            throw new RuntimeException("취소할 권한이 없습니다.");
        }

        application.setStatus(PostApplication.ApplicationStatus.CANCELLED);
        applicationRepository.save(application);

        // 알림: 게시글 작성자에게 지원 취소 알림
        try {
            Post post = application.getPost();
            notificationService.notify(
                    post.getUser().getId(),
                    "APPLICATION_CANCELLED",
                    "스터디 지원 취소",
                    application.getUser().getRealName() + " 님이 '" + post.getTitle() + "' 스터디 지원을 취소했습니다.",
                    "/posts/" + post.getId()
            );
        } catch (Exception ignored) {}
    }

    public boolean hasApplied(Integer userId, Long postId) {
        return applicationRepository.existsByPost_IdAndUser_Id(postId, userId);
    }

    // 사용자의 특정 게시글에 대한 지원 내역 조회
    @Transactional(readOnly = true)
    public PostApplication getApplicationByUserAndPost(Integer userId, Long postId) {
        return applicationRepository.findByPost_IdAndUser_Id(postId, userId).orElse(null);
    }

    public int getApplicationCount(Long postId) {
        return applicationRepository.countByPost_IdAndStatus(postId, PostApplication.ApplicationStatus.PENDING);
    }

    // 게시글 작성자가 받은 지원 목록 조회
    @Transactional(readOnly = true)
    public List<PostApplication> getApplicationsByPost(Long postId, PostApplication.ApplicationStatus status) {
        if (status != null) {
            return applicationRepository.findByPost_IdAndStatusOrderByCreatedAtDesc(postId, status);
        }
        return applicationRepository.findByPost_IdOrderByCreatedAtDesc(postId);
    }

    // 사용자가 지원한 목록 조회
    @Transactional(readOnly = true)
    public List<PostApplication> getApplicationsByUser(Integer userId, PostApplication.ApplicationStatus status) {
        if (status != null) {
            return applicationRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(userId, status);
        }
        return applicationRepository.findByUser_IdOrderByCreatedAtDesc(userId);
    }

    // 지원 승인
    @Transactional
    public void acceptApplication(Long applicationId, Integer postOwnerId) {
        PostApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("지원 내역을 찾을 수 없습니다."));

        Post post = application.getPost();
        
        // 권한 확인 (게시글 작성자만 승인 가능)
        if (!post.getUser().getId().equals(postOwnerId)) {
            throw new RuntimeException("승인할 권한이 없습니다.");
        }

        // 이미 처리된 지원인지 확인
        if (application.getStatus() != PostApplication.ApplicationStatus.PENDING) {
            throw new RuntimeException("이미 처리된 지원입니다.");
        }

        application.setStatus(PostApplication.ApplicationStatus.ACCEPTED);
        applicationRepository.save(application);
        
        // 트랜잭션 플러시하여 상태 변경을 DB에 반영
        applicationRepository.flush();

        // 스터디 그룹 생성 또는 멤버 추가
        // 별도 트랜잭션으로 실행하여 메인 트랜잭션에 영향 없도록
        try {
            StudyGroup group = studyGroupService.createOrAddMember(post.getId(), application.getUser().getId(), postOwnerId);
            if (group != null) {
            log.info("스터디 그룹 생성/멤버 추가 성공: groupId={}, postId={}, userId={}", 
                        group.getId(), post.getId(), application.getUser().getId());
            } else {
                log.info("스터디 그룹 생성 조건 미충족 (2명 이상 필요): postId={}, userId={}", 
                        post.getId(), application.getUser().getId());
            }
        } catch (Exception e) {
            log.error("스터디 그룹 생성 실패: postId={}, userId={}, error={}", 
                    post.getId(), application.getUser().getId(), e.getMessage(), e);
            // 스터디 그룹 생성 실패해도 지원 승인은 유지
            // 예외를 다시 던지지 않음 (트랜잭션 롤백 방지)
            // 대신 경고 로그만 남김
            // 스택 트레이스 출력으로 디버깅 정보 제공
            e.printStackTrace();
        }

        // 알림: 지원자에게 승인 알림
        try {
            notificationService.notify(
                    application.getUser().getId(),
                    "APPLICATION_ACCEPTED",
                    "스터디 지원 승인",
                    "'" + post.getTitle() + "' 스터디 지원이 승인되었습니다!",
                    "/my-applications"
            );
        } catch (Exception ignored) {}
    }

    // 지원 거절
    @Transactional
    public void rejectApplication(Long applicationId, Integer postOwnerId) {
        PostApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("지원 내역을 찾을 수 없습니다."));

        Post post = application.getPost();
        
        // 권한 확인
        if (!post.getUser().getId().equals(postOwnerId)) {
            throw new RuntimeException("거절할 권한이 없습니다.");
        }

        if (application.getStatus() != PostApplication.ApplicationStatus.PENDING) {
            throw new RuntimeException("이미 처리된 지원입니다.");
        }

        application.setStatus(PostApplication.ApplicationStatus.REJECTED);
        applicationRepository.save(application);

        // 알림: 지원자에게 거절 알림
        try {
            notificationService.notify(
                    application.getUser().getId(),
                    "APPLICATION_REJECTED",
                    "스터디 지원 거절",
                    "'" + post.getTitle() + "' 스터디 지원이 거절되었습니다.",
                    "/my-applications"
            );
        } catch (Exception ignored) {}
    }
}

