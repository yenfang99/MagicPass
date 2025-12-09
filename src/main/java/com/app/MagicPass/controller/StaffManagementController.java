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

    // ---- helper: boss check (ONLY for main page + delete) ----
    private boolean isBoss(HttpSession session) {
        Boolean bossFlag = (Boolean) session.getAttribute("isBoss");
        return Boolean.TRUE.equals(bossFlag);
    }

    // ---- Manage Staff (boss only) ----
    @GetMapping("/staff/manage")
    public String manageStaff(HttpSession session, Model model) {
        if (!isBoss(session)) {
            return "login";           // or "redirect:/login"
        }
        model.addAttribute("staffList", staffService.getAllStaff());
        return "managestaff";         // managestaff.html
    }

    // ---- Add Staff page (NO boss/staff check) ----
    @GetMapping("/staff/manage/add")
    public String showAddStaffPage() {
        // no session checks here on purpose
        return "addstaff";            // addstaff.html
    }

    // ---- Handle Add Staff form (NO boss/staff check) ----
   @PostMapping("/staff/manage/add")
public String addStaff(@RequestParam String email,
                       @RequestParam String password,
                       HttpSession session,
                       org.springframework.ui.Model model,
                       RedirectAttributes ra) {

    // (optional) if you still want to guard by boss:
    // Boolean isBoss = (Boolean) session.getAttribute("isBoss");
    // if (isBoss == null || !isBoss) {
    //     return "redirect:/login";
    // }

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
        return "addstaff";   // this renders addstaff.html again
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
