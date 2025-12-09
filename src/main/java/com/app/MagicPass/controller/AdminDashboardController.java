package com.app.MagicPass.controller;

import com.app.MagicPass.model.Staff;
import com.app.MagicPass.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // Check if staff is logged in
        Staff currentStaff = (Staff) session.getAttribute("currentStaff");
        Boolean isStaff = (Boolean) session.getAttribute("isStaff");

        if (currentStaff == null || isStaff == null || !isStaff) {
            return "redirect:/login";  // Redirect to login if not staff
        }

        // Get statistics for dashboard using DashboardService
        long totalMembers = dashboardService.getTotalActiveMembers();
        long totalTickets = dashboardService.getTotalTicketsSold();
        double totalRevenue = dashboardService.getTotalRevenue();
        long membershipTypeCount = dashboardService.getActiveMembershipTypeCount();

        model.addAttribute("totalMembers", totalMembers);
        model.addAttribute("totalTickets", totalTickets);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("membershipTypeCount", membershipTypeCount);
        model.addAttribute("currentStaff", currentStaff);

        return "admin/dashboard";
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}
