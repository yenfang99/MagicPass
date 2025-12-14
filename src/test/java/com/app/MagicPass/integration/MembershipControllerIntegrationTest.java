package com.app.MagicPass.integration;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.model.User;
import com.app.MagicPass.repository.MembershipRepository;
import com.app.MagicPass.repository.MembershipTypeRepository;
import com.app.MagicPass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MembershipController
 * Tests user membership viewing and purchase flow (without Stripe payment)
 *
 * Note: Stripe payment integration is mocked/skipped in tests
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MembershipControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private MockHttpSession mockSession;
    private MembershipType silverType;
    private MembershipType goldType;
    private MembershipType platinumType;

    @BeforeEach
    void setUp() {
        // Setup MockMvc
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        // Clean up before each test
        membershipRepository.deleteAll();
        membershipTypeRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("user@test.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setBirthday(LocalDate.of(1990, 1, 1));
        testUser.setAge(34);
        testUser.setGender("M");
        testUser.setPhone("0123456789");
        testUser = userRepository.save(testUser);

        // Create mock session with authenticated user
        mockSession = new MockHttpSession();
        mockSession.setAttribute("currentUser", testUser);

        // Create test membership types
        silverType = createMembershipType("Silver", "Silver Membership", 99.99, 0.05, 1, true);
        goldType = createMembershipType("Gold", "Gold Membership", 199.99, 0.10, 2, true);
        platinumType = createMembershipType("Platinum", "Platinum Membership", 299.99, 0.15, 3, true);

        silverType = membershipTypeRepository.save(silverType);
        goldType = membershipTypeRepository.save(goldType);
        platinumType = membershipTypeRepository.save(platinumType);
    }

    @Test
    void testMembershipPage_NoExistingMembership_ShouldShowAllTiers() throws Exception {
        // When & Then
        mockMvc.perform(get("/membership").session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("membership"))
                .andExpect(model().attributeExists("membershipTypes"))
                .andExpect(model().attribute("membershipTypes", hasSize(3)))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attribute("currentMembership", nullValue()))
                .andExpect(model().attribute("currentTierLevel", 0));
    }

    @Test
    void testMembershipPage_WithActiveMembership_ShouldShowCurrentMembership() throws Exception {
        // Given - user has active silver membership
        Membership activeMembership = createActiveMembership(testUser.getId(), silverType);
        membershipRepository.save(activeMembership);

        // When & Then
        mockMvc.perform(get("/membership").session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("membership"))
                .andExpect(model().attributeExists("currentMembership"))
                .andExpect(model().attribute("currentMembershipType", notNullValue()))
                .andExpect(model().attribute("currentTierLevel", 1));
    }

    @Test
    void testMembershipPage_NotLoggedIn_ShouldRedirectToLogin() throws Exception {
        // Given - no session
        MockHttpSession emptySession = new MockHttpSession();

        // When & Then
        mockMvc.perform(get("/membership").session(emptySession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testPurchaseMembership_NotLoggedIn_ShouldRedirectToLogin() throws Exception {
        // Given - no session
        MockHttpSession emptySession = new MockHttpSession();

        // When & Then
        mockMvc.perform(post("/membership/purchase").session(emptySession)
                        .param("membershipTypeId", silverType.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
                // Note: Flash attribute not set when redirected by auth interceptor
    }

    @Test
    void testPurchaseMembership_InactiveMembershipType_ShouldRejectWithError() throws Exception {
        // Given - inactive membership type
        MembershipType inactiveType = createMembershipType("Inactive", "Inactive Tier",
                                                          49.99, 0.02, 0, false);
        inactiveType = membershipTypeRepository.save(inactiveType);

        // When & Then
        mockMvc.perform(post("/membership/purchase").session(mockSession)
                        .param("membershipTypeId", inactiveType.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error", containsString("inactive")));
    }

    @Test
    void testPurchaseMembership_DowngradeAttempt_ShouldRejectWithError() throws Exception {
        // Given - user has active gold membership
        Membership activeMembership = createActiveMembership(testUser.getId(), goldType);
        membershipRepository.save(activeMembership);

        // When - try to purchase lower tier (silver)
        mockMvc.perform(post("/membership/purchase").session(mockSession)
                        .param("membershipTypeId", silverType.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"));

        // Then - verify no new membership created
        assertEquals(1, membershipRepository.count(), "Should not create downgrade membership");
    }

    @Test
    void testPaymentCancelled_ShouldRedirectWithError() throws Exception {
        // When & Then (add session for authenticated user)
        mockMvc.perform(get("/membership/cancel").session(mockSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error", containsString("cancelled")));
    }

    @Test
    void testConfirmation_ExistingMembership_ShouldDisplayDetails() throws Exception {
        // Given - create membership
        Membership membership = createActiveMembership(testUser.getId(), silverType);
        membership.setMembershipId("MEMB-2024-001");
        membership = membershipRepository.save(membership);

        // When & Then (add session for authenticated user)
        mockMvc.perform(get("/membership/confirmation")
                        .param("id", membership.getMembershipId())
                        .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("membership-confirmation"))
                .andExpect(model().attributeExists("membershipId"))
                .andExpect(model().attribute("membershipId", "MEMB-2024-001"))
                .andExpect(model().attributeExists("tier"))
                .andExpect(model().attribute("tier", "Silver Membership"))
                .andExpect(model().attributeExists("discountRate"))
                .andExpect(model().attribute("price", 99.99));
    }

    @Test
    void testConfirmation_NonExistentMembership_ShouldRedirectWithError() throws Exception {
        // Given - non-existent membership ID
        String nonExistentId = "MEMB-9999-999";

        // When & Then (add session for authenticated user)
        mockMvc.perform(get("/membership/confirmation")
                        .param("id", nonExistentId)
                        .session(mockSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/membership"))
                .andExpect(flash().attributeExists("error"))
                .andExpect(flash().attribute("error", containsString("not found")));
    }

    @Test
    void testMembershipPage_OnlyActiveTypes_ShouldDisplay() throws Exception {
        // Given - one inactive type
        platinumType.setActive(false);
        membershipTypeRepository.save(platinumType);

        // When & Then
        mockMvc.perform(get("/membership").session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("membership"))
                .andExpect(model().attributeExists("membershipTypes"))
                .andExpect(model().attribute("membershipTypes", hasSize(2))); // Only active types
    }

    @Test
    void testConfirmation_WithDescriptionBenefits_ShouldParseAndDisplay() throws Exception {
        // Given - membership type with description
        silverType.setDescription("Priority access\nDiscount on tickets\nExclusive events");
        membershipTypeRepository.save(silverType);

        Membership membership = createActiveMembership(testUser.getId(), silverType);
        membership.setMembershipId("MEMB-2024-002");
        membership = membershipRepository.save(membership);

        // When & Then (add session for authenticated user)
        mockMvc.perform(get("/membership/confirmation")
                        .param("id", membership.getMembershipId())
                        .session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("membership-confirmation"))
                .andExpect(model().attributeExists("benefitsList"))
                .andExpect(model().attribute("benefitsList", not(empty())));
    }

    // Helper method to create membership type
    private MembershipType createMembershipType(String name, String displayName, Double price,
                                                Double discountRate, Integer tierLevel, Boolean active) {
        MembershipType type = new MembershipType();
        type.setName(name);
        type.setDisplayName(displayName);
        type.setDescription("Test description for " + name);
        type.setPrice(price);
        type.setDiscountRate(discountRate);
        type.setDurationMonths(12);
        type.setActive(active);
        type.setDisplayOrder(tierLevel);
        type.setTierLevel(tierLevel);
        return type;
    }

    // Helper method to create active membership
    private Membership createActiveMembership(Long userId, MembershipType type) {
        Membership membership = new Membership();
        membership.setMembershipId("MEMB-" + System.currentTimeMillis());
        membership.setUserId(userId);
        membership.setMembershipType(type);
        membership.setPrice(type.getPrice());
        membership.setDiscountRate(type.getDiscountRate());
        membership.setStartDate(LocalDateTime.now());
        membership.setExpiryDate(LocalDateTime.now().plusMonths(type.getDurationMonths()));
        membership.setStatus(Membership.MembershipStatus.ACTIVE);
        return membership;
    }
}
