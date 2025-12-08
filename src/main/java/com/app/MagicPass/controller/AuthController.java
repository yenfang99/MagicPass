package com.app.MagicPass.controller;

import com.app.MagicPass.model.User;
import com.app.MagicPass.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ---------- REGISTER ----------

    @GetMapping("/register")
    public String showRegister() {
        return "register";          // shows register.html
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String email,
                             @RequestParam String password,
                             @RequestParam("confirmPassword") String confirmPassword,
                             Model model) {

        // confirm password check (frontend already checks, but double-check here)
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "register";
        }

        try {
            authService.register(email, password);
            // show login page with success message
            model.addAttribute("success",
                    "Registration successful. Please log in.");
            return "login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }
    }

    // ---------- LOGIN ----------

    @GetMapping("/login")
    public String showLogin() {
        return "login";             // shows login.html
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {

        try {
            User user = authService.login(email, password);
            session.setAttribute("currentUser", user);

            // optional message on home page
            model.addAttribute("success",
                    "Login successful. Welcome " + user.getEmail() + "!");

            // DIRECTLY render home.html (NO redirect, NO /;jsessionid issue)
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
}
