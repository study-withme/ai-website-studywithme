package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PostApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    /**
     * 지원 승인 시 스터디 그룹 생성 또는 멤버 추가
     * 개설자 포함하여 최소 2명 이상이 모였을 때만 그룹 생성
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

        // 승인된 지원서 개수 확인 (개설자 제외)
        // flush() 후이므로 현재 승인된 지원서가 포함되어야 함
        long acceptedCount = applicationRepository.countByPost_IdAndStatus(
                postId, PostApplication.ApplicationStatus.ACCEPTED);
        
        log.info("승인된 지원서 개수: postId={}, acceptedCount={}, acceptedUserId={}", 
                postId, acceptedCount, acceptedUserId);
        
        // acceptedCount가 0이면 문제가 있을 수 있음 (flush() 후인데도 0이면)
        // 하지만 현재 지원서는 이미 ACCEPTED 상태로 저장되었으므로, 
        // count 쿼리가 제대로 작동하지 않을 수 있음
        // 따라서 현재 지원서를 직접 확인
        if (acceptedCount == 0) {
            log.warn("승인된 지원서가 0개로 조회되었습니다. 현재 지원서를 직접 확인합니다.");
            // 현재 지원서를 다시 조회하여 확인
            PostApplication currentApp = applicationRepository.findByPost_IdAndUser_Id(postId, acceptedUserId)
                    .orElse(null);
            if (currentApp != null && currentApp.getStatus() == PostApplication.ApplicationStatus.ACCEPTED) {
                acceptedCount = 1; // 현재 지원서만이라도 1로 설정
                log.info("현재 지원서는 ACCEPTED 상태입니다. acceptedCount를 1로 설정합니다.");
            } else {
                log.error("현재 지원서가 ACCEPTED 상태가 아닙니다. applicationId={}, status={}", 
                        currentApp != null ? currentApp.getId() : null,
                        currentApp != null ? currentApp.getStatus() : "null");
                throw new RuntimeException("지원서 상태가 올바르지 않습니다.");
            }
        }

        // 스터디 그룹 조회 (여러 방법 시도)
        StudyGroup group = null;
        try {
            // 방법 1: post_id로 조회
            group = studyGroupRepository.findByPostId(postId).orElse(null);
        } catch (Exception e) {
            log.warn("post_id로 스터디 그룹 조회 실패: postId={}, error={}", postId, e.getMessage());
        }
        
        // 방법 2: post_id로 못 찾았으면 creator_id로 조회
        if (group == null) {
            try {
                List<StudyGroup> groups = studyGroupRepository.findByCreator_IdAndStatusOrderByCreatedAtDesc(
                        postOwnerId, StudyGroup.GroupStatus.ACTIVE);
                // 같은 게시글과 연관된 그룹 찾기
                group = groups.stream()
                        .filter(g -> g.getPost() != null && g.getPost().getId().equals(postId))
                        .findFirst()
                        .orElse(null);
            } catch (Exception e) {
                log.warn("creator_id로 스터디 그룹 조회 실패: postId={}, error={}", postId, e.getMessage());
            }
        }
        
        // 그룹이 없고, 승인된 지원자가 1명 이상이면 그룹 생성 (개설자 포함 2명 이상)
        if (group == null && acceptedCount >= 1) {
            log.info("스터디 그룹 생성 조건 충족: postId={}, acceptedCount={} (개설자 포함 2명 이상)", 
                    postId, acceptedCount);
            
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
            group.setCurrentMembers(1); // 리더만 (아직)
            
            try {
                group = studyGroupRepository.save(group);
                log.info("스터디 그룹 생성 성공: groupId={}, postId={}, title={}", 
                        group.getId(), postId, group.getTitle());
            } catch (Exception e) {
                log.error("스터디 그룹 저장 실패: postId={}, error={}", postId, e.getMessage(), e);
                throw new RuntimeException("스터디 그룹 저장에 실패했습니다: " + e.getMessage(), e);
            }

            // 리더(개설자)를 멤버로 추가
            try {
                // 이미 멤버인지 확인
                if (!memberRepository.existsByStudyGroup_IdAndUser_Id(group.getId(), postOwnerId)) {
                StudyGroupMember leaderMember = new StudyGroupMember();
                leaderMember.setStudyGroup(group);
                leaderMember.setUser(post.getUser());
                leaderMember.setRole(StudyGroupMember.MemberRole.CREATOR);
                leaderMember.setStatus(StudyGroupMember.MemberStatus.ACTIVE);
                    // leftAt은 insertable = false이므로 자동으로 NULL로 설정됨
                memberRepository.save(leaderMember);
                log.info("리더 멤버 추가 성공: groupId={}, userId={}", group.getId(), post.getUser().getId());
                } else {
                    log.info("리더는 이미 멤버로 등록되어 있음: groupId={}, userId={}", group.getId(), postOwnerId);
                }
            } catch (Exception e) {
                log.error("리더 멤버 추가 실패: groupId={}, userId={}, error={}", 
                        group.getId(), post.getUser().getId(), e.getMessage(), e);
                e.printStackTrace(); // 상세 에러 정보 출력
                throw new RuntimeException("리더 멤버 추가에 실패했습니다: " + e.getMessage(), e);
            }
        } else if (group == null) {
            // 그룹이 없고 승인된 지원자가 1명 미만이면 그룹 생성하지 않음
            log.info("스터디 그룹 생성 조건 미충족: postId={}, acceptedCount={} (개설자 포함 2명 이상 필요)", 
                    postId, acceptedCount);
            return null; // 그룹이 생성되지 않았음을 반환
        }

        // 그룹이 null이면 안전하게 반환
        if (group == null) {
            log.warn("그룹이 null입니다. postId={}, acceptedCount={}", postId, acceptedCount);
            return null;
        }

        // 이미 멤버인지 확인
        if (memberRepository.existsByStudyGroup_IdAndUser_Id(group.getId(), acceptedUserId)) {
            log.info("이미 멤버로 등록되어 있음: groupId={}, userId={}", group.getId(), acceptedUserId);
            return group; // 이미 멤버면 그대로 반환
        }

        // 승인된 사용자를 멤버로 추가
        try {
            StudyGroupMember member = new StudyGroupMember();
            member.setStudyGroup(group);
            member.setUser(application.getUser());
            member.setRole(StudyGroupMember.MemberRole.MEMBER);
            member.setStatus(StudyGroupMember.MemberStatus.ACTIVE);
            // leftAt은 insertable = false이므로 자동으로 NULL로 설정됨
            memberRepository.save(member);
            log.info("멤버 추가 성공: groupId={}, userId={}", group.getId(), acceptedUserId);
        } catch (Exception e) {
            log.error("멤버 추가 실패: groupId={}, userId={}, error={}", 
                    group.getId(), acceptedUserId, e.getMessage(), e);
            e.printStackTrace(); // 상세 에러 정보 출력
            throw new RuntimeException("멤버 추가에 실패했습니다: " + e.getMessage(), e);
        }

        // 멤버 수 업데이트 (실제 멤버 수로 재계산)
        try {
            long actualMemberCount = memberRepository.countByStudyGroup_IdAndStatus(
                    group.getId(), StudyGroupMember.MemberStatus.ACTIVE);
            group.setCurrentMembers((int) actualMemberCount);
            studyGroupRepository.save(group);
            log.info("멤버 수 업데이트 성공: groupId={}, currentMembers={}", group.getId(), group.getCurrentMembers());
        } catch (Exception e) {
            log.error("멤버 수 업데이트 실패: groupId={}, error={}", group.getId(), e.getMessage(), e);
            // 멤버 수 업데이트 실패는 치명적이지 않으므로 경고만 남김
        }

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
        try {
            // 방법 1: 멤버로 참여한 그룹 조회
            List<StudyGroup> memberGroups = studyGroupRepository.findActiveGroupsByUserId(
                    userId,
                    StudyGroupMember.MemberStatus.ACTIVE,
                    StudyGroup.GroupStatus.ACTIVE);
            
            // 방법 2: 리더(creator)로 참여한 그룹 조회
            List<StudyGroup> creatorGroups = studyGroupRepository.findByCreator_IdAndStatusOrderByCreatedAtDesc(
                    userId,
                    StudyGroup.GroupStatus.ACTIVE);
            
            // 두 리스트를 합치고 중복 제거
            java.util.Set<Long> groupIds = new java.util.HashSet<>();
            List<StudyGroup> allGroups = new java.util.ArrayList<>();
            
            // 멤버로 참여한 그룹 추가
            if (memberGroups != null) {
                for (StudyGroup group : memberGroups) {
                    if (!groupIds.contains(group.getId())) {
                        groupIds.add(group.getId());
                        allGroups.add(group);
                    }
                }
            }
            
            // 리더로 참여한 그룹 추가 (중복 제거)
            if (creatorGroups != null) {
                for (StudyGroup group : creatorGroups) {
                    if (!groupIds.contains(group.getId())) {
                        groupIds.add(group.getId());
                        allGroups.add(group);
                    }
                }
            }
            
            // Lazy 로딩 방지를 위해 members 초기화
            allGroups.forEach(group -> {
                if (group.getMembers() != null) {
                    group.getMembers().size(); // Lazy 초기화
                }
            });
            
            log.info("사용자 {}의 활성 스터디 그룹 조회: {}개 (멤버: {}, 리더: {})", 
                    userId, allGroups.size(), 
                    memberGroups != null ? memberGroups.size() : 0,
                    creatorGroups != null ? creatorGroups.size() : 0);
            
            return allGroups;
        } catch (Exception e) {
            // 에러 발생 시 로그 출력
            log.error("스터디 그룹 조회 실패: userId={}, error={}", userId, e.getMessage(), e);
            e.printStackTrace(); // 디버깅용
            return java.util.Collections.emptyList();
        }
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

