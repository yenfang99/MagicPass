package com.app.MagicPass.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "memberships")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String membershipId;

    @Column(name = "customer_id", nullable = false)
    private Long userId;  // References users.id from your existing users table

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipTier tier;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Double discountRate; // e.g., 0.10 for 10%

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMembershipId() { return membershipId; }
    public void setMembershipId(String membershipId) { this.membershipId = membershipId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public MembershipTier getTier() { return tier; }
    public void setTier(MembershipTier tier) { this.tier = tier; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getDiscountRate() { return discountRate; }
    public void setDiscountRate(Double discountRate) { this.discountRate = discountRate; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public MembershipStatus getStatus() { return status; }
    public void setStatus(MembershipStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    // Enums
    public enum MembershipTier {
        SILVER("Silver", 199.00, 0.05, "5% discount on all tickets"),
        GOLD("Gold", 399.00, 0.10, "10% discount on all tickets"),
        PLATINUM("Platinum", 699.00, 0.15, "15% discount on all tickets");

        private final String displayName;
        private final Double price;
        private final Double discountRate;
        private final String benefits;

        MembershipTier(String displayName, Double price, Double discountRate, String benefits) {
            this.displayName = displayName;
            this.price = price;
            this.discountRate = discountRate;
            this.benefits = benefits;
        }

        public String getDisplayName() { return displayName; }
        public Double getPrice() { return price; }
        public Double getDiscountRate() { return discountRate; }
        public String getBenefits() { return benefits; }
    }

    public enum MembershipStatus {
        ACTIVE,
        EXPIRED,
        CANCELLED
    }
}
