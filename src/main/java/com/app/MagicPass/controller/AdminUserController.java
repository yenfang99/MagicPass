package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;
    private final MembershipService membershipService;

    public AdminUserController(UserService userService, MembershipService membershipService) {
        this.userService = userService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public String listUsers(Model model) {
        // Get all users
        List<User> userList = userService.getAllUsers();

        // Get all memberships and create a map of userId -> active membership
        List<Membership> allMemberships = membershipService.getAllMemberships();
        Map<Long, Membership> userMembershipMap = new HashMap<>();

        for (Membership membership : allMemberships) {
            if (membership.getStatus() == Membership.MembershipStatus.ACTIVE) {
                userMembershipMap.put(membership.getUserId(), membership);
            }
        }

        // Calculate statistics
        long totalUsers = userList.size();
        long totalMembers = userMembershipMap.size();

        model.addAttribute("userList", userList);
        model.addAttribute("userMembershipMap", userMembershipMap);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalMembers", totalMembers);

        return "admin/users";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("mode", "create");
        return "admin/user-form";
    }

    @PostMapping("/create")
    public String createUser(@ModelAttribute User user, RedirectAttributes ra) {
        try {
            userService.createUser(user);
            ra.addFlashAttribute("success", "User created successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error creating user: " + e.getMessage());
            return "redirect:/admin/users/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            User user = userService.getUserById(id);
            model.addAttribute("user", user);
            model.addAttribute("mode", "edit");
            return "admin/user-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "User not found!");
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/edit/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User user, RedirectAttributes ra) {
        try {
            userService.updateUser(id, user);
            ra.addFlashAttribute("success", "User updated successfully!");
            return "redirect:/admin/users";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error updating user: " + e.getMessage());
            return "redirect:/admin/users/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteUser(id);
            ra.addFlashAttribute("success", "User deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
