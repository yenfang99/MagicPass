package com.app.MagicPass.controller;

import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.MembershipTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final MembershipTypeService membershipTypeService;
    private final MembershipService membershipService;

    public AdminDashboardController(MembershipTypeService membershipTypeService,
                                   MembershipService membershipService) {
        this.membershipTypeService = membershipTypeService;
        this.membershipService = membershipService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Get statistics for dashboard
        long membershipTypeCount = membershipTypeService.getActiveMembershipTypes().size();

        // TODO: Implement these methods in their respective services
        // long totalMembers = membershipService.getTotalActiveMembers();
        // long totalTickets = ticketService.getTotalTicketsSoldThisMonth();
        // double totalRevenue = paymentService.getTotalRevenue();

        model.addAttribute("membershipTypeCount", membershipTypeCount);
        model.addAttribute("totalMembers", 0); // Placeholder
        model.addAttribute("totalTickets", 0); // Placeholder
        model.addAttribute("totalRevenue", 0.0); // Placeholder

        return "admin/dashboard";
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}
