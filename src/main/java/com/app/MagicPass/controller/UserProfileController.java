package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
public class UserProfileController {

    private final MembershipService membershipService;
    private final OrderService orderService;

    public UserProfileController(MembershipService membershipService,
                                 OrderService orderService) {
        this.membershipService = membershipService;
        this.orderService = orderService;
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
        return "profile";   // match templates/profile.html
    }

    @GetMapping("/orders")
    public String orderHistory(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("orders", orderService.getOrdersForUser(currentUser.getId()));
        model.addAttribute("user", currentUser);
        return "order-history";
    }
}

