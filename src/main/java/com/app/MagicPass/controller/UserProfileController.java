package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.MembershipService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserProfileController {

    private final MembershipService membershipService;

    public UserProfileController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");

        // If not logged in, send back to login
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Get user's active membership if exists
        Membership currentMembership = membershipService.getActiveMembership(currentUser.getId());

        model.addAttribute("user", currentUser);
        model.addAttribute("currentMembership", currentMembership);
        return "profile";   // 👈 match templates/profile.html
    }
}
