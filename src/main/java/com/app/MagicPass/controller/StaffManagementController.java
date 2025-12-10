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

    // helper: boss only
    private boolean isBoss(HttpSession session) {
        Boolean isStaff = (Boolean) session.getAttribute("isStaff");
        Boolean isBoss  = (Boolean) session.getAttribute("isBoss");
        return Boolean.TRUE.equals(isStaff) && Boolean.TRUE.equals(isBoss);
    }

    // ---- Manage Staff (boss only) ----
    @GetMapping("/staff/manage")
    public String manageStaff(HttpSession session, Model model) {
        if (!isBoss(session)) {
            return "redirect:/login";        // or redirect:/admin/dashboard, your choice
        }

        model.addAttribute("staffList", staffService.getAllStaff());
        return "admin/managestaff";
    }

    // ---- Add Staff page (boss only) ----
    @GetMapping("/staff/manage/add")
    public String showAddStaffPage(HttpSession session) {
        if (!isBoss(session)) {
            return "redirect:/login";
        }
        return "admin/addstaff";
    }

    // ---- Handle Add Staff form (boss only) ----
    @PostMapping("/staff/manage/add")
    public String addStaff(@RequestParam String email,
                           @RequestParam String password,
                           HttpSession session,
                           Model model,
                           RedirectAttributes ra) {

        if (!isBoss(session)) {
            return "redirect:/login";
        }

        try {
            staffService.createStaff(email, password);
            ra.addFlashAttribute("success", "Staff account created successfully.");
            return "redirect:/staff/manage";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("email", email);
            return "admin/addstaff";
        }
    }

    // ---- Delete Staff (boss only) ----
    @PostMapping("/staff/manage/delete")
public String deleteStaff(@RequestParam Long id,
                          HttpSession session,
                          RedirectAttributes ra) {

    // (optional) if you want *only boss* to delete:
    Boolean isBoss = (Boolean) session.getAttribute("isBoss");
    if (isBoss == null || !isBoss) {
        return "redirect:/login";
    }

    try {
        staffService.deleteStaff(id);
        ra.addFlashAttribute("success", "Staff account removed.");
    } catch (IllegalArgumentException ex) {
        ra.addFlashAttribute("error", ex.getMessage());
    }

    return "redirect:/staff/manage";
}

}
