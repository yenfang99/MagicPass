package com.app.MagicPass.controller;

import com.app.MagicPass.model.Staff;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.AuthService;
import com.app.MagicPass.service.StaffService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.app.MagicPass.service.UserService;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final AuthService authService;
    private final StaffService staffService;
    private final UserService userService;   // 👈 add this

    public AuthController(AuthService authService,
                          StaffService staffService,
                          UserService userService) {   // 👈 add param
        this.authService = authService;
        this.staffService = staffService;
        this.userService = userService;      // 👈 assign
    }

    // ---------- REGISTER (users only) ----------
    @GetMapping("/register")
    public String showRegister() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String email,
                             @RequestParam String password,
                             @RequestParam("confirmPassword") String confirmPassword,
                             Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        try {
            authService.register(email, password);
            model.addAttribute("success", "Registration successful. Please log in.");
            return "login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }

    // ---------- LOGIN (users + staff + boss) ----------
    @GetMapping("/login")
    public String showLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {

        String normalized = email.trim().toLowerCase();

        // ---- 1. Staff / Boss login (staff_accounts table) ----
        if (staffService.isStaffEmail(normalized)) {
            try {
                Staff staff = staffService.loginStaff(normalized, password);

                // store staff in session
                session.setAttribute("currentStaff", staff);
                session.setAttribute("currentUser", null);
                session.setAttribute("isStaff", true);
                session.setAttribute("isBoss", staff.isBoss());

                // All staff (including boss) go to admin dashboard
                return "redirect:/admin/dashboard";

            } catch (IllegalArgumentException ex) {
                model.addAttribute("error", ex.getMessage());
                return "login";
            }
        }

        // ---- 2. Normal user login (users table) ----
        try {
            User user = authService.login(normalized, password);

            session.setAttribute("currentUser", user);
            session.setAttribute("currentStaff", null);
            session.setAttribute("isStaff", false);
            session.setAttribute("isBoss", false);

            model.addAttribute("success",
                    "Login successful. Welcome " + user.getEmail() + "!");
            return "home";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "login";
        }
    }

    // ---------- LOGOUT ----------
    @GetMapping("/logout")
    public String logout(HttpSession session, Model model) {
        session.invalidate();
        model.addAttribute("success", "You have been logged out.");
        return "login";
    }

    @GetMapping("/account")
public String viewAccount(HttpSession session, Model model) {
    // Get logged-in user from session
    User currentUser = (User) session.getAttribute("currentUser");

    if (currentUser == null) {
        // Not logged in – redirect to login
        return "redirect:/login";
    }

    // Optional: re-fetch from DB to make sure data is fresh
    User userFromDb = userService.getUserById(currentUser.getId());

    model.addAttribute("user", userFromDb);
    return "user/account-detail";   // this will be your Thymeleaf page
}
}
