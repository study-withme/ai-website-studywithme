package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserOnlineStatusService {

    private final UserOnlineStatusRepository onlineStatusRepository;
    private final com.example.studywithme.repository.UserRepository userRepository;

    /**
     * 온라인 상태 업데이트
     */
    @Transactional
    public UserOnlineStatus updateStatus(Integer userId, UserOnlineStatus.OnlineStatus status,
                                         Long studyGroupId, String statusMessage) {
        if (userId == null) {
            throw new RuntimeException("사용자 ID가 필요합니다.");
        }
        UserOnlineStatus onlineStatus = onlineStatusRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    UserOnlineStatus newStatus = new UserOnlineStatus();
                    newStatus.setUser(userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")));
                    return newStatus;
                });

        onlineStatus.setIsOnline(status != UserOnlineStatus.OnlineStatus.OFFLINE);
        onlineStatus.setCurrentStatus(status);
        onlineStatus.setLastActiveTime(LocalDateTime.now());
        onlineStatus.setCurrentStudyGroupId(studyGroupId);
        // statusMessage가 null이 아니면 업데이트 (하트비트 등으로 null 전달 시 기존 메시지 보존)
        if (statusMessage != null) {
            onlineStatus.setStatusMessage(statusMessage);
        }

        return onlineStatusRepository.save(onlineStatus);
    }

    /**
     * 활동 시간 업데이트 (하트비트)
     */
    @Transactional
    public void updateLastActiveTime(Integer userId) {
        Optional<UserOnlineStatus> statusOpt = onlineStatusRepository.findByUser_Id(userId);
        if (statusOpt.isPresent()) {
            UserOnlineStatus status = statusOpt.get();
            status.setLastActiveTime(LocalDateTime.now());
            onlineStatusRepository.save(status);
        }
    }

    /**
     * 스터디 그룹의 온라인 멤버 목록
     */
    public List<UserOnlineStatus> getOnlineMembers(Long groupId) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5); // 5분 이내 활동
        return onlineStatusRepository.findOnlineMembersByGroup(groupId, threshold);
    }

    /**
     * 자리비움 상태 업데이트 (스케줄러에서 호출)
     */
    @Transactional
    public void updateAwayUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<UserOnlineStatus> awayUsers = onlineStatusRepository.findAwayUsers(threshold);
        
        for (UserOnlineStatus status : awayUsers) {
            if (status.getCurrentStatus() == UserOnlineStatus.OnlineStatus.ONLINE) {
                status.setCurrentStatus(UserOnlineStatus.OnlineStatus.AWAY);
                onlineStatusRepository.save(status);
            }
        }
    }

    /**
     * 사용자 상태 조회
     */
    public Optional<UserOnlineStatus> getUserStatus(Integer userId) {
        return onlineStatusRepository.findByUser_Id(userId);
    }
}
