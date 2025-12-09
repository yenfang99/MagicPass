package com.app.MagicPass.builder;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import java.time.LocalDateTime;

/**
 * Builder pattern for creating Membership objects.
 * Provides a fluent API for constructing memberships with required and optional fields.
 */
public class MembershipBuilder {

    private String membershipId;
    private Long userId;
    private MembershipType membershipType;
    private Double price;
    private Double discountRate;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private Membership.MembershipStatus status = Membership.MembershipStatus.ACTIVE;

    public MembershipBuilder membershipId(String membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public MembershipBuilder userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public MembershipBuilder membershipType(MembershipType membershipType) {
        this.membershipType = membershipType;
        return this;
    }

    public MembershipBuilder price(Double price) {
        this.price = price;
        return this;
    }

    public MembershipBuilder discountRate(Double discountRate) {
        this.discountRate = discountRate;
        return this;
    }

    public MembershipBuilder startDate(LocalDateTime startDate) {
        this.startDate = startDate;
        return this;
    }

    public MembershipBuilder expiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public MembershipBuilder status(Membership.MembershipStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Builds and returns a Membership instance with the configured values.
     * @return fully constructed Membership object
     * @throws IllegalStateException if required fields are missing
     */
    public Membership build() {
        // Validate required fields
        if (membershipId == null || membershipId.isEmpty()) {
            throw new IllegalStateException("Membership ID is required");
        }
        if (userId == null) {
            throw new IllegalStateException("User ID is required");
        }
        if (membershipType == null) {
            throw new IllegalStateException("Membership Type is required");
        }
        if (price == null) {
            throw new IllegalStateException("Price is required");
        }
        if (discountRate == null) {
            throw new IllegalStateException("Discount Rate is required");
        }
        if (startDate == null) {
            throw new IllegalStateException("Start Date is required");
        }
        if (expiryDate == null) {
            throw new IllegalStateException("Expiry Date is required");
        }

        // Create and populate membership
        Membership membership = new Membership();
        membership.setMembershipId(this.membershipId);
        membership.setUserId(this.userId);
        membership.setMembershipType(this.membershipType);
        membership.setPrice(this.price);
        membership.setDiscountRate(this.discountRate);
        membership.setStartDate(this.startDate);
        membership.setExpiryDate(this.expiryDate);
        membership.setStatus(this.status);

        return membership;
    }
}
