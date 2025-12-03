package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyGroupCalendarService {

    private final StudyGroupCalendarRepository calendarRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final com.example.studywithme.repository.UserRepository userRepository;
    private final com.example.studywithme.repository.StudyGroupMemberRepository memberRepository;

    /**
     * 일정 생성
     */
    @Transactional
    public StudyGroupCalendar createEvent(Long groupId, Integer userId, LocalDate eventDate,
                                          LocalTime eventTime, String title, String content,
                                          String eventType, String color) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        if (userId == null) {
            throw new RuntimeException("사용자 ID가 필요합니다.");
        }
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));

        // 멤버인지 확인 (Repository 사용)
        boolean isMember = memberRepository.existsByStudyGroup_IdAndUser_Id(groupId, userId);
        
        if (!isMember) {
            throw new RuntimeException("스터디 멤버만 일정을 추가할 수 있습니다.");
        }

        StudyGroupCalendar event = new StudyGroupCalendar();
        event.setStudyGroup(group);
        event.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다.")));
        event.setEventDate(eventDate);
        event.setEventTime(eventTime);
        event.setTitle(title);
        event.setContent(content);
        event.setEventType(eventType != null ? eventType : "STUDY");
        event.setColor(color != null ? color : "#3b82f6");

        return calendarRepository.save(event);
    }

    /**
     * 일정 수정
     */
    @Transactional
    public StudyGroupCalendar updateEvent(Long eventId, Integer userId, LocalDate eventDate,
                                          LocalTime eventTime, String title, String content,
                                          String eventType, String color) {
        if (eventId == null) {
            throw new RuntimeException("일정 ID가 필요합니다.");
        }
        StudyGroupCalendar event = calendarRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        // 작성자 또는 팀장만 수정 가능
        boolean isCreator = event.getUser().getId().equals(userId);
        boolean isLeader = event.getStudyGroup().getCreator().getId().equals(userId);
        
        if (!isCreator && !isLeader) {
            throw new RuntimeException("일정을 수정할 권한이 없습니다.");
        }

        if (eventDate != null) event.setEventDate(eventDate);
        if (eventTime != null) event.setEventTime(eventTime);
        if (title != null) event.setTitle(title);
        if (content != null) event.setContent(content);
        if (eventType != null) event.setEventType(eventType);
        if (color != null) event.setColor(color);

        return calendarRepository.save(event);
    }

    /**
     * 일정 삭제
     */
    @Transactional
    public void deleteEvent(Long eventId, Integer userId) {
        if (eventId == null) {
            throw new RuntimeException("일정 ID가 필요합니다.");
        }
        StudyGroupCalendar event = calendarRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

        // 작성자 또는 팀장만 삭제 가능
        boolean isCreator = event.getUser().getId().equals(userId);
        boolean isLeader = event.getStudyGroup().getCreator().getId().equals(userId);
        
        if (!isCreator && !isLeader) {
            throw new RuntimeException("일정을 삭제할 권한이 없습니다.");
        }

        calendarRepository.delete(event);
    }

    /**
     * 특정 날짜의 일정 조회
     */
    @Transactional(readOnly = true)
    public List<StudyGroupCalendar> getEventsByDate(Long groupId, LocalDate date) {
        return calendarRepository.findByStudyGroup_IdAndEventDate(groupId, date);
    }

    /**
     * 특정 월의 일정 조회
     */
    @Transactional(readOnly = true)
    public List<StudyGroupCalendar> getEventsByMonth(Long groupId, int year, int month) {
        return calendarRepository.findByStudyGroupAndMonth(groupId, year, month);
    }
}
