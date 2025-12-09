package com.app.MagicPass.controller;

<<<<<<< Updated upstream
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.MembershipTypeService;
=======
import com.app.MagicPass.model.Staff;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.MembershipTypeService;
import jakarta.servlet.http.HttpSession;
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
    public String dashboard(Model model) {
=======
    public String dashboard(HttpSession session, Model model) {
        // Check if staff is logged in
        Staff currentStaff = (Staff) session.getAttribute("currentStaff");
        Boolean isStaff = (Boolean) session.getAttribute("isStaff");

        if (currentStaff == null || isStaff == null || !isStaff) {
            return "redirect:/login";  // Redirect to login if not staff
        }

>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
=======
        model.addAttribute("currentStaff", currentStaff);
>>>>>>> Stashed changes

        return "admin/dashboard";
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}
