package com.example.studywithme.studygroup.controller;

import com.example.studywithme.studygroup.entity.StudyGroup;
import com.example.studywithme.studygroup.service.StudyGroupService;
import com.example.studywithme.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.List;

/**
 * 스터디 그룹 화면(목록·상세).
 */
@Controller
@RequiredArgsConstructor
public class StudyGroupWebController {

    private final StudyGroupService studyGroupService;

    @GetMapping("/study-groups")
    public String studyGroups(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        try {
            List<StudyGroup> groups = studyGroupService.getActiveGroupsByUser(loginUser.getId());
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("groups", groups != null ? groups : Collections.emptyList());
        } catch (Exception e) {
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("groups", Collections.emptyList());
        }
        return "study-groups";
    }

    @GetMapping("/study-groups/{id}")
    public String studyGroupDetail(@PathVariable Long id, HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/auth?error=login_required";
        }
        try {
            StudyGroup group = studyGroupService.getGroupById(id);
            model.addAttribute("loginUser", loginUser);
            model.addAttribute("group", group);
            model.addAttribute("members", studyGroupService.getGroupMembers(id));
            return "study-group-detail";
        } catch (RuntimeException e) {
            return "redirect:/study-groups?error=" + e.getMessage();
        }
    }
}
