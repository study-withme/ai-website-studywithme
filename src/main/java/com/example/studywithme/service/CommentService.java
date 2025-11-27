package com.example.studywithme.service;

import com.example.studywithme.entity.Comment;
import com.example.studywithme.entity.CommentLike;
import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.User;
import com.example.studywithme.repository.CommentLikeRepository;
import com.example.studywithme.repository.CommentRepository;
import com.example.studywithme.repository.PostRepository;
import com.example.studywithme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId, String sort, Integer currentUserId) {
        List<Comment> all = commentRepository.findByPost_IdAndDeletedFalseOrderByCreatedAtAsc(postId);

        Comparator<Comment> comparator;
        if ("popular".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparingInt((Comment c) -> c.getLikeCount() != null ? c.getLikeCount() : 0)
                    .reversed()
                    .thenComparing(Comment::getId);
        } else {
            comparator = Comparator.comparing(Comment::getId).reversed();
        }

        return all.stream()
                .sorted(comparator)
                .map(c -> toResponse(c, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse addComment(Integer userId, Long postId, String content, Long parentId,
                                      String ip, String userAgent) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("댓글 내용을 입력해주세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setPost(post);
        comment.setContent(content.trim());
        comment.setLikeCount(0);
        comment.setReportCount(0);
        comment.setDeleted(false);
        comment.setIpAddress(ip);
        comment.setUserAgent(userAgent);

        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다."));
            comment.setParentComment(parent);
        }

        Comment saved = commentRepository.save(comment);

        // 알림: 대댓글이면 부모 댓글 작성자, 아니면 게시글 작성자
        try {
            if (saved.getParentComment() != null && saved.getParentComment().getUser() != null) {
                Integer targetUserId = saved.getParentComment().getUser().getId();
                if (!targetUserId.equals(userId)) {
                    notificationService.notify(
                            targetUserId,
                            "NEW_REPLY",
                            "내 댓글에 새로운 답글",
                            saved.getContent(),
                            "/posts/" + postId + "#comment-" + saved.getId()
                    );
                }
            } else if (post.getUser() != null && !post.getUser().getId().equals(userId)) {
                notificationService.notify(
                        post.getUser().getId(),
                        "NEW_COMMENT",
                        "내 게시글에 새로운 댓글",
                        saved.getContent(),
                        "/posts/" + postId + "#comment-" + saved.getId()
                );
            }
        } catch (Exception ignored) {}

        return toResponse(saved, userId);
    }

    @Transactional
    public boolean toggleLike(Integer userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        boolean exists = commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);

        if (exists) {
            commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
            comment.setLikeCount(Math.max(0, (comment.getLikeCount() != null ? comment.getLikeCount() : 0) - 1));
            commentRepository.save(comment);
            return false;
        } else {
            CommentLike like = new CommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            commentLikeRepository.save(like);
            comment.setLikeCount((comment.getLikeCount() != null ? comment.getLikeCount() : 0) + 1);
            commentRepository.save(comment);

            // 알림: 댓글 작성자와 다를 때만
            try {
                if (comment.getUser() != null && !comment.getUser().getId().equals(userId)) {
                    notificationService.notify(
                            comment.getUser().getId(),
                            "COMMENT_LIKE",
                            "내 댓글에 좋아요가 눌렸습니다",
                            comment.getContent(),
                            "/posts/" + comment.getPost().getId() + "#comment-" + comment.getId()
                    );
                }
            } catch (Exception ignored) {}

            return true;
        }
    }

    private CommentResponse toResponse(Comment c, Integer currentUserId) {
        CommentResponse dto = new CommentResponse();
        dto.setId(c.getId());
        dto.setContent(c.getContent());
        dto.setLikes(c.getLikeCount() != null ? c.getLikeCount() : 0);
        dto.setParentId(c.getParentComment() != null ? c.getParentComment().getId() : null);
        dto.setUser(c.getUser() != null ? c.getUser().getRealName() : "익명");
        dto.setAvatar("https://i.pravatar.cc/40?img=" + (c.getUser() != null ? c.getUser().getId() : 1));
        dto.setTime(c.getCreatedAt() != null ? c.getCreatedAt().format(TIME_FORMATTER) : "");
        dto.setMine(currentUserId != null && c.getUser() != null && currentUserId.equals(c.getUser().getId()));
        return dto;
    }

    @lombok.Getter
    @lombok.Setter
    public static class CommentResponse {
        private Long id;
        private String user;
        private String avatar;
        private String content;
        private String time;
        private Integer likes;
        private Long parentId;
        private boolean mine;
    }
}


