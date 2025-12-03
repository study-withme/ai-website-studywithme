package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BlockedPostRepository blockedPostRepository;
    private final BlockedCommentRepository blockedCommentRepository;
    private final FilterWordRepository filterWordRepository;
    private final FilterKeywordRepository filterKeywordRepository;
    private final FilterPatternRepository filterPatternRepository;
    private final AILearningDataRepository aiLearningDataRepository;
    private final PostRepository postRepository;
    private final AITagService aiTagService;

    // 차단된 게시글 목록 조회
    public Page<BlockedPost> getBlockedPosts(int page, int size, BlockedPost.BlockStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            return blockedPostRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return blockedPostRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // 차단된 게시글 복구
    @Transactional
    public void restorePost(Long blockedPostId, Integer adminId) {
        if (blockedPostId == null) {
            throw new RuntimeException("차단된 게시글 ID가 필요합니다.");
        }
        BlockedPost blockedPost = blockedPostRepository.findById(blockedPostId)
                .orElseThrow(() -> new RuntimeException("차단된 게시글을 찾을 수 없습니다."));
        
        blockedPost.setStatus(BlockedPost.BlockStatus.RESTORED);
        blockedPost.setIsReviewed(true);
        blockedPost.setReviewedAt(LocalDateTime.now());
        
        User admin = new User();
        admin.setId(adminId);
        blockedPost.setReviewedBy(admin);
        
        blockedPostRepository.save(blockedPost);
    }

    // 욕설 필터 단어 추가
    @Transactional
    public FilterWord addFilterWord(String word, FilterWord.WordType wordType, Integer adminId) {
        Optional<FilterWord> existing = filterWordRepository.findByWord(word);
        if (existing.isPresent()) {
            FilterWord fw = existing.get();
            fw.setIsActive(true);
            fw.setWordType(wordType);
            return filterWordRepository.save(fw);
        }

        FilterWord filterWord = new FilterWord();
        filterWord.setWord(word);
        filterWord.setWordType(wordType);
        filterWord.setIsActive(true);
        
        if (adminId != null) {
            User admin = new User();
            admin.setId(adminId);
            filterWord.setCreatedBy(admin);
        }
        
        return filterWordRepository.save(filterWord);
    }

    // 욕설 필터 단어 삭제
    @Transactional
    public void deleteFilterWord(Long id) {
        if (id == null) {
            throw new RuntimeException("필터 단어 ID가 필요합니다.");
        }
        filterWordRepository.deleteById(id);
    }

    // 키워드 추가
    @Transactional
    public FilterKeyword addFilterKeyword(String keyword, FilterKeyword.KeywordType keywordType, 
                                         String description, Integer adminId) {
        Optional<FilterKeyword> existing = filterKeywordRepository.findByKeyword(keyword);
        if (existing.isPresent()) {
            FilterKeyword fk = existing.get();
            fk.setIsActive(true);
            fk.setKeywordType(keywordType);
            fk.setDescription(description);
            return filterKeywordRepository.save(fk);
        }

        FilterKeyword filterKeyword = new FilterKeyword();
        filterKeyword.setKeyword(keyword);
        filterKeyword.setKeywordType(keywordType);
        filterKeyword.setDescription(description);
        filterKeyword.setIsActive(true);
        
        if (adminId != null) {
            User admin = new User();
            admin.setId(adminId);
            filterKeyword.setCreatedBy(admin);
        }
        
        return filterKeywordRepository.save(filterKeyword);
    }

    // 키워드 삭제
    @Transactional
    public void deleteFilterKeyword(Long id) {
        if (id == null) {
            throw new RuntimeException("필터 키워드 ID가 필요합니다.");
        }
        filterKeywordRepository.deleteById(id);
    }

    // 패턴 추가
    @Transactional
    public FilterPattern addFilterPattern(String patternName, String patternRegex, 
                                         FilterPattern.PatternType patternType, 
                                         String description, Integer adminId) {
        FilterPattern pattern = new FilterPattern();
        pattern.setPatternName(patternName);
        pattern.setPatternRegex(patternRegex);
        pattern.setPatternType(patternType);
        pattern.setDescription(description);
        pattern.setIsActive(true);
        
        if (adminId != null) {
            User admin = new User();
            admin.setId(adminId);
            pattern.setCreatedBy(admin);
        }
        
        return filterPatternRepository.save(pattern);
    }

    // 패턴 삭제
    @Transactional
    public void deleteFilterPattern(Long id) {
        if (id == null) {
            throw new RuntimeException("필터 패턴 ID가 필요합니다.");
        }
        filterPatternRepository.deleteById(id);
    }

    // AI 학습 데이터 조회 (차단 빈도 높은 패턴)
    public Page<AILearningData> getLearningData(int page, int size, int minFrequency) {
        Pageable pageable = PageRequest.of(page, size);
        if (minFrequency > 0) {
            return aiLearningDataRepository.findHighFrequencyPatterns(minFrequency, pageable);
        }
        return aiLearningDataRepository.findAllByOrderByFrequencyDesc(pageable);
    }

    // 모든 필터 단어 조회
    public List<FilterWord> getAllFilterWords() {
        return filterWordRepository.findAll();
    }

    // 모든 키워드 조회
    public List<FilterKeyword> getAllFilterKeywords() {
        return filterKeywordRepository.findAll();
    }

    // 모든 패턴 조회
    public List<FilterPattern> getAllFilterPatterns() {
        return filterPatternRepository.findAll();
    }

    // 차단된 댓글 목록 조회
    public Page<BlockedComment> getBlockedComments(int page, int size, BlockedComment.BlockStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            return blockedCommentRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return blockedCommentRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // 차단된 댓글 복구
    @Transactional
    public void restoreComment(Long blockedCommentId, Integer adminId) {
        if (blockedCommentId == null) {
            throw new RuntimeException("차단된 댓글 ID가 필요합니다.");
        }
        BlockedComment blockedComment = blockedCommentRepository.findById(blockedCommentId)
                .orElseThrow(() -> new RuntimeException("차단된 댓글을 찾을 수 없습니다."));
        
        blockedComment.setStatus(BlockedComment.BlockStatus.RESTORED);
        blockedComment.setIsReviewed(true);
        blockedComment.setReviewedAt(LocalDateTime.now());
        
        User admin = new User();
        admin.setId(adminId);
        blockedComment.setReviewedBy(admin);
        
        blockedCommentRepository.save(blockedComment);
    }

    // 통계 정보
    public AdminStats getStats() {
        AdminStats stats = new AdminStats();
        
        // 각 통계를 개별적으로 처리하여 하나가 실패해도 나머지는 조회 가능하도록
        try {
            stats.setTotalBlockedPosts(blockedPostRepository.countBlocked());
        } catch (Exception e) {
            stats.setTotalBlockedPosts(0);
            System.err.println("차단된 게시글 통계 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            stats.setTotalBlockedComments(blockedCommentRepository.countBlocked());
        } catch (Exception e) {
            stats.setTotalBlockedComments(0);
            System.err.println("차단된 댓글 통계 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            stats.setTotalFilterWords(filterWordRepository.count());
        } catch (Exception e) {
            stats.setTotalFilterWords(0);
            System.err.println("필터 단어 통계 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            stats.setTotalFilterKeywords(filterKeywordRepository.count());
        } catch (Exception e) {
            stats.setTotalFilterKeywords(0);
            System.err.println("필터 키워드 통계 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            stats.setTotalFilterPatterns(filterPatternRepository.count());
        } catch (Exception e) {
            stats.setTotalFilterPatterns(0);
            System.err.println("필터 패턴 통계 조회 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }

    /**
     * 모든 게시글에 대해 AI 기반으로 카테고리/태그를 재분류합니다.
     * - 제목+본문을 기반으로 Python 태그 추천기를 호출
     * - 기존 카테고리와 다르고, 신뢰도 기준(예: 0.6 이상)을 넘으면 카테고리를 교체
     * - 태그가 비어 있는 경우 AI가 추천한 태그를 기본값으로 설정
     *
     * @return 재분류된 게시글 수
     */
    @Transactional
    public int reclassifyAllPostsByAI(double minConfidence) {
        List<Post> posts = postRepository.findAll();
        int updatedCount = 0;

        for (Post post : posts) {
            String title = post.getTitle() != null ? post.getTitle() : "";
            String content = post.getContent() != null ? post.getContent() : "";
            String originalCategory = post.getCategory();
            String originalTags = post.getTags();

            try {
                Map<String, Object> result = aiTagService.recommendTags(title, content);

                String aiCategory = (String) result.getOrDefault("category", originalCategory);
                Double confidence = (Double) result.getOrDefault("category_confidence", 0.0);

                boolean changed = false;

                // 카테고리 교정
                if (aiCategory != null
                        && !aiCategory.isBlank()
                        && confidence != null
                        && confidence >= minConfidence) {
                    if (originalCategory == null || !aiCategory.equals(originalCategory)) {
                        post.setCategory(aiCategory);
                        changed = true;
                    }
                }

                // 태그 채우기 (비어 있을 때만)
                if ((originalTags == null || originalTags.isBlank()) && result.get("tags") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> aiTags = (List<String>) result.get("tags");
                    if (!aiTags.isEmpty()) {
                        post.setTags(String.join(",", aiTags));
                        changed = true;
                    }
                }

                if (changed) {
                    post.setAiAnalyzed(true);
                    post.setAiAnalyzedAt(LocalDateTime.now());
                    postRepository.save(post);
                    updatedCount++;
                }

            } catch (Exception e) {
                // 개별 게시글 처리 실패는 전체 배치를 멈추지 않음
                // 필요하다면 로그만 남기고 무시
                System.err.println("게시글 ID " + post.getId() + " 재분류 중 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return updatedCount;
    }

    public static class AdminStats {
        private long totalBlockedPosts;
        private long totalBlockedComments;
        private long totalFilterWords;
        private long totalFilterKeywords;
        private long totalFilterPatterns;

        // Getters and Setters
        public long getTotalBlockedPosts() { return totalBlockedPosts; }
        public void setTotalBlockedPosts(long totalBlockedPosts) { this.totalBlockedPosts = totalBlockedPosts; }
        public long getTotalBlockedComments() { return totalBlockedComments; }
        public void setTotalBlockedComments(long totalBlockedComments) { this.totalBlockedComments = totalBlockedComments; }
        public long getTotalFilterWords() { return totalFilterWords; }
        public void setTotalFilterWords(long totalFilterWords) { this.totalFilterWords = totalFilterWords; }
        public long getTotalFilterKeywords() { return totalFilterKeywords; }
        public void setTotalFilterKeywords(long totalFilterKeywords) { this.totalFilterKeywords = totalFilterKeywords; }
        public long getTotalFilterPatterns() { return totalFilterPatterns; }
        public void setTotalFilterPatterns(long totalFilterPatterns) { this.totalFilterPatterns = totalFilterPatterns; }
    }
}

