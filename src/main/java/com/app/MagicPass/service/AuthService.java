package com.app.MagicPass.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Period;
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

    // --- REGISTER (age computed from birthday) ---
    public User register(String name,
                         String email,
                         String password,
                         LocalDate birthday,
                         String gender,
                         String phone) {

        // name
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required.");
        }

        // email
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

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        // password
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");
        if (!hasSpecial) {
            throw new IllegalArgumentException(
                    "Password must contain at least one special character (e.g. !,@,#)."
            );
        }

        // birthday
        if (birthday == null) {
            throw new IllegalArgumentException("Birthday is required.");
        }

        int computedAge = Period.between(birthday, LocalDate.now()).getYears();
        if (computedAge < 1 || computedAge > 120) {
            throw new IllegalArgumentException("Age is invalid.");
        }

        // gender
        gender = (gender == null) ? "" : gender.trim().toUpperCase();
        if (!gender.equals("F") && !gender.equals("M")) {
            throw new IllegalArgumentException("Gender must be F or M.");
        }

        // phone
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required.");
        }

        phone = phone.trim();
        if (!phone.matches("^\\d{9,12}$")) {
            throw new IllegalArgumentException("Phone number must be 9 to 12 digits.");
        }

        // save
        User u = new User();
        u.setName(name.trim());
        u.setEmail(email);
        u.setPasswordHash(hashPassword(password));
        u.setMember(true);

        u.setBirthday(birthday);
        u.setAge(computedAge);       // ✅ auto compute
        u.setGender(gender);
        u.setPhone(phone);

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

    // SHA-256 hashing (assignment only)
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
