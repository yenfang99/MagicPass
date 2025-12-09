package com.app.MagicPass.controller;

import com.app.MagicPass.service.StaffService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StaffManagementController {

    private final StaffService staffService;

    public StaffManagementController(StaffService staffService) {
        this.staffService = staffService;
    }

    // ---- Manage Staff (staff only, both boss and regular staff) ----
    @GetMapping("/staff/manage")
    public String manageStaff(HttpSession session, Model model) {
        // Check if user is logged in as staff
        Boolean isStaff = (Boolean) session.getAttribute("isStaff");
        if (isStaff == null || !isStaff) {
            return "redirect:/login";
        }

        model.addAttribute("staffList", staffService.getAllStaff());
        return "admin/managestaff";         // admin/managestaff.html
    }

    // ---- Add Staff page (staff only) ----
    @GetMapping("/staff/manage/add")
    public String showAddStaffPage(HttpSession session) {
        // Check if user is logged in as staff
        Boolean isStaff = (Boolean) session.getAttribute("isStaff");
        if (isStaff == null || !isStaff) {
            return "redirect:/login";
        }
        return "admin/addstaff";            // admin/addstaff.html
    }

    // ---- Handle Add Staff form (staff only) ----
    @PostMapping("/staff/manage/add")
    public String addStaff(@RequestParam String email,
                           @RequestParam String password,
                           HttpSession session,
                           org.springframework.ui.Model model,
                           RedirectAttributes ra) {

        // Check if user is logged in as staff
        Boolean isStaff = (Boolean) session.getAttribute("isStaff");
        if (isStaff == null || !isStaff) {
            return "redirect:/login";
        }

        try {
            staffService.createStaff(email, password);

            // success → go back to Manage Staff
            ra.addFlashAttribute("success", "Staff account created successfully.");
            return "redirect:/staff/manage";

        } catch (IllegalArgumentException ex) {
            // validation error → stay on Add Staff page
            model.addAttribute("error", ex.getMessage());
            // keep the values user typed (optional)
            model.addAttribute("email", email);
            return "admin/addstaff";   // this renders admin/addstaff.html again
        }
    }

    // ---- Delete Staff (keep boss check – deleting is dangerous) ----
    @PostMapping("/staff/manage/delete")
public String deleteStaff(@RequestParam Long id,
                          HttpSession session,
                          RedirectAttributes ra) {

    // (optional boss check here)
    // if (!isBoss(session)) return "redirect:/login";

    staffService.deleteStaff(id);
    ra.addFlashAttribute("success", "Staff account removed.");
    return "redirect:/staff/manage";
}
}
