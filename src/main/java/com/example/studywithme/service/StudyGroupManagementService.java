package com.example.studywithme.service;

import com.example.studywithme.entity.*;
import com.example.studywithme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 스터디 그룹 관리 서비스 (커리큘럼, 리소스, 댓글 등)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyGroupManagementService {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository memberRepository;
    private final StudyGroupCurriculumRepository curriculumRepository;
    private final StudyGroupResourceRepository resourceRepository;
    private final StudyGroupCommentRepository commentRepository;
    private final com.example.studywithme.repository.UserRepository userRepository;

    // ========== 커리큘럼 관리 ==========
    
    @Transactional
    public StudyGroupCurriculum createCurriculum(Long groupId, Integer userId, 
                                                  Integer weekNumber, String title, 
                                                  String description,
                                                  java.time.LocalDate startDate,
                                                  java.time.LocalDate endDate) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));
        
        // 권한 확인 (리더만)
        if (!group.getCreator().getId().equals(userId)) {
            throw new RuntimeException("커리큘럼을 생성할 권한이 없습니다.");
        }
        
        StudyGroupCurriculum curriculum = new StudyGroupCurriculum();
        curriculum.setStudyGroup(group);
        curriculum.setWeekNumber(weekNumber);
        curriculum.setTitle(title);
        curriculum.setDescription(description);
        curriculum.setStartDate(startDate);
        curriculum.setEndDate(endDate);
        curriculum.setStatus(StudyGroupCurriculum.CurriculumStatus.PENDING);
        
        return curriculumRepository.save(curriculum);
    }
    
    @Transactional
    public StudyGroupCurriculum updateCurriculum(Long curriculumId, Integer userId,
                                                  String title, String description,
                                                  StudyGroupCurriculum.CurriculumStatus status) {
        if (curriculumId == null) {
            throw new RuntimeException("커리큘럼 ID가 필요합니다.");
        }
        StudyGroupCurriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new RuntimeException("커리큘럼을 찾을 수 없습니다."));
        
        // 권한 확인
        if (!curriculum.getStudyGroup().getCreator().getId().equals(userId)) {
            throw new RuntimeException("커리큘럼을 수정할 권한이 없습니다.");
        }
        
        if (title != null) curriculum.setTitle(title);
        if (description != null) curriculum.setDescription(description);
        if (status != null) curriculum.setStatus(status);
        
        return curriculumRepository.save(curriculum);
    }
    
    @Transactional
    public void deleteCurriculum(Long curriculumId, Integer userId) {
        if (curriculumId == null) {
            throw new RuntimeException("커리큘럼 ID가 필요합니다.");
        }
        StudyGroupCurriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new RuntimeException("커리큘럼을 찾을 수 없습니다."));
        
        // 권한 확인
        if (!curriculum.getStudyGroup().getCreator().getId().equals(userId)) {
            throw new RuntimeException("커리큘럼을 삭제할 권한이 없습니다.");
        }
        
        curriculumRepository.delete(curriculum);
    }
    
    public List<StudyGroupCurriculum> getCurriculums(Long groupId) {
        return curriculumRepository.findByStudyGroup_IdOrderByWeekNumberAsc(groupId);
    }
    
    // ========== 리소스 관리 ==========
    
    @Transactional
    public StudyGroupResource createResource(Long groupId, Integer userId,
                                             StudyGroupResource.ResourceType resourceType,
                                             String name, String url, String description) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        if (userId == null) {
            throw new RuntimeException("사용자 ID가 필요합니다.");
        }
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));
        
        // 멤버 확인
        if (!memberRepository.existsByStudyGroup_IdAndUser_Id(groupId, userId)) {
            throw new RuntimeException("스터디 그룹 멤버만 리소스를 추가할 수 있습니다.");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        StudyGroupResource resource = new StudyGroupResource();
        resource.setStudyGroup(group);
        resource.setResourceType(resourceType);
        resource.setName(name);
        resource.setUrl(url);
        resource.setDescription(description);
        resource.setCreatedBy(user);
        
        return resourceRepository.save(resource);
    }
    
    @Transactional
    public StudyGroupResource updateResource(Long resourceId, Integer userId,
                                            String name, String url, String description) {
        if (resourceId == null) {
            throw new RuntimeException("리소스 ID가 필요합니다.");
        }
        StudyGroupResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("리소스를 찾을 수 없습니다."));
        
        // 권한 확인 (생성자 또는 리더)
        boolean isCreator = resource.getCreatedBy() != null && 
                           resource.getCreatedBy().getId().equals(userId);
        boolean isLeader = resource.getStudyGroup().getCreator().getId().equals(userId);
        
        if (!isCreator && !isLeader) {
            throw new RuntimeException("리소스를 수정할 권한이 없습니다.");
        }
        
        if (name != null) resource.setName(name);
        if (url != null) resource.setUrl(url);
        if (description != null) resource.setDescription(description);
        
        return resourceRepository.save(resource);
    }
    
    @Transactional
    public void deleteResource(Long resourceId, Integer userId) {
        if (resourceId == null) {
            throw new RuntimeException("리소스 ID가 필요합니다.");
        }
        StudyGroupResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("리소스를 찾을 수 없습니다."));
        
        // 권한 확인
        boolean isCreator = resource.getCreatedBy() != null && 
                           resource.getCreatedBy().getId().equals(userId);
        boolean isLeader = resource.getStudyGroup().getCreator().getId().equals(userId);
        
        if (!isCreator && !isLeader) {
            throw new RuntimeException("리소스를 삭제할 권한이 없습니다.");
        }
        
        resourceRepository.delete(resource);
    }
    
    public List<StudyGroupResource> getResources(Long groupId) {
        return resourceRepository.findByStudyGroup_IdOrderByCreatedAtDesc(groupId);
    }
    
    public List<StudyGroupResource> getResourcesByType(Long groupId, 
                                                       StudyGroupResource.ResourceType resourceType) {
        return resourceRepository.findByStudyGroup_IdAndResourceTypeOrderByCreatedAtDesc(groupId, resourceType);
    }
    
    // ========== 댓글 관리 ==========
    
    @Transactional
    public StudyGroupComment createComment(Long groupId, Integer userId, String content, Long parentCommentId) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        if (userId == null) {
            throw new RuntimeException("사용자 ID가 필요합니다.");
        }
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));
        
        // 멤버 확인
        if (!memberRepository.existsByStudyGroup_IdAndUser_Id(groupId, userId)) {
            throw new RuntimeException("스터디 그룹 멤버만 댓글을 작성할 수 있습니다.");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        StudyGroupComment comment = new StudyGroupComment();
        comment.setStudyGroup(group);
        comment.setUser(user);
        comment.setContent(content);
        
        if (parentCommentId != null) {
            StudyGroupComment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("부모 댓글을 찾을 수 없습니다."));
            comment.setParentComment(parent);
        }
        
        return commentRepository.save(comment);
    }
    
    @Transactional
    public StudyGroupComment updateComment(Long commentId, Integer userId, String content) {
        if (commentId == null) {
            throw new RuntimeException("댓글 ID가 필요합니다.");
        }
        StudyGroupComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        
        // 권한 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("댓글을 수정할 권한이 없습니다.");
        }
        
        comment.setContent(content);
        return commentRepository.save(comment);
    }
    
    @Transactional
    public void deleteComment(Long commentId, Integer userId) {
        if (commentId == null) {
            throw new RuntimeException("댓글 ID가 필요합니다.");
        }
        StudyGroupComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        
        // 권한 확인 (작성자 또는 리더)
        boolean isAuthor = comment.getUser().getId().equals(userId);
        boolean isLeader = comment.getStudyGroup().getCreator().getId().equals(userId);
        
        if (!isAuthor && !isLeader) {
            throw new RuntimeException("댓글을 삭제할 권한이 없습니다.");
        }
        
        comment.setDeleted(true);
        commentRepository.save(comment);
    }
    
    public List<StudyGroupComment> getComments(Long groupId) {
        return commentRepository.findTopLevelCommentsByStudyGroupId(groupId);
    }
    
    // ========== 스터디 그룹 수정/삭제 ==========
    
    @Transactional
    public StudyGroup updateStudyGroup(Long groupId, Integer userId, String title, 
                                       String description, Integer maxMembers) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));
        
        // 권한 확인 (리더만)
        if (!group.getCreator().getId().equals(userId)) {
            throw new RuntimeException("스터디 그룹을 수정할 권한이 없습니다.");
        }
        
        if (title != null) group.setTitle(title);
        if (description != null) group.setDescription(description);
        if (maxMembers != null) {
            if (maxMembers < group.getCurrentMembers()) {
                throw new RuntimeException("최대 인원은 현재 인원보다 작을 수 없습니다.");
            }
            group.setMaxMembers(maxMembers);
        }
        
        return studyGroupRepository.save(group);
    }
    
    @Transactional
    public void deleteStudyGroup(Long groupId, Integer userId) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));
        
        // 권한 확인 (리더만)
        if (!group.getCreator().getId().equals(userId)) {
            throw new RuntimeException("스터디 그룹을 삭제할 권한이 없습니다.");
        }
        
        group.setStatus(StudyGroup.GroupStatus.CLOSED);
        studyGroupRepository.save(group);
    }

    // ========== 멤버 탈퇴 / 추방 ==========

    /**
     * 사용자가 스스로 스터디 그룹에서 탈퇴
     */
    @Transactional
    public void leaveStudyGroup(Long groupId, Integer userId) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        if (userId == null) {
            throw new RuntimeException("사용자 ID가 필요합니다.");
        }

        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));

        StudyGroupMember member = memberRepository.findByStudyGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹 멤버가 아닙니다."));

        // 개설자는 탈퇴할 수 없고, 그룹을 종료하거나 다른 리더에게 위임해야 함
        if (member.getRole() == StudyGroupMember.MemberRole.CREATOR) {
            throw new RuntimeException("스터디 개설자는 탈퇴할 수 없습니다. 그룹을 종료하거나 다른 리더에게 위임해 주세요.");
        }

        // 이미 나간 멤버면 아무 작업도 하지 않음
        if (member.getStatus() != StudyGroupMember.MemberStatus.ACTIVE) {
            return;
        }

        member.setStatus(StudyGroupMember.MemberStatus.LEFT);
        member.setLeftAt(LocalDateTime.now());
        memberRepository.save(member);

        // 현재 인원 재계산
        long activeCount = memberRepository.countByStudyGroup_IdAndStatus(
                groupId, StudyGroupMember.MemberStatus.ACTIVE);
        group.setCurrentMembers((int) activeCount);

        // 인원 수에 따라 상태 업데이트 (정원에서 빠졌으면 FULL 해제)
        if (group.getMaxMembers() != null
                && activeCount < group.getMaxMembers()
                && group.getStatus() == StudyGroup.GroupStatus.FULL) {
            group.setStatus(StudyGroup.GroupStatus.ACTIVE);
        }

        studyGroupRepository.save(group);
    }

    /**
     * 리더가 특정 멤버를 추방
     */
    @Transactional
    public void removeMember(Long groupId, Integer leaderId, Integer targetUserId) {
        if (groupId == null) {
            throw new RuntimeException("스터디 그룹 ID가 필요합니다.");
        }
        if (leaderId == null || targetUserId == null) {
            throw new RuntimeException("사용자 ID가 필요합니다.");
        }

        StudyGroup group = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("스터디 그룹을 찾을 수 없습니다."));

        // 리더 권한 확인
        if (!group.getCreator().getId().equals(leaderId)) {
            throw new RuntimeException("멤버를 관리할 권한이 없습니다.");
        }

        StudyGroupMember member = memberRepository.findByStudyGroup_IdAndUser_Id(groupId, targetUserId)
                .orElseThrow(() -> new RuntimeException("해당 사용자는 스터디 그룹 멤버가 아닙니다."));

        // 리더 자신 또는 다른 리더는 추방할 수 없음
        if (member.getRole() == StudyGroupMember.MemberRole.CREATOR) {
            throw new RuntimeException("스터디 개설자는 추방할 수 없습니다.");
        }

        if (member.getStatus() != StudyGroupMember.MemberStatus.ACTIVE) {
            return;
        }

        member.setStatus(StudyGroupMember.MemberStatus.REMOVED);
        member.setLeftAt(LocalDateTime.now());
        memberRepository.save(member);

        long activeCount = memberRepository.countByStudyGroup_IdAndStatus(
                groupId, StudyGroupMember.MemberStatus.ACTIVE);
        group.setCurrentMembers((int) activeCount);

        if (group.getMaxMembers() != null
                && activeCount < group.getMaxMembers()
                && group.getStatus() == StudyGroup.GroupStatus.FULL) {
            group.setStatus(StudyGroup.GroupStatus.ACTIVE);
        }

        studyGroupRepository.save(group);
    }
}
