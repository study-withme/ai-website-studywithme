package com.example.studywithme.service;

import com.example.studywithme.entity.ChatMessage;
import com.example.studywithme.entity.Post;
import com.example.studywithme.entity.User;
import com.example.studywithme.repository.ChatMessageRepository;
import com.example.studywithme.repository.PostRepository;
import com.example.studywithme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final ChatMessageRepository chatMessageRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    // RestTemplate은 필요할 때마다 생성 (Bean으로 관리하지 않음)
    private RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.model:gemini-1.5-pro}")
    private String geminiModel;
    
    @Value("${gemini.api.url:}")
    private String geminiApiUrl;

    // 웹사이트 기능 설명 (시스템 프롬프트)
    private static final String WEBSITE_CONTEXT = """
        당신은 "Study With Me" 스터디 매칭 플랫폼의 AI 고객센터 어시스턴트입니다.
        
        ## 웹사이트 주요 기능:
        1. **게시글 작성/수정/삭제**: 스터디 모집 게시글을 작성하고 관리할 수 있습니다.
        2. **게시글 검색**: 키워드로 원하는 스터디를 검색할 수 있습니다.
        3. **카테고리 필터링**: 프로그래밍, 스터디, 모임, 언어, 취미, 자격증, 취업 등 카테고리별로 필터링 가능합니다.
        4. **좋아요/북마크**: 관심 있는 게시글에 좋아요를 누르거나 북마크할 수 있습니다.
        5. **댓글 작성**: 게시글에 댓글을 달아 소통할 수 있습니다.
        6. **게시글 지원**: 스터디에 지원할 수 있습니다.
        7. **AI 추천**: 개인화된 스터디 추천을 받을 수 있습니다 (/recommend 페이지).
        8. **마이페이지**: 내가 작성한 게시글, 북마크, 지원 현황을 확인할 수 있습니다.
        9. **진행중인 스터디**: 참여 중인 스터디 그룹을 관리할 수 있습니다.
        
        ## 사용자 요청 처리 규칙:
        - 사용자가 "마이페이지 보여줘", "내 게시글 보여줘" 같은 요청을 하면 → action: "SHOW_MYPAGE"
        - "게시글 검색", "스터디 찾아줘", "프로그래밍 스터디 보여줘" 같은 요청 → action: "SEARCH_POSTS", keyword 포함
        - "북마크 보여줘", "저장한 글 보여줘" → action: "SHOW_BOOKMARKS"
        - "AI 추천 받고 싶어" → action: "SHOW_RECOMMENDATIONS"
        - 일반적인 질문이나 도움 요청은 action 없이 텍스트 응답만 제공
        
        응답은 친절하고 도움이 되는 톤으로 작성하되, 가능한 한 간결하게 답변하세요.
        """;

    /**
     * 사용자 메시지 처리 및 AI 응답 생성
     */
    @Transactional
    public Map<String, Object> processMessage(String userMessage, Integer userId) {
        try {
            // 1. 사용자 메시지 저장
            User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
            ChatMessage userMsg = new ChatMessage();
            userMsg.setUser(user);
            userMsg.setMessage(userMessage);
            userMsg.setResponse(""); // 사용자 메시지는 응답 없음
            userMsg.setRole(ChatMessage.MessageRole.USER);
            chatMessageRepository.save(userMsg);

            // 2. 최근 대화 맥락 가져오기 (최근 10개)
            List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessages(
                userId, 10
            );
            Collections.reverse(recentMessages); // 시간순으로 정렬

            // 3. 요청 파싱 및 액션 결정
            ActionInfo actionInfo = parseUserRequest(userMessage, userId);

            // 4. Gemini API 호출
            String aiResponse = callGeminiAPI(userMessage, recentMessages, userId, actionInfo);

            // 5. AI 응답 저장
            ChatMessage aiMsg = new ChatMessage();
            aiMsg.setUser(user);
            aiMsg.setMessage("");
            aiMsg.setResponse(aiResponse);
            aiMsg.setRole(ChatMessage.MessageRole.ASSISTANT);
            if (actionInfo != null && actionInfo.actionType != null) {
                aiMsg.setActionType(actionInfo.actionType);
                aiMsg.setActionData(actionInfo.actionData);
            }
            chatMessageRepository.save(aiMsg);

            // 6. 응답 반환
            Map<String, Object> response = new HashMap<>();
            response.put("message", aiResponse);
            response.put("action", actionInfo != null ? actionInfo.actionType : null);
            response.put("actionData", actionInfo != null ? actionInfo.actionData : null);
            response.put("timestamp", LocalDateTime.now().toString());

            return response;

        } catch (Exception e) {
            log.error("챗봇 메시지 처리 오류", e);
            return Map.of(
                "message", "죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
                "error", e.getMessage()
            );
        }
    }

    /**
     * 사용자 요청 파싱 및 액션 결정
     */
    private ActionInfo parseUserRequest(String message, Integer userId) {
        String lowerMsg = message.toLowerCase().trim();

        // 마이페이지 관련
        if (lowerMsg.contains("마이페이지") || lowerMsg.contains("내 게시글") || 
            lowerMsg.contains("내 글") || lowerMsg.contains("작성한 글")) {
            return new ActionInfo("SHOW_MYPAGE", null);
        }

        // 북마크 관련
        if (lowerMsg.contains("북마크") || lowerMsg.contains("저장한") || 
            lowerMsg.contains("즐겨찾기") || lowerMsg.contains("보관")) {
            return new ActionInfo("SHOW_BOOKMARKS", null);
        }

        // 게시글 검색 관련
        if (lowerMsg.contains("검색") || lowerMsg.contains("찾아") || 
            lowerMsg.contains("보여줘") || lowerMsg.contains("추천")) {
            // 키워드 추출
            String keyword = extractSearchKeyword(message);
            if (keyword != null && !keyword.isEmpty()) {
                return new ActionInfo("SEARCH_POSTS", keyword);
            }
        }

        // AI 추천 관련
        if (lowerMsg.contains("ai 추천") || lowerMsg.contains("추천 받") || 
            lowerMsg.contains("맞춤 추천")) {
            return new ActionInfo("SHOW_RECOMMENDATIONS", null);
        }

        return null; // 일반 대화
    }

    /**
     * 검색 키워드 추출
     */
    private String extractSearchKeyword(String message) {
        // 간단한 키워드 추출 (실제로는 더 정교한 NLP 필요)
        String[] searchPatterns = {"검색", "찾아", "보여줘", "추천"};
        for (String pattern : searchPatterns) {
            int idx = message.toLowerCase().indexOf(pattern);
            if (idx > 0) {
                String keyword = message.substring(0, idx).trim();
                if (keyword.length() > 0 && keyword.length() < 50) {
                    return keyword;
                }
            }
        }
        // 패턴이 없으면 전체 메시지를 키워드로
        if (message.length() < 50) {
            return message.trim();
        }
        return null;
    }

    /**
     * Gemini API 호출
     */
    @SuppressWarnings("unchecked")
    private String callGeminiAPI(String userMessage, List<ChatMessage> context, 
                                  Integer userId, ActionInfo actionInfo) {
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            return "AI 챗봇을 사용하려면 Gemini API 키가 필요합니다. application.properties에 gemini.api.key를 설정해주세요.";
        }

        try {
            // 대화 맥락 구성
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append(WEBSITE_CONTEXT);
            if (userId != null) {
                contextBuilder.append("\n현재 로그인한 사용자입니다.");
            } else {
                contextBuilder.append("\n현재 비로그인 사용자입니다.");
            }

            // 최근 대화 맥락 추가
            if (!context.isEmpty()) {
                contextBuilder.append("\n\n## 최근 대화 맥락:\n");
                for (ChatMessage msg : context) {
                    if (msg.getRole() == ChatMessage.MessageRole.USER && !msg.getMessage().isEmpty()) {
                        contextBuilder.append("사용자: ").append(msg.getMessage()).append("\n");
                    } else if (msg.getRole() == ChatMessage.MessageRole.ASSISTANT && !msg.getResponse().isEmpty()) {
                        contextBuilder.append("AI: ").append(msg.getResponse()).append("\n");
                    }
                }
            }

            // 요청 구성 (Gemini API 형식)
            Map<String, Object> requestBody = new HashMap<>();
            
            // 시스템 인스트럭션
            Map<String, Object> systemInstruction = new HashMap<>();
            systemInstruction.put("parts", List.of(Map.of("text", contextBuilder.toString())));
            requestBody.put("systemInstruction", systemInstruction);
            
            // 사용자 메시지
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", userMessage)));
            contents.add(content);
            requestBody.put("contents", contents);

            // HTTP 요청
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // API 키는 URL 쿼리 파라미터로 전달 (헤더도 함께 사용 가능하지만 쿼리 파라미터가 더 안정적)
            // headers.set("x-goog-api-key", geminiApiKey);
            
            // URL 구성 (application.properties에 URL이 없으면 기본값 사용)
            String baseUrl;
            if (geminiApiUrl != null && !geminiApiUrl.isEmpty()) {
                baseUrl = geminiApiUrl;
            } else {
                // 기본 URL 구성 - 모델명을 URL 인코딩
                String model = geminiModel != null && !geminiModel.isEmpty() ? geminiModel : "gemini-1.5-pro";
                // 모델명에 하이픈이 있어도 그대로 사용 (Gemini API는 하이픈을 지원함)
                baseUrl = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent", model);
            }
            
            // API 키를 쿼리 파라미터로 추가
            // URL에 이미 key 파라미터가 있으면 추가하지 않음
            String url;
            if (baseUrl.contains("?key=") || baseUrl.contains("&key=")) {
                url = baseUrl;
            } else {
                String separator = baseUrl.contains("?") ? "&" : "?";
                url = baseUrl + separator + "key=" + geminiApiKey;
            }
            
            log.info("Gemini API 호출 - Base URL: {}, 최종 URL: {}, 모델: {}", baseUrl, url.replace(geminiApiKey, "***"), geminiModel);
            log.debug("요청 본문 크기: {} bytes", requestBody.toString().length());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 타입 안전한 응답 처리 (RestTemplate은 런타임에 타입을 확인)
            @SuppressWarnings({"unchecked", "rawtypes"})
            ResponseEntity<Map> rawResponse = getRestTemplate().postForEntity(url, request, Map.class);
            
            log.debug("Gemini API 응답 상태: {}", rawResponse.getStatusCode());
            log.debug("Gemini API 응답 본문: {}", rawResponse.getBody());

            // 응답 파싱
            if (rawResponse.getStatusCode() == HttpStatus.OK) {
                Map<?, ?> rawBody = rawResponse.getBody();
                if (rawBody == null) {
                    log.warn("Gemini API 응답 본문이 null입니다.");
                    return "AI 응답이 비어있습니다.";
                }
                
                // 타입 안전하게 변환 (rawBody는 null이 아님을 확인함)
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) rawBody;
                
                // 오류 확인
                if (body.containsKey("error")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> error = (Map<String, Object>) body.get("error");
                    String errorMessage = error != null ? (String) error.get("message") : "알 수 없는 오류";
                    log.error("Gemini API 오류: {}", errorMessage);
                    return "AI 서비스 오류: " + errorMessage;
                }
                
                Object candidatesObj = body.get("candidates");
                if (candidatesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) candidatesObj;
                    if (!candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        
                        // finishReason 확인
                        Object finishReason = candidate.get("finishReason");
                        if (finishReason != null && !"STOP".equals(finishReason)) {
                            log.warn("Gemini API finishReason: {}", finishReason);
                            if ("SAFETY".equals(finishReason)) {
                                return "안전 필터에 의해 응답이 차단되었습니다. 다른 질문을 시도해주세요.";
                            }
                        }
                        
                        Object contentObj = candidate.get("content");
                        if (contentObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> candidateContent = (Map<String, Object>) contentObj;
                            Object partsObj = candidateContent.get("parts");
                            if (partsObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> parts = (List<Map<String, Object>>) partsObj;
                                if (!parts.isEmpty()) {
                                    Object textObj = parts.get(0).get("text");
                                    if (textObj instanceof String) {
                                        String response = (String) textObj;
                                        log.debug("Gemini API 응답 성공: {}", response.substring(0, Math.min(100, response.length())));
                                        return response;
                                    } else {
                                        log.warn("응답 텍스트가 String이 아닙니다: {}", textObj.getClass());
                                    }
                                } else {
                                    log.warn("parts 리스트가 비어있습니다.");
                                }
                            } else {
                                log.warn("parts가 List가 아닙니다: {}", partsObj != null ? partsObj.getClass() : "null");
                            }
                        } else {
                            log.warn("content가 Map이 아닙니다: {}", contentObj != null ? contentObj.getClass() : "null");
                        }
                    } else {
                        log.warn("candidates 리스트가 비어있습니다.");
                    }
                } else {
                    log.warn("candidates가 List가 아닙니다: {}", candidatesObj != null ? candidatesObj.getClass() : "null");
                }
            } else {
                log.warn("Gemini API 응답 상태 코드: {}", rawResponse.getStatusCode());
            }

            log.warn("Gemini API 응답 파싱 실패. 응답 본문: {}", rawResponse.getBody());
            return "AI 응답을 생성하는 중 오류가 발생했습니다.";

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("Gemini API HTTP 오류: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 400) {
                return "API 요청 형식이 잘못되었습니다. 관리자에게 문의해주세요.";
            } else if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                return "API 키가 유효하지 않습니다. 관리자에게 문의해주세요.";
            } else if (e.getStatusCode().value() == 429) {
                return "API 사용량이 초과되었습니다. 잠시 후 다시 시도해주세요.";
            }
            return "AI 서비스에 일시적인 문제가 발생했습니다. (HTTP " + e.getStatusCode().value() + ")";
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Gemini API 네트워크 오류", e);
            return "네트워크 연결에 문제가 있습니다. 인터넷 연결을 확인해주세요.";
        } catch (Exception e) {
            log.error("Gemini API 호출 오류", e);
            log.error("오류 상세: {}", e.getMessage(), e);
            return "AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요. (오류: " + e.getClass().getSimpleName() + ")";
        }
    }

    /**
     * 게시글 유사도 검색
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchSimilarPosts(String keyword, Integer userId, int limit) {
        try {
            // 키워드로 게시글 검색
            var posts = postRepository.searchByKeyword(keyword, PageRequest.of(0, limit * 2));
            
            // 유사도 점수 계산 (간단한 키워드 매칭)
            List<Map<String, Object>> results = posts.getContent().stream()
                .map(post -> {
                    double score = calculateSimilarityScore(post, keyword);
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", post.getId());
                    item.put("title", post.getTitle());
                    item.put("category", post.getCategory());
                    item.put("tags", post.getTags());
                    item.put("viewCount", post.getViewCount());
                    item.put("likeCount", post.getLikeCount());
                    item.put("createdAt", post.getCreatedAt());
                    item.put("similarityScore", score);
                    return item;
                })
                .sorted((a, b) -> Double.compare(
                    (Double) b.get("similarityScore"), 
                    (Double) a.get("similarityScore")
                ))
                .limit(limit)
                .collect(Collectors.toList());

            return results;
        } catch (Exception e) {
            log.error("게시글 검색 오류", e);
            return new ArrayList<>();
        }
    }

    /**
     * 유사도 점수 계산
     */
    private double calculateSimilarityScore(Post post, String keyword) {
        double score = 0.0;
        String lowerKeyword = keyword.toLowerCase();
        
        // 제목 매칭
        if (post.getTitle() != null && post.getTitle().toLowerCase().contains(lowerKeyword)) {
            score += 10.0;
        }
        
        // 태그 매칭
        if (post.getTags() != null && post.getTags().toLowerCase().contains(lowerKeyword)) {
            score += 5.0;
        }
        
        // 카테고리 매칭
        if (post.getCategory() != null && post.getCategory().toLowerCase().contains(lowerKeyword)) {
            score += 3.0;
        }
        
        // 인기도 가중치
        if (post.getLikeCount() != null) {
            score += post.getLikeCount() * 0.1;
        }
        if (post.getViewCount() != null) {
            score += post.getViewCount() * 0.01;
        }
        
        return score;
    }

    /**
     * 대화 내역 조회
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getChatHistory(Integer userId, int limit) {
        var messages = chatMessageRepository.findRecentMessages(userId, limit);
        Collections.reverse(messages); // 시간순 정렬

        return messages.stream()
            .map(msg -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", msg.getId());
                item.put("role", msg.getRole().name());
                item.put("message", msg.getMessage());
                item.put("response", msg.getResponse());
                item.put("actionType", msg.getActionType());
                item.put("actionData", msg.getActionData());
                item.put("createdAt", msg.getCreatedAt().toString());
                return item;
            })
            .collect(Collectors.toList());
    }

    /**
     * 사용자의 대화 내역 삭제 (초기화)
     */
    @Transactional
    public void clearChatHistory(Integer userId) {
        try {
            if (userId != null) {
                // 사용자별 메시지 삭제
                List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
                chatMessageRepository.deleteAll(messages);
                log.info("사용자 ID {}의 대화 내역 {}개 삭제 완료", userId, messages.size());
            } else {
                // 비로그인 사용자 메시지 삭제 (user_id가 NULL인 메시지)
                List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(null);
                chatMessageRepository.deleteAll(messages);
                log.info("비로그인 사용자의 대화 내역 {}개 삭제 완료", messages.size());
            }
        } catch (Exception e) {
            log.error("대화 내역 삭제 오류", e);
            throw new RuntimeException("대화 내역 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 액션 정보 클래스
     */
    private static class ActionInfo {
        String actionType;
        String actionData;

        ActionInfo(String actionType, String actionData) {
            this.actionType = actionType;
            this.actionData = actionData;
        }
    }
}
