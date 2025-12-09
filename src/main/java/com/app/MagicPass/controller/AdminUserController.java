package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.User;
import com.app.MagicPass.repository.MembershipRepository;
import com.app.MagicPass.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public AdminUserController(UserRepository userRepository, MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping
    public String listUsers(HttpSession session, Model model) {
        // Check if user is logged in as staff
        Boolean isStaff = (Boolean) session.getAttribute("isStaff");
        if (isStaff == null || !isStaff) {
            return "redirect:/login";
        }

        // Get all users from database
        List<User> userList = userRepository.findAll();

        // Get all active memberships
        List<Membership> activeMemberships = membershipRepository.findByStatus(Membership.MembershipStatus.ACTIVE);

        // Create a map of userId -> active membership
        Map<Long, Membership> userMembershipMap = new HashMap<>();
        for (Membership membership : activeMemberships) {
            userMembershipMap.put(membership.getUserId(), membership);
        }

        // Calculate statistics
        long totalUsers = userList.size();
        long totalMembers = activeMemberships.size();

        model.addAttribute("userList", userList);
        model.addAttribute("userMembershipMap", userMembershipMap);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalMembers", totalMembers);
        return "admin/users";
    }
}
