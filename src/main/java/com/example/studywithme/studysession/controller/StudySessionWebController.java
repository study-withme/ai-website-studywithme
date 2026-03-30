package com.example.studywithme.studysession.controller;

import com.example.studywithme.studygroup.entity.StudyGroup;
import com.example.studywithme.studygroup.service.StudyGroupService;
import com.example.studywithme.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 그룹 내 학습 세션 화면.
 */
@Controller
@RequiredArgsConstructor
public class StudySessionWebController {

    private final StudyGroupService studyGroupService;

    @GetMapping("/study-groups/{id}/session")
    public String studySession(@PathVariable Long id, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        try {
            StudyGroup group = studyGroupService.getGroupById(id);
            boolean isMember = studyGroupService.getGroupMembers(id).stream()
                    .anyMatch(m -> m.getUser().getId().equals(loginUser.getId()));
            if (!isMember) {
                return "redirect:/study-groups?error=스터디 멤버만 접근할 수 있습니다.";
            }
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("group", group);
            return "study-session";
        } catch (RuntimeException e) {
            return "redirect:/study-groups?error=" + e.getMessage();
        }
    }
}
