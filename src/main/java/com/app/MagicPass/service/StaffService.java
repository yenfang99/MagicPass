package com.app.MagicPass.service;

import com.app.MagicPass.model.Staff;
import com.app.MagicPass.repository.StaffRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Service
public class StaffService {

    private final StaffRepository staffRepository;

    public StaffService(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    /**
     * Create the boss account automatically when the application starts,
     * if it does not exist yet.
     */
    @PostConstruct
    public void initBoss() {
        String bossEmail = "boss@magicpass.my";
        if (!staffRepository.existsByEmail(bossEmail)) {
            Staff boss = new Staff();
            boss.setEmail(bossEmail);
            boss.setPasswordHash(hashPassword("Abc123456")); // default boss password
            boss.setBoss(true);
            staffRepository.save(boss);
        }
    }

    /**
     * Check whether the given email belongs to a staff account.
     */
    public boolean isStaffEmail(String email) {
        if (email == null) return false;
        String normalized = email.trim().toLowerCase();
        return staffRepository.existsByEmail(normalized);
    }

    /**
     * Login for staff + boss accounts (uses staff_accounts table).
     */
    public Staff loginStaff(String email, String password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        String normalized = email.trim().toLowerCase();
        var opt = staffRepository.findByEmail(normalized);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        Staff staff = opt.get();
        String hash = hashPassword(password);
        if (!staff.getPasswordHash().equals(hash)) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return staff;
    }

    /**
     * Get all staff accounts (including boss) for Manage Staff page.
     */
    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    /**
     * Create a new normal staff account (boss uses this from Manage Staff page).
     */
    public void createStaff(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Staff email is required.");
        }
        String normalized = email.trim().toLowerCase();

        if (!normalized.endsWith("@magicpass.my")) {
            throw new IllegalArgumentException("Staff email must end with @magicpass.my.");
        }

        if (staffRepository.existsByEmail(normalized)) {
            throw new IllegalArgumentException("This staff email already exists.");
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        // at least one non-alphanumeric character
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");
        if (!hasSpecial) {
            throw new IllegalArgumentException(
                    "Password must contain at least one special character (e.g. !,@,#)."
            );
        }

        Staff s = new Staff();
        s.setEmail(normalized);
        s.setPasswordHash(hashPassword(password));
        s.setBoss(false); // only boss is true, others are normal staff

        staffRepository.save(s);
    }

    /**
     * Delete a staff account by id (used by boss).
     */
    public void deleteStaff(Long id) {
        staffRepository.deleteById(id);
    }

    /**
     * Simple SHA-256 hashing (same style as AuthService).
     */
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
