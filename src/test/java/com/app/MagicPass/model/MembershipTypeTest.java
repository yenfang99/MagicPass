package com.app.MagicPass.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class MembershipTypeTest {

    private MembershipType membershipType;

    @BeforeEach
    void setUp() {
        membershipType = new MembershipType();
    }

    @Test
    void testIdGetterAndSetter() {
        Long id = 1L;
        membershipType.setId(id);
        assertEquals(id, membershipType.getId());
    }

    @Test
    void testNameGetterAndSetter() {
        String name = "GOLD";
        membershipType.setName(name);
        assertEquals(name, membershipType.getName());
    }

    @Test
    void testDisplayNameGetterAndSetter() {
        String displayName = "Gold Membership";
        membershipType.setDisplayName(displayName);
        assertEquals(displayName, membershipType.getDisplayName());
    }

    @Test
    void testDescriptionGetterAndSetter() {
        String description = "Premium gold tier membership with exclusive benefits";
        membershipType.setDescription(description);
        assertEquals(description, membershipType.getDescription());
    }

    @Test
    void testPriceGetterAndSetter() {
        Double price = 299.99;
        membershipType.setPrice(price);
        assertEquals(price, membershipType.getPrice());
    }

    @Test
    void testDiscountRateGetterAndSetter() {
        Double discountRate = 0.15;
        membershipType.setDiscountRate(discountRate);
        assertEquals(discountRate, membershipType.getDiscountRate());
    }

    @Test
    void testDurationMonthsGetterAndSetter() {
        Integer duration = 24;
        membershipType.setDurationMonths(duration);
        assertEquals(duration, membershipType.getDurationMonths());
    }

    @Test
    void testDefaultDurationMonthsIs12() {
        MembershipType newType = new MembershipType();
        assertEquals(12, newType.getDurationMonths());
    }

    @Test
    void testActiveGetterAndSetter() {
        membershipType.setActive(true);
        assertTrue(membershipType.getActive());

        membershipType.setActive(false);
        assertFalse(membershipType.getActive());
    }

    @Test
    void testDefaultActiveIsTrue() {
        MembershipType newType = new MembershipType();
        assertTrue(newType.getActive());
    }

    @Test
    void testDisplayOrderGetterAndSetter() {
        Integer displayOrder = 5;
        membershipType.setDisplayOrder(displayOrder);
        assertEquals(displayOrder, membershipType.getDisplayOrder());
    }

    @Test
    void testDefaultDisplayOrderIsZero() {
        MembershipType newType = new MembershipType();
        assertEquals(0, newType.getDisplayOrder());
    }

    @Test
    void testTierLevelGetterAndSetter() {
        Integer tierLevel = 3;
        membershipType.setTierLevel(tierLevel);
        assertEquals(tierLevel, membershipType.getTierLevel());
    }

    @Test
    void testDefaultTierLevelIsOne() {
        MembershipType newType = new MembershipType();
        assertEquals(1, newType.getTierLevel());
    }

    @Test
    void testCreatedAtGetterAndSetter() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        membershipType.setCreatedAt(createdAt);
        assertEquals(createdAt, membershipType.getCreatedAt());
    }

    @Test
    void testUpdatedAtGetterAndSetter() {
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 15, 30);
        membershipType.setUpdatedAt(updatedAt);
        assertEquals(updatedAt, membershipType.getUpdatedAt());
    }

    @Test
    void testOnCreateMethod() {
        // Simulate the @PrePersist callback
        LocalDateTime beforeCreate = LocalDateTime.now();
        membershipType.onCreate();
        LocalDateTime afterCreate = LocalDateTime.now();

        assertNotNull(membershipType.getCreatedAt());
        assertNotNull(membershipType.getUpdatedAt());

        // Verify timestamps are within expected range
        assertTrue(!membershipType.getCreatedAt().isBefore(beforeCreate));
        assertTrue(!membershipType.getCreatedAt().isAfter(afterCreate));
        assertTrue(!membershipType.getUpdatedAt().isBefore(beforeCreate));
        assertTrue(!membershipType.getUpdatedAt().isAfter(afterCreate));

        // Verify both timestamps are equal on creation
        assertEquals(membershipType.getCreatedAt(), membershipType.getUpdatedAt());
    }

    @Test
    void testOnUpdateMethod() throws InterruptedException {
        // First create
        membershipType.onCreate();
        LocalDateTime originalCreatedAt = membershipType.getCreatedAt();
        LocalDateTime originalUpdatedAt = membershipType.getUpdatedAt();

        // Wait a moment to ensure time difference
        Thread.sleep(10);

        // Simulate the @PreUpdate callback
        membershipType.onUpdate();

        // createdAt should remain unchanged
        assertEquals(originalCreatedAt, membershipType.getCreatedAt());

        // updatedAt should be newer
        assertTrue(membershipType.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void testCompleteObjectCreation() {
        // Test creating a complete membership type object
        membershipType.setId(1L);
        membershipType.setName("PLATINUM");
        membershipType.setDisplayName("Platinum Membership");
        membershipType.setDescription("Ultimate tier with all benefits");
        membershipType.setPrice(599.99);
        membershipType.setDiscountRate(0.25);
        membershipType.setDurationMonths(12);
        membershipType.setActive(true);
        membershipType.setDisplayOrder(1);
        membershipType.setTierLevel(3);

        // Verify all fields
        assertEquals(1L, membershipType.getId());
        assertEquals("PLATINUM", membershipType.getName());
        assertEquals("Platinum Membership", membershipType.getDisplayName());
        assertEquals("Ultimate tier with all benefits", membershipType.getDescription());
        assertEquals(599.99, membershipType.getPrice());
        assertEquals(0.25, membershipType.getDiscountRate());
        assertEquals(12, membershipType.getDurationMonths());
        assertTrue(membershipType.getActive());
        assertEquals(1, membershipType.getDisplayOrder());
        assertEquals(3, membershipType.getTierLevel());
    }

    @Test
    void testNullValues() {
        // Test that null values can be set (though database constraints prevent some)
        membershipType.setId(null);
        membershipType.setName(null);
        membershipType.setDisplayName(null);
        membershipType.setDescription(null);
        membershipType.setPrice(null);
        membershipType.setDiscountRate(null);
        membershipType.setDurationMonths(null);
        membershipType.setActive(null);
        membershipType.setDisplayOrder(null);
        membershipType.setTierLevel(null);
        membershipType.setCreatedAt(null);
        membershipType.setUpdatedAt(null);

        assertNull(membershipType.getId());
        assertNull(membershipType.getName());
        assertNull(membershipType.getDisplayName());
        assertNull(membershipType.getDescription());
        assertNull(membershipType.getPrice());
        assertNull(membershipType.getDiscountRate());
        assertNull(membershipType.getDurationMonths());
        assertNull(membershipType.getActive());
        assertNull(membershipType.getDisplayOrder());
        assertNull(membershipType.getTierLevel());
        assertNull(membershipType.getCreatedAt());
        assertNull(membershipType.getUpdatedAt());
    }

    @Test
    void testPriceEdgeCases() {
        // Test zero price
        membershipType.setPrice(0.0);
        assertEquals(0.0, membershipType.getPrice());

        // Test very large price
        membershipType.setPrice(999999.99);
        assertEquals(999999.99, membershipType.getPrice());

        // Test decimal precision
        membershipType.setPrice(49.99);
        assertEquals(49.99, membershipType.getPrice());
    }

    @Test
    void testDiscountRateEdgeCases() {
        // Test 0% discount
        membershipType.setDiscountRate(0.0);
        assertEquals(0.0, membershipType.getDiscountRate());

        // Test 100% discount
        membershipType.setDiscountRate(1.0);
        assertEquals(1.0, membershipType.getDiscountRate());

        // Test fractional discount
        membershipType.setDiscountRate(0.12345);
        assertEquals(0.12345, membershipType.getDiscountRate());

        // Test 50% discount
        membershipType.setDiscountRate(0.5);
        assertEquals(0.5, membershipType.getDiscountRate());
    }

    @Test
    void testDurationMonthsEdgeCases() {
        // Test 1 month
        membershipType.setDurationMonths(1);
        assertEquals(1, membershipType.getDurationMonths());

        // Test 12 months
        membershipType.setDurationMonths(12);
        assertEquals(12, membershipType.getDurationMonths());

        // Test 24 months
        membershipType.setDurationMonths(24);
        assertEquals(24, membershipType.getDurationMonths());

        // Test lifetime (e.g., 999 months)
        membershipType.setDurationMonths(999);
        assertEquals(999, membershipType.getDurationMonths());
    }

    @Test
    void testTierLevelScenarios() {
        // Test different tier levels
        membershipType.setTierLevel(1); // Bronze
        assertEquals(1, membershipType.getTierLevel());

        membershipType.setTierLevel(2); // Silver
        assertEquals(2, membershipType.getTierLevel());

        membershipType.setTierLevel(3); // Gold
        assertEquals(3, membershipType.getTierLevel());

        membershipType.setTierLevel(4); // Platinum
        assertEquals(4, membershipType.getTierLevel());
    }

    @Test
    void testDisplayOrderScenarios() {
        // Test negative display order
        membershipType.setDisplayOrder(-1);
        assertEquals(-1, membershipType.getDisplayOrder());

        // Test zero display order
        membershipType.setDisplayOrder(0);
        assertEquals(0, membershipType.getDisplayOrder());

        // Test positive display order
        membershipType.setDisplayOrder(100);
        assertEquals(100, membershipType.getDisplayOrder());
    }

    @Test
    void testLongDescription() {
        // Test with very long description
        String longDescription = "A".repeat(1000);
        membershipType.setDescription(longDescription);
        assertEquals(longDescription, membershipType.getDescription());
        assertEquals(1000, membershipType.getDescription().length());
    }

    @Test
    void testEmptyStrings() {
        // Test empty strings
        membershipType.setName("");
        membershipType.setDisplayName("");
        membershipType.setDescription("");

        assertEquals("", membershipType.getName());
        assertEquals("", membershipType.getDisplayName());
        assertEquals("", membershipType.getDescription());
    }

    @Test
    void testMembershipTypeComparison() {
        // Create two membership types with same values
        MembershipType type1 = new MembershipType();
        type1.setId(1L);
        type1.setName("SILVER");
        type1.setTierLevel(2);

        MembershipType type2 = new MembershipType();
        type2.setId(1L);
        type2.setName("SILVER");
        type2.setTierLevel(2);

        // Test individual field equality
        assertEquals(type1.getId(), type2.getId());
        assertEquals(type1.getName(), type2.getName());
        assertEquals(type1.getTierLevel(), type2.getTierLevel());
    }
}
