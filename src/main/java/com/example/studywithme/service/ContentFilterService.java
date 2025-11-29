package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ContentFilterService {

    private final FilterWordRepository filterWordRepository;
    private final FilterKeywordRepository filterKeywordRepository;
    private final FilterPatternRepository filterPatternRepository;
    private final BlockedPostRepository blockedPostRepository;
    private final BlockedCommentRepository blockedCommentRepository;
    private final AILearningDataRepository aiLearningDataRepository;

    /**
     * 게시글 내용 필터링 (욕설, 키워드, 패턴 체크)
     * @param postId null이면 검사만 수행하고 차단하지 않음
     */
    @Transactional
    public FilterResult filterContent(String title, String content, Long postId, Integer userId) {
        FilterResult result = new FilterResult();
        result.setBlocked(false);

        // HTML 태그 제거
        String textTitle = title.replaceAll("<[^>]*>", "").trim();
        String textContent = content.replaceAll("<[^>]*>", "").trim();
        String fullText = (textTitle + " " + textContent).toLowerCase();

        // 1. 욕설 필터 단어 체크
        List<FilterWord> activeWords = filterWordRepository.findByIsActiveTrue();
        for (FilterWord word : activeWords) {
            if (fullText.contains(word.getWord().toLowerCase())) {
                result.setBlocked(true);
                result.setBlockReason("욕설 감지: " + word.getWord());
                result.setBlockType(BlockedPost.BlockType.PROFANITY);
                result.setDetectedKeyword(word.getWord());
                break;
            }
        }

        if (result.isBlocked()) {
            if (postId != null) {
                blockPost(postId, userId, title, content, result);
            }
            return result;
        }

        // 2. 키워드 체크
        List<FilterKeyword> activeKeywords = filterKeywordRepository.findByIsActiveTrue();
        for (FilterKeyword keyword : activeKeywords) {
            boolean matched = false;
            String kw = keyword.getKeyword().toLowerCase();
            
            switch (keyword.getKeywordType()) {
                case EXACT:
                    matched = fullText.equals(kw);
                    break;
                case PARTIAL:
                    matched = fullText.contains(kw);
                    break;
                case REGEX:
                    try {
                        Pattern pattern = Pattern.compile(kw, Pattern.CASE_INSENSITIVE);
                        matched = pattern.matcher(fullText).find();
                    } catch (Exception e) {
                        // 잘못된 정규식은 무시
                    }
                    break;
            }

            if (matched) {
                result.setBlocked(true);
                result.setBlockReason("금지 키워드 감지: " + keyword.getKeyword());
                result.setBlockType(BlockedPost.BlockType.KEYWORD);
                result.setDetectedKeyword(keyword.getKeyword());
                
                // 차단 횟수 증가
                keyword.setBlockCount(keyword.getBlockCount() + 1);
                filterKeywordRepository.save(keyword);
                
                break;
            }
        }

        if (result.isBlocked()) {
            if (postId != null) {
                blockPost(postId, userId, title, content, result);
            }
            return result;
        }

        // 3. 패턴 체크
        List<FilterPattern> activePatterns = filterPatternRepository.findByIsActiveTrue();
        for (FilterPattern pattern : activePatterns) {
            try {
                Pattern regexPattern = Pattern.compile(pattern.getPatternRegex(), Pattern.CASE_INSENSITIVE);
                boolean matched = false;
                
                switch (pattern.getPatternType()) {
                    case TITLE:
                        matched = regexPattern.matcher(textTitle).find();
                        break;
                    case CONTENT:
                        matched = regexPattern.matcher(textContent).find();
                        break;
                    case BOTH:
                        matched = regexPattern.matcher(textTitle).find() || 
                                  regexPattern.matcher(textContent).find();
                        break;
                }

                if (matched) {
                    result.setBlocked(true);
                    result.setBlockReason("차단 패턴 감지: " + pattern.getPatternName());
                    result.setBlockType(BlockedPost.BlockType.PATTERN);
                    
                    // 차단 횟수 증가
                    pattern.setBlockCount(pattern.getBlockCount() + 1);
                    filterPatternRepository.save(pattern);
                    
                    // 학습 데이터 저장
                    saveLearningData(textTitle + " " + textContent, result.getBlockReason(), pattern.getPatternRegex());
                    
                    break;
                }
            } catch (Exception e) {
                // 잘못된 정규식은 무시
            }
        }

        if (result.isBlocked()) {
            if (postId != null) {
                blockPost(postId, userId, title, content, result);
            }
        }

        return result;
    }

    private void blockPost(Long postId, Integer userId, String title, String content, FilterResult result) {
        if (postId == null) return;

        BlockedPost blockedPost = new BlockedPost();
        blockedPost.setPostId(postId);
        blockedPost.setTitle(title);
        blockedPost.setContent(content);
        blockedPost.setBlockReason(result.getBlockReason());
        blockedPost.setBlockType(result.getBlockType());
        blockedPost.setStatus(BlockedPost.BlockStatus.BLOCKED);
        
        if (result.getDetectedKeyword() != null) {
            blockedPost.setDetectedKeywords("[\"" + result.getDetectedKeyword() + "\"]");
        }

        // User 엔티티 설정
        User user = new User();
        user.setId(userId);
        blockedPost.setUser(user);

        blockedPostRepository.save(blockedPost);
    }

    /**
     * 댓글 내용 필터링 (욕설, 키워드, 패턴 체크)
     * @param commentId null이면 검사만 수행하고 차단하지 않음
     */
    @Transactional
    public FilterResult filterComment(String content, Long commentId, Long postId, Integer userId) {
        FilterResult result = new FilterResult();
        result.setBlocked(false);

        // HTML 태그 제거
        String textContent = content.replaceAll("<[^>]*>", "").trim();
        String fullText = textContent.toLowerCase();

        // 1. 욕설 필터 단어 체크
        List<FilterWord> activeWords = filterWordRepository.findByIsActiveTrue();
        for (FilterWord word : activeWords) {
            if (fullText.contains(word.getWord().toLowerCase())) {
                result.setBlocked(true);
                result.setBlockReason("욕설 감지: " + word.getWord());
                result.setBlockType(BlockedPost.BlockType.PROFANITY);
                result.setDetectedKeyword(word.getWord());
                break;
            }
        }

        if (result.isBlocked()) {
            if (commentId != null) {
                blockComment(commentId, postId, userId, content, result);
            }
            return result;
        }

        // 2. 키워드 체크
        List<FilterKeyword> activeKeywords = filterKeywordRepository.findByIsActiveTrue();
        for (FilterKeyword keyword : activeKeywords) {
            boolean matched = false;
            String kw = keyword.getKeyword().toLowerCase();
            
            switch (keyword.getKeywordType()) {
                case EXACT:
                    matched = fullText.equals(kw);
                    break;
                case PARTIAL:
                    matched = fullText.contains(kw);
                    break;
                case REGEX:
                    try {
                        Pattern pattern = Pattern.compile(kw, Pattern.CASE_INSENSITIVE);
                        matched = pattern.matcher(fullText).find();
                    } catch (Exception e) {
                        // 잘못된 정규식은 무시
                    }
                    break;
            }

            if (matched) {
                result.setBlocked(true);
                result.setBlockReason("금지 키워드 감지: " + keyword.getKeyword());
                result.setBlockType(BlockedPost.BlockType.KEYWORD);
                result.setDetectedKeyword(keyword.getKeyword());
                
                // 차단 횟수 증가
                keyword.setBlockCount(keyword.getBlockCount() + 1);
                filterKeywordRepository.save(keyword);
                
                break;
            }
        }

        if (result.isBlocked()) {
            if (commentId != null) {
                blockComment(commentId, postId, userId, content, result);
            }
            return result;
        }

        // 3. 패턴 체크 (댓글은 CONTENT만 체크)
        List<FilterPattern> activePatterns = filterPatternRepository.findByIsActiveTrue();
        for (FilterPattern pattern : activePatterns) {
            try {
                Pattern regexPattern = Pattern.compile(pattern.getPatternRegex(), Pattern.CASE_INSENSITIVE);
                boolean matched = false;
                
                switch (pattern.getPatternType()) {
                    case CONTENT:
                    case BOTH:
                        matched = regexPattern.matcher(textContent).find();
                        break;
                    case TITLE:
                        // 댓글에는 제목이 없으므로 스킵
                        break;
                }

                if (matched) {
                    result.setBlocked(true);
                    result.setBlockReason("차단 패턴 감지: " + pattern.getPatternName());
                    result.setBlockType(BlockedPost.BlockType.PATTERN);
                    
                    // 차단 횟수 증가
                    pattern.setBlockCount(pattern.getBlockCount() + 1);
                    filterPatternRepository.save(pattern);
                    
                    // 학습 데이터 저장
                    saveLearningData(textContent, result.getBlockReason(), pattern.getPatternRegex(), AILearningData.ContentType.COMMENT);
                    
                    break;
                }
            } catch (Exception e) {
                // 잘못된 정규식은 무시
            }
        }

        if (result.isBlocked()) {
            if (commentId != null) {
                blockComment(commentId, postId, userId, content, result);
            }
        }

        return result;
    }

    private void blockComment(Long commentId, Long postId, Integer userId, String content, FilterResult result) {
        if (commentId == null) return;

        BlockedComment blockedComment = new BlockedComment();
        blockedComment.setCommentId(commentId);
        blockedComment.setPostId(postId);
        blockedComment.setContent(content);
        blockedComment.setBlockReason(result.getBlockReason());
        blockedComment.setBlockType(BlockedComment.BlockType.valueOf(result.getBlockType().name()));
        blockedComment.setStatus(BlockedComment.BlockStatus.BLOCKED);
        
        if (result.getDetectedKeyword() != null) {
            blockedComment.setDetectedKeywords("[\"" + result.getDetectedKeyword() + "\"]");
        }

        // User 엔티티 설정
        User user = new User();
        user.setId(userId);
        blockedComment.setUser(user);

        blockedCommentRepository.save(blockedComment);
    }

    private void saveLearningData(String contentSample, String blockReason, String detectedPattern) {
        saveLearningData(contentSample, blockReason, detectedPattern, AILearningData.ContentType.POST);
    }

    private void saveLearningData(String contentSample, String blockReason, String detectedPattern, AILearningData.ContentType contentType) {
        AILearningData learningData = new AILearningData();
        learningData.setContentType(contentType);
        learningData.setContentSample(contentSample);
        learningData.setBlockReason(blockReason);
        learningData.setDetectedPattern(detectedPattern);
        learningData.setFrequency(1);
        aiLearningDataRepository.save(learningData);
    }

    public static class FilterResult {
        private boolean blocked;
        private String blockReason;
        private BlockedPost.BlockType blockType;
        private String detectedKeyword;
        private Float aiConfidence;

        // Getters and Setters
        public boolean isBlocked() { return blocked; }
        public void setBlocked(boolean blocked) { this.blocked = blocked; }
        public String getBlockReason() { return blockReason; }
        public void setBlockReason(String blockReason) { this.blockReason = blockReason; }
        public BlockedPost.BlockType getBlockType() { return blockType; }
        public void setBlockType(BlockedPost.BlockType blockType) { this.blockType = blockType; }
        public String getDetectedKeyword() { return detectedKeyword; }
        public void setDetectedKeyword(String detectedKeyword) { this.detectedKeyword = detectedKeyword; }
        public Float getAiConfidence() { return aiConfidence; }
        public void setAiConfidence(Float aiConfidence) { this.aiConfidence = aiConfidence; }
    }
}

