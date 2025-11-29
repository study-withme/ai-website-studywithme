package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PostApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    /**
     * 지원 승인 시 스터디 그룹 생성 또는 멤버 추가
     */
    @Transactional
    public StudyGroup createOrAddMember(Long postId, Integer acceptedUserId, Integer postOwnerId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 권한 확인
        if (!post.getUser().getId().equals(postOwnerId)) {
            throw new RuntimeException("스터디 그룹을 생성할 권한이 없습니다.");
        }

        // 지원 내역 확인
        PostApplication application = applicationRepository.findByPost_IdAndUser_Id(postId, acceptedUserId)
                .orElseThrow(() -> new RuntimeException("지원 내역을 찾을 수 없습니다."));

        if (application.getStatus() != PostApplication.ApplicationStatus.ACCEPTED) {
            throw new RuntimeException("승인된 지원만 그룹에 추가할 수 있습니다.");
        }

        // 스터디 그룹이 없으면 생성
        StudyGroup group = studyGroupRepository.findByPostId(postId).orElse(null);
        
        if (group == null) {
            group = new StudyGroup();
            group.setPost(post);
            group.setCreator(post.getUser());
            group.setTitle(post.getTitle());
            group.setDescription(post.getContent());
            group.setCategory(post.getCategory());
            group.setTags(post.getTags());
            group.setStatus(StudyGroup.GroupStatus.ACTIVE);
            group.setStartDate(java.time.LocalDate.now());
            // 게시글의 정원 정보가 없으면 기본값 10
            group.setMaxMembers(10);
            group.setCurrentMembers(1); // 리더만
            
            group = studyGroupRepository.save(group);

            // 리더를 멤버로 추가
            StudyGroupMember leaderMember = new StudyGroupMember();
            leaderMember.setStudyGroup(group);
            leaderMember.setUser(post.getUser());
            leaderMember.setRole(StudyGroupMember.MemberRole.CREATOR);
            leaderMember.setStatus(StudyGroupMember.MemberStatus.ACTIVE);
            memberRepository.save(leaderMember);
        }

        // 이미 멤버인지 확인
        if (memberRepository.existsByStudyGroup_IdAndUser_Id(group.getId(), acceptedUserId)) {
            return group; // 이미 멤버면 그대로 반환
        }

        // 승인된 사용자를 멤버로 추가
        StudyGroupMember member = new StudyGroupMember();
        member.setStudyGroup(group);
        member.setUser(application.getUser());
        member.setRole(StudyGroupMember.MemberRole.MEMBER);
        member.setStatus(StudyGroupMember.MemberStatus.ACTIVE);
        memberRepository.save(member);

        // 멤버 수 업데이트
        group.setCurrentMembers(group.getCurrentMembers() + 1);
        studyGroupRepository.save(group);

        // 알림: 승인된 사용자에게 그룹 참여 알림
        try {
            notificationService.notify(
                    acceptedUserId,
                    "STUDY_GROUP_JOINED",
                    "스터디 그룹 참여",
                    "'" + post.getTitle() + "' 스터디 그룹에 참여하셨습니다!",
                    "/study-groups/" + group.getId()
            );
        } catch (Exception ignored) {}

        return group;
    }

    /**
     * 사용자가 참여한 활성 스터디 그룹 목록 조회
     */
    @Transactional(readOnly = true)
    public List<StudyGroup> getActiveGroupsByUser(Integer userId) {
        return studyGroupRepository.findActiveGroupsByUserId(userId);
    }

    /**
     * 스터디 그룹 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public StudyGroup getGroupById(Long groupId) {
        return studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));
    }

    /**
     * 스터디 그룹 멤버 목록 조회
     */
    @Transactional(readOnly = true)
    public List<StudyGroupMember> getGroupMembers(Long groupId) {
        return memberRepository.findByStudyGroup_IdAndStatus(groupId, StudyGroupMember.MemberStatus.ACTIVE);
    }
}

