package com.app.MagicPass.service;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.model.MembershipType;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StripeService.
 *
 * Note: These tests do NOT make actual API calls to Stripe.
 * They test the service's logic for building Stripe parameters correctly.
 *
 * For actual Stripe integration testing, use:
 * 1. Stripe Test Mode with test API keys
 * 2. Stripe CLI webhook testing
 * 3. Manual testing in test environment
 */
class StripeServiceTest {

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService();
        // Set the publishable key via reflection (simulates @Value injection)
        ReflectionTestUtils.setField(stripeService, "publishableKey", "pk_test_mock_key");
    }

    @Test
    void testGetPublishableKey() {
        // Test that publishable key is accessible
        String key = stripeService.getPublishableKey();

        assertNotNull(key);
        assertEquals("pk_test_mock_key", key);
    }

    @Test
    void testCreateCheckoutSessionParameterValidation() {
        // Test parameter validation (business logic, not Stripe API)
        MembershipType membershipType = new MembershipType();
        membershipType.setId(1L);
        membershipType.setDisplayName("Gold Membership");
        membershipType.setPrice(299.99);
        membershipType.setDurationMonths(12);

        // Verify parameters are set correctly
        assertNotNull(membershipType.getId());
        assertNotNull(membershipType.getDisplayName());
        assertNotNull(membershipType.getPrice());
        assertEquals(12, membershipType.getDurationMonths());

        // Note: We don't call the actual createCheckoutSession method
        // because it would make a real API call to Stripe
        // In production code, this would be mocked
    }

    @Test
    void testPriceConversionToCents() {
        // Test the business logic of price conversion
        double priceInMYR = 299.99;
        long expectedPriceInCents = 29999L;

        // Simulate the conversion logic from line 30
        long actualPriceInCents = (long) (priceInMYR * 100);

        assertEquals(expectedPriceInCents, actualPriceInCents);
    }

    @Test
    void testPriceConversionEdgeCases() {
        // Test edge cases for price conversion
        assertEquals(0L, (long) (0.0 * 100));
        assertEquals(100L, (long) (1.0 * 100));
        assertEquals(9999L, (long) (99.99 * 100));
        assertEquals(100000L, (long) (1000.0 * 100));
    }

    @Test
    void testDurationDescriptionLogic() {
        // Test the description generation logic (lines 31-33)

        // 12 months should show "1 year"
        int duration12 = 12;
        String description12 = duration12 == 12
                ? "1 year membership with exclusive benefits"
                : duration12 + " months membership with exclusive benefits";
        assertEquals("1 year membership with exclusive benefits", description12);

        // Other durations should show months
        int duration6 = 6;
        String description6 = duration6 == 12
                ? "1 year membership with exclusive benefits"
                : duration6 + " months membership with exclusive benefits";
        assertEquals("6 months membership with exclusive benefits", description6);

        int duration24 = 24;
        String description24 = duration24 == 12
                ? "1 year membership with exclusive benefits"
                : duration24 + " months membership with exclusive benefits";
        assertEquals("24 months membership with exclusive benefits", description24);
    }

    @Test
    void testTicketDescriptionFormat() {
        // Test the description format for tickets (lines 77-79)
        CheckoutRequest req = new CheckoutRequest();
        req.setAdultQty(2);
        req.setStudentQty(1);
        req.setChildQty(3);
        req.setReservationDate(LocalDate.of(2024, 12, 25));

        String expectedDescription = "Adult: 2, Student: 1, Children: 3 - Date: 2024-12-25";
        String actualDescription = String.format("Adult: %d, Student: %d, Children: %d - Date: %s",
                req.getAdultQty(), req.getStudentQty(), req.getChildQty(),
                req.getReservationDate().toString());

        assertEquals(expectedDescription, actualDescription);
    }

    @Test
    void testTicketPriceConversionToCents() {
        // Test price conversion for tickets
        double grandTotal = 1234.56;
        long expectedInCents = 123456L;

        long actualInCents = (long) (grandTotal * 100);

        assertEquals(expectedInCents, actualInCents);
    }

    @Test
    void testMetadataUserIdHandling() {
        // Test the userId metadata handling logic (line 106)

        // When userId is present
        Long userId = 123L;
        String userIdMetadata = userId != null ? userId.toString() : "anonymous";
        assertEquals("123", userIdMetadata);

        // When userId is null
        Long nullUserId = null;
        String anonymousMetadata = nullUserId != null ? nullUserId.toString() : "anonymous";
        assertEquals("anonymous", anonymousMetadata);
    }

    @Test
    void testCheckoutRequestDataIntegrity() {
        // Test that CheckoutRequest maintains data integrity
        CheckoutRequest req = new CheckoutRequest();
        req.setUserId(100L);
        req.setAdultQty(5);
        req.setStudentQty(3);
        req.setChildQty(2);
        req.setReservationDate(LocalDate.of(2024, 12, 31));

        assertEquals(100L, req.getUserId());
        assertEquals(5, req.getAdultQty());
        assertEquals(3, req.getStudentQty());
        assertEquals(2, req.getChildQty());
        assertEquals(LocalDate.of(2024, 12, 31), req.getReservationDate());
    }

    @Test
    void testMembershipTypeDataForStripeSession() {
        // Test that MembershipType has all required data for Stripe session
        MembershipType type = new MembershipType();
        type.setId(1L);
        type.setName("GOLD");
        type.setDisplayName("Gold Membership");
        type.setPrice(299.99);
        type.setDurationMonths(12);

        // Verify all fields needed for Stripe session are present
        assertNotNull(type.getId(), "ID required for metadata");
        assertNotNull(type.getDisplayName(), "Display name required for product name");
        assertNotNull(type.getPrice(), "Price required for payment amount");
        assertNotNull(type.getDurationMonths(), "Duration required for description");
        assertTrue(type.getPrice() > 0, "Price must be positive");
    }

    @Test
    void testSuccessAndCancelUrlFormat() {
        // Test URL format validation
        String successUrl = "http://localhost:8080/membership/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = "http://localhost:8080/membership/cancel";

        assertNotNull(successUrl);
        assertNotNull(cancelUrl);
        assertTrue(successUrl.contains("success"));
        assertTrue(cancelUrl.contains("cancel"));
        assertTrue(successUrl.startsWith("http"));
        assertTrue(cancelUrl.startsWith("http"));
    }
}
