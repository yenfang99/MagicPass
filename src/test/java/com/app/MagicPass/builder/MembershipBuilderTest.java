package com.app.MagicPass.builder;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MembershipBuilder
 * Tests the builder pattern implementation for creating Membership objects
 */
class MembershipBuilderTest {

    private MembershipType membershipType;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;

    @BeforeEach
    void setUp() {
        membershipType = new MembershipType();
        membershipType.setId(1L);
        membershipType.setName("Gold");
        membershipType.setPrice(299.99);
        membershipType.setDiscountRate(0.10);

        startDate = LocalDateTime.now();
        expiryDate = startDate.plusMonths(12);
    }

    @Test
    void testBuildMembership_WithAllRequiredFields_ShouldSucceed() {
        // When
        Membership membership = new MembershipBuilder()
                .membershipId("MEM-001")
                .userId(100L)
                .membershipType(membershipType)
                .price(299.99)
                .discountRate(10.0)
                .startDate(startDate)
                .expiryDate(expiryDate)
                .build();

        // Then
        assertNotNull(membership);
        assertEquals("MEM-001", membership.getMembershipId());
        assertEquals(100L, membership.getUserId());
        assertEquals(membershipType, membership.getMembershipType());
        assertEquals(299.99, membership.getPrice());
        assertEquals(10.0, membership.getDiscountRate());
        assertEquals(startDate, membership.getStartDate());
        assertEquals(expiryDate, membership.getExpiryDate());
        assertEquals(Membership.MembershipStatus.ACTIVE, membership.getStatus());
    }

    @Test
    void testBuildMembership_WithCustomStatus_ShouldSucceed() {
        // When
        Membership membership = new MembershipBuilder()
                .membershipId("MEM-002")
                .userId(101L)
                .membershipType(membershipType)
                .price(299.99)
                .discountRate(10.0)
                .startDate(startDate)
                .expiryDate(expiryDate)
                .status(Membership.MembershipStatus.EXPIRED)
                .build();

        // Then
        assertNotNull(membership);
        assertEquals(Membership.MembershipStatus.EXPIRED, membership.getStatus());
    }

    @Test
    void testBuildMembership_WithoutMembershipId_ShouldThrowException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new MembershipBuilder()
                    .userId(100L)
                    .membershipType(membershipType)
                    .price(299.99)
                    .discountRate(10.0)
                    .startDate(startDate)
                    .expiryDate(expiryDate)
                    .build();
        });

        assertEquals("Membership ID is required", exception.getMessage());
    }

    @Test
    void testBuildMembership_WithEmptyMembershipId_ShouldThrowException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new MembershipBuilder()
                    .membershipId("")
                    .userId(100L)
                    .membershipType(membershipType)
                    .price(299.99)
                    .discountRate(10.0)
                    .startDate(startDate)
                    .expiryDate(expiryDate)
                    .build();
        });

        assertEquals("Membership ID is required", exception.getMessage());
    }

    @Test
    void testBuildMembership_WithoutUserId_ShouldThrowException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new MembershipBuilder()
                    .membershipId("MEM-001")
                    .membershipType(membershipType)
                    .price(299.99)
                    .discountRate(10.0)
                    .startDate(startDate)
                    .expiryDate(expiryDate)
                    .build();
        });

        assertEquals("User ID is required", exception.getMessage());
    }

    @Test
    void testBuildMembership_WithoutMembershipType_ShouldThrowException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new MembershipBuilder()
                    .membershipId("MEM-001")
                    .userId(100L)
                    .price(299.99)
                    .discountRate(10.0)
                    .startDate(startDate)
                    .expiryDate(expiryDate)
                    .build();
        });

        assertEquals("Membership Type is required", exception.getMessage());
    }

    @Test
    void testBuildMembership_WithoutPrice_ShouldThrowException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new MembershipBuilder()
                    .membershipId("MEM-001")
                    .userId(100L)
                    .membershipType(membershipType)
                    .discountRate(10.0)
                    .startDate(startDate)
                    .expiryDate(expiryDate)
                    .build();
        });

        assertEquals("Price is required", exception.getMessage());
    }

    @Test
    void testBuildMembership_WithoutDiscountRate_ShouldThrowException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new MembershipBuilder()
                    .membershipId("MEM-001")
                    .userId(100L)
                    .membershipType(membershipType)
                    .price(299.99)
                    .startDate(startDate)
                    .expiryDate(expiryDate)
                    .build();
        });

        assertEquals("Discount Rate is required", exception.getMessage());
    }

    @Test
    void testBuildMembership_WithoutStartDate_ShouldThrowException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new MembershipBuilder()
                    .membershipId("MEM-001")
                    .userId(100L)
                    .membershipType(membershipType)
                    .price(299.99)
                    .discountRate(10.0)
                    .expiryDate(expiryDate)
                    .build();
        });

        assertEquals("Start Date is required", exception.getMessage());
    }

    @Test
    void testBuildMembership_WithoutExpiryDate_ShouldThrowException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            new MembershipBuilder()
                    .membershipId("MEM-001")
                    .userId(100L)
                    .membershipType(membershipType)
                    .price(299.99)
                    .discountRate(10.0)
                    .startDate(startDate)
                    .build();
        });

        assertEquals("Expiry Date is required", exception.getMessage());
    }

    @Test
    void testBuilderMethodChaining() {
        // When
        MembershipBuilder builder = new MembershipBuilder()
                .membershipId("MEM-001")
                .userId(100L)
                .membershipType(membershipType)
                .price(299.99)
                .discountRate(10.0)
                .startDate(startDate)
                .expiryDate(expiryDate)
                .status(Membership.MembershipStatus.ACTIVE);

        // Then - verify builder returns itself for method chaining
        assertNotNull(builder);
        Membership membership = builder.build();
        assertNotNull(membership);
    }

    @Test
    void testBuildMultipleMemberships_ShouldCreateIndependentObjects() {
        // When
        Membership membership1 = new MembershipBuilder()
                .membershipId("MEM-001")
                .userId(100L)
                .membershipType(membershipType)
                .price(299.99)
                .discountRate(10.0)
                .startDate(startDate)
                .expiryDate(expiryDate)
                .build();

        Membership membership2 = new MembershipBuilder()
                .membershipId("MEM-002")
                .userId(101L)
                .membershipType(membershipType)
                .price(399.99)
                .discountRate(15.0)
                .startDate(startDate)
                .expiryDate(expiryDate)
                .build();

        // Then
        assertNotEquals(membership1.getMembershipId(), membership2.getMembershipId());
        assertNotEquals(membership1.getUserId(), membership2.getUserId());
        assertNotEquals(membership1.getPrice(), membership2.getPrice());
        assertNotEquals(membership1.getDiscountRate(), membership2.getDiscountRate());
    }
}
