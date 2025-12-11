package com.app.MagicPass.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class MembershipTest {

    private Membership membership;
    private MembershipType membershipType;

    @BeforeEach
    void setUp() {
        membership = new Membership();
        membershipType = new MembershipType();
        membershipType.setId(1L);
        membershipType.setName("GOLD");
    }

    @Test
    void testIdGetterAndSetter() {
        Long id = 1L;
        membership.setId(id);
        assertEquals(id, membership.getId());
    }

    @Test
    void testMembershipIdGetterAndSetter() {
        String membershipId = "MEM-2024-001";
        membership.setMembershipId(membershipId);
        assertEquals(membershipId, membership.getMembershipId());
    }

    @Test
    void testUserIdGetterAndSetter() {
        Long userId = 100L;
        membership.setUserId(userId);
        assertEquals(userId, membership.getUserId());
    }

    @Test
    void testMembershipTypeGetterAndSetter() {
        membership.setMembershipType(membershipType);
        assertEquals(membershipType, membership.getMembershipType());
        assertEquals(1L, membership.getMembershipType().getId());
        assertEquals("GOLD", membership.getMembershipType().getName());
    }

    @Test
    void testPriceGetterAndSetter() {
        Double price = 299.99;
        membership.setPrice(price);
        assertEquals(price, membership.getPrice());
    }

    @Test
    void testDiscountRateGetterAndSetter() {
        Double discountRate = 0.15;
        membership.setDiscountRate(discountRate);
        assertEquals(discountRate, membership.getDiscountRate());
    }

    @Test
    void testStartDateGetterAndSetter() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        membership.setStartDate(startDate);
        assertEquals(startDate, membership.getStartDate());
    }

    @Test
    void testExpiryDateGetterAndSetter() {
        LocalDateTime expiryDate = LocalDateTime.of(2025, 1, 1, 0, 0);
        membership.setExpiryDate(expiryDate);
        assertEquals(expiryDate, membership.getExpiryDate());
    }

    @Test
    void testStatusGetterAndSetter() {
        membership.setStatus(Membership.MembershipStatus.ACTIVE);
        assertEquals(Membership.MembershipStatus.ACTIVE, membership.getStatus());

        membership.setStatus(Membership.MembershipStatus.EXPIRED);
        assertEquals(Membership.MembershipStatus.EXPIRED, membership.getStatus());

        membership.setStatus(Membership.MembershipStatus.CANCELLED);
        assertEquals(Membership.MembershipStatus.CANCELLED, membership.getStatus());
    }

    @Test
    void testDefaultStatusIsActive() {
        Membership newMembership = new Membership();
        assertEquals(Membership.MembershipStatus.ACTIVE, newMembership.getStatus());
    }

    @Test
    void testCreatedAtGetter() {
        LocalDateTime createdAt = LocalDateTime.now();
        // Note: createdAt has no setter as it's managed by @PrePersist
        // We can only test the getter returns the value
        assertNull(membership.getCreatedAt()); // Initially null before persistence
    }

    @Test
    void testOnCreateMethod() {
        // Simulate the @PrePersist callback
        membership.onCreate();
        assertNotNull(membership.getCreatedAt());
        assertTrue(membership.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void testMembershipStatusEnumValues() {
        // Test all enum values exist
        Membership.MembershipStatus[] statuses = Membership.MembershipStatus.values();
        assertEquals(3, statuses.length);

        // Verify each status
        assertEquals(Membership.MembershipStatus.ACTIVE, Membership.MembershipStatus.valueOf("ACTIVE"));
        assertEquals(Membership.MembershipStatus.EXPIRED, Membership.MembershipStatus.valueOf("EXPIRED"));
        assertEquals(Membership.MembershipStatus.CANCELLED, Membership.MembershipStatus.valueOf("CANCELLED"));
    }

    @Test
    void testCompleteObjectCreation() {
        // Test creating a complete membership object
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusYears(1);

        membership.setId(1L);
        membership.setMembershipId("MEM-2024-001");
        membership.setUserId(100L);
        membership.setMembershipType(membershipType);
        membership.setPrice(299.99);
        membership.setDiscountRate(0.15);
        membership.setStartDate(now);
        membership.setExpiryDate(expiry);
        membership.setStatus(Membership.MembershipStatus.ACTIVE);

        // Verify all fields
        assertEquals(1L, membership.getId());
        assertEquals("MEM-2024-001", membership.getMembershipId());
        assertEquals(100L, membership.getUserId());
        assertEquals(membershipType, membership.getMembershipType());
        assertEquals(299.99, membership.getPrice());
        assertEquals(0.15, membership.getDiscountRate());
        assertEquals(now, membership.getStartDate());
        assertEquals(expiry, membership.getExpiryDate());
        assertEquals(Membership.MembershipStatus.ACTIVE, membership.getStatus());
    }

    @Test
    void testNullValues() {
        // Test that null values can be set (though database constraints prevent persistence)
        membership.setId(null);
        membership.setMembershipId(null);
        membership.setUserId(null);
        membership.setMembershipType(null);
        membership.setPrice(null);
        membership.setDiscountRate(null);
        membership.setStartDate(null);
        membership.setExpiryDate(null);
        membership.setStatus(null);

        assertNull(membership.getId());
        assertNull(membership.getMembershipId());
        assertNull(membership.getUserId());
        assertNull(membership.getMembershipType());
        assertNull(membership.getPrice());
        assertNull(membership.getDiscountRate());
        assertNull(membership.getStartDate());
        assertNull(membership.getExpiryDate());
        assertNull(membership.getStatus());
    }

    @Test
    void testPriceEdgeCases() {
        // Test zero price
        membership.setPrice(0.0);
        assertEquals(0.0, membership.getPrice());

        // Test very large price
        membership.setPrice(999999.99);
        assertEquals(999999.99, membership.getPrice());

        // Test negative price (should be prevented by business logic, not model)
        membership.setPrice(-100.0);
        assertEquals(-100.0, membership.getPrice());
    }

    @Test
    void testDiscountRateEdgeCases() {
        // Test 0% discount
        membership.setDiscountRate(0.0);
        assertEquals(0.0, membership.getDiscountRate());

        // Test 100% discount
        membership.setDiscountRate(1.0);
        assertEquals(1.0, membership.getDiscountRate());

        // Test fractional discount
        membership.setDiscountRate(0.12345);
        assertEquals(0.12345, membership.getDiscountRate());
    }

    @Test
    void testDateRangeScenarios() {
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime expiry = LocalDateTime.of(2025, 6, 1, 10, 0);

        membership.setStartDate(start);
        membership.setExpiryDate(expiry);

        // Verify dates are set correctly
        assertEquals(start, membership.getStartDate());
        assertEquals(expiry, membership.getExpiryDate());

        // Verify expiry is after start (business logic test)
        assertTrue(membership.getExpiryDate().isAfter(membership.getStartDate()));
    }
}
