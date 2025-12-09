package com.app.MagicPass.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.app.MagicPass.model.User;
import com.app.MagicPass.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // --- REGISTER ---
    public User register(String email, String password) {
    // basic validation
    if (email == null || email.isBlank()) {
        throw new IllegalArgumentException("Email is required.");
    }

    email = email.trim().toLowerCase();

    if (!email.contains("@")) {
        throw new IllegalArgumentException("Please enter a valid email.");
    }

    // block staff domain through public register
    if (email.endsWith("@magicpass.my")) {
        throw new IllegalArgumentException(
                "Staff accounts cannot be registered here. Please contact the system administrator."
        );
    }

    if (password == null || password.length() < 8) {
        throw new IllegalArgumentException("Password must be at least 8 characters.");
    }

    boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");
    if (!hasSpecial) {
        throw new IllegalArgumentException(
                "Password must contain at least one special character (e.g. !,@,#)."
        );
    }

    if (userRepository.existsByEmail(email)) {
        throw new IllegalArgumentException("Email is already registered.");
    }

    User u = new User();
    u.setEmail(email);
    u.setPasswordHash(hashPassword(password));
    u.setMember(true);

    return userRepository.save(u);
}


    // --- LOGIN ---
    
    public User login(String email, String password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        var userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = userOpt.get();
        String hash = hashPassword(password);
        if (!user.getPasswordHash().equals(hash)) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return user;
    }

    // simple SHA-256 hashing (for assignment only; real system use bcrypt)
    private String hashPassword(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}