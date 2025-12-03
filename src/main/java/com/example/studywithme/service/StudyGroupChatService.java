package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyGroupChatService {

    private final StudyGroupChatRepository chatRepository;
    private final StudyGroupRepository studyGroupRepository;

    /**
     * 메시지 전송
     */
    @Transactional
    public StudyGroupChat sendMessage(Long groupId, Integer userId, String message, 
                                      StudyGroupChat.MessageType messageType) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));

        StudyGroupChat chat = new StudyGroupChat();
        chat.setStudyGroup(group);
        chat.setUser(group.getMembers().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .map(StudyGroupMember::getUser)
                .orElseThrow(() -> new RuntimeException("스터디 멤버가 아닙니다.")));
        chat.setMessage(message);
        chat.setMessageType(messageType);

        return chatRepository.save(chat);
    }

    /**
     * 시스템 메시지 전송
     */
    @Transactional
    public StudyGroupChat sendSystemMessage(Long groupId, String message) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));

        StudyGroupChat chat = new StudyGroupChat();
        chat.setStudyGroup(group);
        chat.setUser(group.getCreator()); // 시스템 메시지는 그룹 생성자로 표시
        chat.setMessage(message);
        chat.setMessageType(StudyGroupChat.MessageType.SYSTEM);

        return chatRepository.save(chat);
    }

    /**
     * 최근 메시지 조회
     */
    public List<StudyGroupChat> getRecentMessages(Long groupId, int limit) {
        List<StudyGroupChat> messages = chatRepository.findRecentMessagesByGroup(groupId);
        return messages.stream().limit(limit).toList();
    }

    /**
     * 특정 시간 이후의 메시지 조회
     */
    public List<StudyGroupChat> getMessagesSince(Long groupId, LocalDateTime since) {
        return chatRepository.findMessagesSince(groupId, since);
    }
}
