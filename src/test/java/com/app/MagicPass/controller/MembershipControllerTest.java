package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.MembershipTypeService;
import com.app.MagicPass.service.StripeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for MembershipController
 * Tests membership purchase flow and authentication
 */
@ExtendWith(MockitoExtension.class)
class MembershipControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MembershipService membershipService;

    @Mock
    private MembershipTypeService membershipTypeService;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private MembershipController membershipController;

    private User user;
    private MockHttpSession session;
    private MembershipType silverType;
    private Membership activeSilverMembership;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(membershipController).build();

        user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");

        session = new MockHttpSession();

        silverType = new MembershipType();
        silverType.setId(1L);
        silverType.setName("Silver");
        silverType.setDisplayName("Silver Membership");
        silverType.setPrice(99.99);
        silverType.setDiscountRate(0.05);
        silverType.setActive(true);
        silverType.setTierLevel(1);

        activeSilverMembership = new Membership();
        activeSilverMembership.setMembershipId("MEM-12345678");
        activeSilverMembership.setUserId(1L);
        activeSilverMembership.setMembershipType(silverType);
        activeSilverMembership.setPrice(99.99);
        activeSilverMembership.setDiscountRate(0.05);
        activeSilverMembership.setStartDate(LocalDateTime.now());
        activeSilverMembership.setExpiryDate(LocalDateTime.now().plusMonths(12));
        activeSilverMembership.setStatus(Membership.MembershipStatus.ACTIVE);
    }

    @Test
    void testMembershipPage_WithoutLogin_ShouldRedirectToLogin() throws Exception {
        // Given - no user in session

        // When & Then
        mockMvc.perform(get("/membership").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(membershipTypeService, never()).getActiveMembershipTypes();
    }

    @Test
    void testPurchaseMembership_WithoutLogin_ShouldRedirectToLogin() throws Exception {
        // Given - no user in session

        // When & Then
        mockMvc.perform(post("/membership/purchase")
                        .param("membershipTypeId", "1")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("error"));

        verify(stripeService, never()).createCheckoutSession(any(), any(), any(), any());
    }

    @Test
    void testPurchaseMembership_InactiveMembershipType_ShouldRedirectWithError() throws Exception {
        // Given
        session.setAttribute("currentUser", user);

        MembershipType inactiveType = new MembershipType();
        inactiveType.setId(1L);
        inactiveType.setActive(false);

        when(membershipTypeService.getMembershipTypeById(1L)).thenReturn(inactiveType);

        // When & Then
        mockMvc.perform(post("/membership/purchase")
                        .param("membershipTypeId", "1")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));

        verify(stripeService, never()).createCheckoutSession(any(), any(), any(), any());
    }

    @Test
    void testPurchaseMembership_CannotPurchase_ShouldRedirectWithError() throws Exception {
        // Given
        session.setAttribute("currentUser", user);

        when(membershipTypeService.getMembershipTypeById(1L)).thenReturn(silverType);
        when(membershipService.canPurchaseMembershipType(1L, silverType))
                .thenReturn("You already have an active Silver membership");

        // When & Then
        mockMvc.perform(post("/membership/purchase")
                        .param("membershipTypeId", "1")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));

        verify(stripeService, never()).createCheckoutSession(any(), any(), any(), any());
    }

    @Test
    void testPaymentCancelled_ShouldRedirectWithError() throws Exception {
        // When & Then
        mockMvc.perform(get("/membership/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void testConfirmation_MembershipNotFound_ShouldRedirectWithError() throws Exception {
        // Given
        String membershipId = "MEM-NOTFOUND";
        when(membershipService.getMembershipByMembershipId(membershipId)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/membership/confirmation")
                        .param("id", membershipId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));

        verify(membershipService, times(1)).getMembershipByMembershipId(membershipId);
    }

    @Test
    void testConfirmation_WithError_ShouldRedirectWithError() throws Exception {
        // Given
        String membershipId = "MEM-12345678";
        when(membershipService.getMembershipByMembershipId(membershipId))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/membership/confirmation")
                        .param("id", membershipId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));

        verify(membershipService, times(1)).getMembershipByMembershipId(membershipId);
    }

    // NEW TESTS TO IMPROVE COVERAGE

    @Test
    void testPurchaseMembership_Success() throws Exception {
        // Given - successful Stripe session creation
        session.setAttribute("currentUser", user);

        com.stripe.model.checkout.Session stripeSession = mock(com.stripe.model.checkout.Session.class);
        when(stripeSession.getUrl()).thenReturn("https://checkout.stripe.com/test-session");

        when(membershipTypeService.getMembershipTypeById(1L)).thenReturn(silverType);
        when(membershipService.canPurchaseMembershipType(1L, silverType)).thenReturn(null);
        when(stripeService.createCheckoutSession(any(), any(), any(), any())).thenReturn(stripeSession);

        // When & Then
        mockMvc.perform(post("/membership/purchase")
                        .param("membershipTypeId", "1")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.stripe.com/test-session"));

        verify(stripeService, times(1)).createCheckoutSession(any(), any(), any(), any());
    }

    @Test
    void testPurchaseMembership_StripeException() throws Exception {
        // Given - Stripe throws exception
        session.setAttribute("currentUser", user);

        when(membershipTypeService.getMembershipTypeById(1L)).thenReturn(silverType);
        when(membershipService.canPurchaseMembershipType(1L, silverType)).thenReturn(null);
        when(stripeService.createCheckoutSession(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Stripe API error"));

        // When & Then
        mockMvc.perform(post("/membership/purchase")
                        .param("membershipTypeId", "1")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void testPaymentSuccess_PaidStatus() throws Exception {
        // Given - successful payment
        com.stripe.model.checkout.Session stripeSession = mock(com.stripe.model.checkout.Session.class);
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("userId", "1");
        metadata.put("membershipTypeId", "1");

        when(stripeSession.getPaymentStatus()).thenReturn("paid");
        when(stripeSession.getMetadata()).thenReturn(metadata);
        when(stripeService.retrieveSession("sess_123")).thenReturn(stripeSession);
        when(membershipTypeService.getMembershipTypeById(1L)).thenReturn(silverType);
        when(membershipService.purchaseMembership(1L, silverType)).thenReturn(activeSilverMembership);

        // When & Then
        mockMvc.perform(get("/membership/success")
                        .param("session_id", "sess_123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership/confirmation?id=MEM-12345678"))
                .andExpect(flash().attributeExists("success"));

        verify(membershipService, times(1)).purchaseMembership(1L, silverType);
    }

    @Test
    void testPaymentSuccess_NotPaid() throws Exception {
        // Given - payment not completed
        com.stripe.model.checkout.Session stripeSession = mock(com.stripe.model.checkout.Session.class);
        when(stripeSession.getPaymentStatus()).thenReturn("unpaid");
        when(stripeService.retrieveSession("sess_123")).thenReturn(stripeSession);

        // When & Then
        mockMvc.perform(get("/membership/success")
                        .param("session_id", "sess_123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));

        verify(membershipService, never()).purchaseMembership(any(), any());
    }

    @Test
    void testPaymentSuccess_InactiveMembershipType() throws Exception {
        // Given - membership type became inactive
        com.stripe.model.checkout.Session stripeSession = mock(com.stripe.model.checkout.Session.class);
        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("userId", "1");
        metadata.put("membershipTypeId", "1");

        MembershipType inactiveType = new MembershipType();
        inactiveType.setId(1L);
        inactiveType.setActive(false);

        when(stripeSession.getPaymentStatus()).thenReturn("paid");
        when(stripeSession.getMetadata()).thenReturn(metadata);
        when(stripeService.retrieveSession("sess_123")).thenReturn(stripeSession);
        when(membershipTypeService.getMembershipTypeById(1L)).thenReturn(inactiveType);

        // When & Then
        mockMvc.perform(get("/membership/success")
                        .param("session_id", "sess_123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));

        verify(membershipService, never()).purchaseMembership(any(), any());
    }

    @Test
    void testPaymentSuccess_Exception() throws Exception {
        // Given - error retrieving session
        when(stripeService.retrieveSession("sess_123"))
                .thenThrow(new RuntimeException("Session not found"));

        // When & Then
        mockMvc.perform(get("/membership/success")
                        .param("session_id", "sess_123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));
    }

    // DIRECT CONTROLLER TESTS (bypass MockMvc view resolution issues)

    @Test
    void testMembershipPage_DirectCall_WithActiveMembership() {
        // Given
        session.setAttribute("currentUser", user);
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();

        when(membershipTypeService.getActiveMembershipTypes()).thenReturn(java.util.List.of(silverType));
        when(membershipService.getActiveMembershipWithActiveType(1L)).thenReturn(activeSilverMembership);

        // When
        String viewName = membershipController.membershipPage(session, model);

        // Then
        assertEquals("membership", viewName);
        assertEquals(java.util.List.of(silverType), model.get("membershipTypes"));
        assertEquals(activeSilverMembership, model.get("currentMembership"));
        assertEquals(silverType, model.get("currentMembershipType"));
        assertEquals(user, model.get("currentUser"));
        assertEquals(1, model.get("currentTierLevel"));

        verify(membershipTypeService, times(1)).getActiveMembershipTypes();
        verify(membershipService, times(1)).getActiveMembershipWithActiveType(1L);
    }

    @Test
    void testMembershipPage_DirectCall_NoActiveMembership() {
        // Given
        session.setAttribute("currentUser", user);
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();

        when(membershipTypeService.getActiveMembershipTypes()).thenReturn(java.util.List.of(silverType));
        when(membershipService.getActiveMembershipWithActiveType(1L)).thenReturn(null);

        // When
        String viewName = membershipController.membershipPage(session, model);

        // Then
        assertEquals("membership", viewName);
        assertNull(model.get("currentMembership"));
        assertNull(model.get("currentMembershipType"));
        assertEquals(0, model.get("currentTierLevel"));
    }

    @Test
    void testConfirmation_DirectCall_WithLineBreakDescription() {
        // Given
        silverType.setDescription("Benefit 1\nBenefit 2\nBenefit 3");
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap ra =
            new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        when(membershipService.getMembershipByMembershipId("MEM-12345678"))
                .thenReturn(activeSilverMembership);

        // When
        String viewName = membershipController.confirmation("MEM-12345678", model, ra);

        // Then
        assertEquals("membership-confirmation", viewName);
        assertEquals("MEM-12345678", model.get("membershipId"));
        assertEquals("Silver Membership", model.get("tier"));
        assertEquals(99.99, model.get("price"));
        assertEquals(5.0, model.get("discountRate"));

        @SuppressWarnings("unchecked")
        java.util.List<String> benefits = (java.util.List<String>) model.get("benefitsList");
        assertEquals(3, benefits.size());
        assertEquals("Benefit 1", benefits.get(0));
        assertEquals("Benefit 2", benefits.get(1));
        assertEquals("Benefit 3", benefits.get(2));
    }

    @Test
    void testConfirmation_DirectCall_WithSentenceDescription() {
        // Given
        silverType.setDescription("First benefit. Second benefit. Third benefit.");
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap ra =
            new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        when(membershipService.getMembershipByMembershipId("MEM-12345678"))
                .thenReturn(activeSilverMembership);

        // When
        String viewName = membershipController.confirmation("MEM-12345678", model, ra);

        // Then
        assertEquals("membership-confirmation", viewName);

        @SuppressWarnings("unchecked")
        java.util.List<String> benefits = (java.util.List<String>) model.get("benefitsList");
        assertEquals(3, benefits.size());
        assertTrue(benefits.get(0).contains("First benefit"));
        assertTrue(benefits.get(1).contains("Second benefit"));
        assertTrue(benefits.get(2).contains("Third benefit"));
    }

    @Test
    void testConfirmation_DirectCall_WithEmptyDescription() {
        // Given
        silverType.setDescription("");
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap ra =
            new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        when(membershipService.getMembershipByMembershipId("MEM-12345678"))
                .thenReturn(activeSilverMembership);

        // When
        String viewName = membershipController.confirmation("MEM-12345678", model, ra);

        // Then
        assertEquals("membership-confirmation", viewName);

        @SuppressWarnings("unchecked")
        java.util.List<String> benefits = (java.util.List<String>) model.get("benefitsList");
        assertEquals(0, benefits.size());
    }

    @Test
    void testConfirmation_DirectCall_WithNullDescription() {
        // Given
        silverType.setDescription(null);
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap ra =
            new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        when(membershipService.getMembershipByMembershipId("MEM-12345678"))
                .thenReturn(activeSilverMembership);

        // When
        String viewName = membershipController.confirmation("MEM-12345678", model, ra);

        // Then
        assertEquals("membership-confirmation", viewName);

        @SuppressWarnings("unchecked")
        java.util.List<String> benefits = (java.util.List<String>) model.get("benefitsList");
        assertEquals(0, benefits.size());
    }

    @Test
    void testConfirmation_DirectCall_WithSingleLineDescription() {
        // Given
        silverType.setDescription("Single benefit only");
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap ra =
            new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        when(membershipService.getMembershipByMembershipId("MEM-12345678"))
                .thenReturn(activeSilverMembership);

        // When
        String viewName = membershipController.confirmation("MEM-12345678", model, ra);

        // Then
        assertEquals("membership-confirmation", viewName);

        @SuppressWarnings("unchecked")
        java.util.List<String> benefits = (java.util.List<String>) model.get("benefitsList");
        assertEquals(1, benefits.size());
        assertEquals("Single benefit only", benefits.get(0));
    }
}
