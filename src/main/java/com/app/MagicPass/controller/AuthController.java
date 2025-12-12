package com.app.MagicPass.controller;

import com.app.MagicPass.model.Staff;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.AuthService;
import com.app.MagicPass.service.StaffService;
import com.app.MagicPass.service.UserService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
public class AuthController {

    private final AuthService authService;
    private final StaffService staffService;
    private final UserService userService;

    public AuthController(AuthService authService,
                          StaffService staffService,
                          UserService userService) {
        this.authService = authService;
        this.staffService = staffService;
        this.userService = userService;
    }

    // ---------- REGISTER ----------
    @GetMapping("/register")
    public String showRegister() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String name,
                             @RequestParam String email,
                             @RequestParam String phone,
                             @RequestParam String gender,
                             @RequestParam String birthday, // yyyy-MM-dd
                             @RequestParam String password,
                             @RequestParam("confirmPassword") String confirmPassword,
                             Model model) {

        // keep typed values (so user no need retype if error)
        model.addAttribute("name", name);
        model.addAttribute("email", email);
        model.addAttribute("phone", phone);
        model.addAttribute("gender", gender);
        model.addAttribute("birthday", birthday);

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        try {
            LocalDate bday = LocalDate.parse(birthday);

            authService.register(name, email, password, bday, gender, phone);

            model.addAttribute("success", "Registration successful. Please log in.");
            return "login";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        } catch (Exception ex) {
            model.addAttribute("error", "Invalid birthday format.");
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

        // 1) staff/boss
        if (staffService.isStaffEmail(normalized)) {
            try {
                Staff staff = staffService.loginStaff(normalized, password);

                session.setAttribute("currentStaff", staff);
                session.setAttribute("currentUser", null);
                session.setAttribute("isStaff", true);
                session.setAttribute("isBoss", staff.isBoss());

                return "redirect:/admin/dashboard";

            } catch (IllegalArgumentException ex) {
                model.addAttribute("error", ex.getMessage());
                return "login";
            }
        }

        // 2) normal user
        try {
            User user = authService.login(normalized, password);

            session.setAttribute("currentUser", user);
            session.setAttribute("currentStaff", null);
            session.setAttribute("isStaff", false);
            session.setAttribute("isBoss", false);

            return "redirect:/"; // better than returning "home" directly

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

    // ---------- ACCOUNT ----------
    @GetMapping("/account")
    public String viewAccount(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        User userFromDb = userService.getUserById(currentUser.getId());
        model.addAttribute("user", userFromDb);
        return "user/account-detail";
    }
}
