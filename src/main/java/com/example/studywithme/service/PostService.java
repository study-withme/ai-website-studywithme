package com.example.studywithme.service;

import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.User;
import com.example.studywithme.repository.PostRepository;
import com.example.studywithme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ContentFilterService contentFilterService;
    private final AITagService aiTagService;

    // 게시글 작성
    @Transactional
    public Post createPost(Integer userId, String title, String content, String category, String tags) {
        if (userId == null) {
            throw new RuntimeException("사용자 ID가 필요합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        post.setTags(tags);
        post.setViewCount(0);
        post.setLikeCount(0);

        Post savedPost = postRepository.save(post);
        
        // AI 필터링 체크 (저장 후 검사하여 blocked_posts 테이블에 기록)
        ContentFilterService.FilterResult filterResult = contentFilterService.filterContent(title, content, savedPost.getId(), userId);
        if (filterResult.isBlocked()) {
            // 차단된 경우 게시글 삭제
            postRepository.delete(savedPost);
            throw new RuntimeException("게시글이 차단되었습니다: " + filterResult.getBlockReason());
        }

        // AI 기반 카테고리/태그 자동 보정
        try {
            // 제목+본문으로 카테고리/태그 추천
            java.util.Map<String, Object> aiResult = aiTagService.recommendTags(title, content);
            String aiCategory = (String) aiResult.getOrDefault("category", category);
            Double confidence = (Double) aiResult.getOrDefault("category_confidence", 0.0);

            // 미스매치이면서 신뢰도가 충분히 높을 때만 카테고리 자동 교정
            // 예: 사용자가 선택한 카테고리와 다르고, 신뢰도 0.6 이상일 때
            if (aiCategory != null
                    && !aiCategory.isBlank()
                    && !aiCategory.equals(category)
                    && confidence != null
                    && confidence >= 0.6) {
                savedPost.setCategory(aiCategory);
            }

            // 태그가 비어 있으면 AI가 추천한 태그를 기본값으로 사용
            if ((tags == null || tags.isBlank()) && aiResult.get("tags") instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> aiTags = (java.util.List<String>) aiResult.get("tags");
                if (!aiTags.isEmpty()) {
                    savedPost.setTags(String.join(",", aiTags));
                }
            }

            // 태그 내용 기반으로 카테고리 자동 보정
            String adjustedCategory = adjustCategoryByTags(savedPost.getCategory(), savedPost.getTags());
            savedPost.setCategory(adjustedCategory);

            // AI 분석 여부/시간 기록 (있을 경우)
            savedPost.setAiAnalyzed(true);
            savedPost.setAiAnalyzedAt(java.time.LocalDateTime.now());

            savedPost = postRepository.save(savedPost);
        } catch (Exception e) {
            // AI 태그 추천 실패 시에도 글 저장은 유지
            // 필요시 로그만 남기고 무시
        }

        return savedPost;
    }

    // 게시글 수정
    @Transactional
    public Post updatePost(Long postId, Integer userId, String title, String content, String category, String tags) {
        if (postId == null) {
            throw new RuntimeException("게시글 ID가 필요합니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("게시글을 수정할 권한이 없습니다.");
        }

        // AI 필터링 체크
        ContentFilterService.FilterResult filterResult = contentFilterService.filterContent(title, content, postId, userId);
        if (filterResult.isBlocked()) {
            throw new RuntimeException("게시글이 차단되었습니다: " + filterResult.getBlockReason());
        }

        post.setTitle(title);
        post.setContent(content);
        post.setCategory(category);
        post.setTags(tags);

        // AI 기반 카테고리/태그 자동 보정
        try {
            java.util.Map<String, Object> aiResult = aiTagService.recommendTags(title, content);
            String aiCategory = (String) aiResult.getOrDefault("category", category);
            Double confidence = (Double) aiResult.getOrDefault("category_confidence", 0.0);

            if (aiCategory != null
                    && !aiCategory.isBlank()
                    && !aiCategory.equals(category)
                    && confidence != null
                    && confidence >= 0.6) {
                post.setCategory(aiCategory);
            }

            if ((tags == null || tags.isBlank()) && aiResult.get("tags") instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> aiTags = (java.util.List<String>) aiResult.get("tags");
                if (!aiTags.isEmpty()) {
                    post.setTags(String.join(",", aiTags));
                }
            }

            // 태그 내용 기반으로 카테고리 자동 보정
            String adjustedCategory = adjustCategoryByTags(post.getCategory(), post.getTags());
            post.setCategory(adjustedCategory);

            post.setAiAnalyzed(true);
            post.setAiAnalyzedAt(java.time.LocalDateTime.now());
        } catch (Exception e) {
            // AI 태그 추천 실패 시에는 기존 카테고리/태그 유지
        }

        return postRepository.save(post);
    }

    /**
     * 태그 문자열을 기반으로 카테고리를 보정합니다.
     * - 태그에 개발 관련 키워드가 있으면 "개발"
     * - 영어/어학 관련 키워드가 있으면 "영어"
     * - 자격증 관련 키워드가 있으면 "자격증"
     * - 취업 관련 키워드가 있으면 "취업"
     * - 독서 관련 키워드가 있으면 "독서"
     * 위에 아무것도 없으면 기존 카테고리를 그대로 유지합니다.
     */
    private String adjustCategoryByTags(String currentCategory, String tags) {
        if (tags == null || tags.isBlank()) {
            return currentCategory;
        }

        String lower = tags.toLowerCase();

        // 개발 관련
        if (containsAny(lower,
                "cs", "네트워크", "운영체제", "os", "database", "데이터베이스",
                "java", "자바", "python", "파이썬", "spring", "스프링",
                "react", "리액트", "node", "알고리즘", "프로그래밍", "개발")) {
            return "개발";
        }

        // 영어/어학 관련
        if (containsAny(lower,
                "영어", "english", "토익", "toeic", "토플", "toefl",
                "오픽", "opic", "ielts", "회화", "speaking")) {
            return "영어";
        }

        // 자격증 관련
        if (containsAny(lower,
                "자격증", "certificate", "정보처리기사", "컴활", "sqld",
                "리눅스마스터", "한국사", "공인회계사")) {
            return "자격증";
        }

        // 취업 관련
        if (containsAny(lower,
                "취업", "job", "면접", "interview", "포트폴리오",
                "이력서", "자소서", "인턴", "공채", "코딩테스트", "코테")) {
            return "취업";
        }

        // 독서/책 관련
        if (containsAny(lower,
                "독서", "book", "책", "서평", "리뷰", "에세이", "소설")) {
            return "독서";
        }

        return currentCategory;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId, Integer userId) {
        if (postId == null) {
            throw new RuntimeException("게시글 ID가 필요합니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 작성자 확인
        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("게시글을 삭제할 권한이 없습니다.");
        }

        postRepository.delete(post);
    }

    // 게시글 상세 조회 (조회수 증가, 좋아요 수 동기화)
    @Transactional
    public Post getPost(Long postId) {
        if (postId == null) {
            throw new RuntimeException("게시글 ID가 필요합니다.");
        }
        Post post = postRepository.findByIdWithUser(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 조회수 증가
        Integer currentViews = post.getViewCount() != null ? post.getViewCount() : 0;
        post.setViewCount(currentViews + 1);
        
        // 좋아요 수 동기화 (실제 DB의 좋아요 수와 동기화)
        // PostLikeService를 주입받아 사용하거나, 여기서 직접 카운트할 수 있음
        // 일단은 기존 likeCount를 유지
        
        postRepository.save(post);

        return post;
    }

    // 게시글 목록 조회 (최신순 또는 인기순)
    @Transactional(readOnly = true)
    public Page<Post> getPosts(Pageable pageable, String sort) {
        if ("popular".equals(sort)) {
            return postRepository.findAllByOrderByPopularityDesc(pageable);
        }
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // 카테고리별 게시글 조회 (최신순 또는 인기순)
    @Transactional(readOnly = true)
    public Page<Post> getPostsByCategory(String category, Pageable pageable, String sort) {
        if ("popular".equals(sort)) {
            return postRepository.findByCategoryOrderByPopularityDesc(category, pageable);
        }
        return postRepository.findByCategoryOrderByCreatedAtDesc(category, pageable);
    }

    // 사용자별 게시글 조회
    @Transactional(readOnly = true)
    public Page<Post> getPostsByUserId(Integer userId, Pageable pageable) {
        return postRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }

    // 게시글 검색
    @Transactional(readOnly = true)
    public Page<Post> searchPosts(String keyword, Pageable pageable) {
        return postRepository.searchByKeyword(keyword, pageable);
    }

    // 게시글 존재 여부 확인
    @Transactional(readOnly = true)
    public boolean existsById(Long postId) {
        if (postId == null) {
            return false;
        }
        return postRepository.existsById(postId);
    }

    /**
     * 기존에 저장된 모든 게시글에 대해
     * 태그 내용을 기준으로 카테고리를 일괄 보정합니다.
     *
     * @return 실제로 카테고리가 변경된 게시글 수
     */
    @Transactional
    public int adjustAllPostCategoriesByTagsForAllPosts() {
        List<Post> posts = postRepository.findAll();
        int updated = 0;

        for (Post post : posts) {
            String originalCategory = post.getCategory();
            String adjustedCategory = adjustCategoryByTags(originalCategory, post.getTags());

            if (!Objects.equals(originalCategory, adjustedCategory)) {
                post.setCategory(adjustedCategory);
                updated++;
            }
        }

        return updated;
    }

    // 작성자의 다른 게시글 상위 5개
    @Transactional(readOnly = true)
    public java.util.List<Post> getOtherPostsByAuthor(Integer userId, Long excludePostId) {
        return postRepository.findTop5ByUser_IdAndIdNotOrderByCreatedAtDesc(userId, excludePostId);
    }
}

