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
}
