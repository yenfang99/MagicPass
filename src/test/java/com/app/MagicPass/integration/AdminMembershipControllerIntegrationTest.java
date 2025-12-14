package com.app.MagicPass.integration;

import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.model.User;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminMembershipController
 * Tests admin CRUD operations for membership types
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminMembershipControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MembershipTypeRepository membershipTypeRepository;

    @Autowired
    private UserRepository userRepository;

    private User testStaff;
    private MockHttpSession mockSession;

    @BeforeEach
    void setUp() {
        // Setup MockMvc
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        // Clean up before each test
        membershipTypeRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test staff user
        testStaff = new User();
        testStaff.setName("Test Staff");
        testStaff.setEmail("staff@test.com");
        testStaff.setPasswordHash("hashedPassword");
        testStaff.setBirthday(LocalDate.of(1990, 1, 1));
        testStaff.setAge(34);
        testStaff.setGender("M");
        testStaff.setPhone("0123456789");
        testStaff = userRepository.save(testStaff);

        // Create mock session with authenticated staff user
        mockSession = new MockHttpSession();
        mockSession.setAttribute("currentStaff", testStaff);
    }

    @Test
    void testListMembershipTypes_ShouldReturnAll() throws Exception {
        // Given - create test membership types
        MembershipType silver = createMembershipType("Silver", "Silver Membership", 99.99, 0.05, 1);
        MembershipType gold = createMembershipType("Gold", "Gold Membership", 199.99, 0.10, 2);
        membershipTypeRepository.save(silver);
        membershipTypeRepository.save(gold);

        // When & Then
        mockMvc.perform(get("/admin/membership-types").session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/membership-types"))
                .andExpect(model().attributeExists("membershipTypes"))
                .andExpect(model().attribute("membershipTypes", hasSize(2)));
    }

    @Test
    void testShowCreateForm_ShouldDisplayForm() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/membership-types/create").session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/membership-type-form"))
                .andExpect(model().attributeExists("membershipType"))
                .andExpect(model().attribute("mode", "create"));
    }

    @Test
    void testCreateMembershipType_ValidData_ShouldSaveToDatabase() throws Exception {
        // When
        mockMvc.perform(post("/admin/membership-types/create").session(mockSession)
                        .param("name", "Platinum")
                        .param("displayName", "Platinum Membership")
                        .param("description", "Premium tier with best benefits")
                        .param("price", "299.99")
                        .param("discountRate", "0.15")
                        .param("durationMonths", "12")
                        .param("active", "true")
                        .param("displayOrder", "3")
                        .param("tierLevel", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("success"));

        // Then - verify saved to database
        assertEquals(1, membershipTypeRepository.count(), "Membership type should be saved");
        MembershipType saved = membershipTypeRepository.findAll().get(0);
        assertEquals("Platinum", saved.getName());
        assertEquals("Platinum Membership", saved.getDisplayName());
        assertEquals(299.99, saved.getPrice());
        assertEquals(0.15, saved.getDiscountRate());
        assertEquals(3, saved.getTierLevel());
        assertTrue(saved.getActive());
    }

    @Test
    void testCreateMembershipType_DuplicateName_ShouldRedirectWithError() throws Exception {
        // Given - existing membership type
        MembershipType existing = createMembershipType("Silver", "Silver Membership", 99.99, 0.05, 1);
        membershipTypeRepository.save(existing);

        // When & Then
        mockMvc.perform(post("/admin/membership-types/create").session(mockSession)
                        .param("name", "Silver")
                        .param("displayName", "Silver Membership 2")
                        .param("price", "99.99")
                        .param("discountRate", "0.05")
                        .param("durationMonths", "12")
                        .param("active", "true")
                        .param("displayOrder", "1")
                        .param("tierLevel", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types/create"))
                .andExpect(flash().attributeExists("error"));

        // Then - verify only one exists
        assertEquals(1, membershipTypeRepository.count());
    }

    @Test
    void testShowEditForm_ExistingMembershipType_ShouldDisplayForm() throws Exception {
        // Given
        MembershipType membershipType = createMembershipType("Gold", "Gold Membership", 199.99, 0.10, 2);
        membershipType = membershipTypeRepository.save(membershipType);

        // When & Then
        mockMvc.perform(get("/admin/membership-types/edit/" + membershipType.getId()).session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/membership-type-form"))
                .andExpect(model().attributeExists("membershipType"))
                .andExpect(model().attribute("mode", "edit"));
    }

    @Test
    void testShowEditForm_NonExistent_ShouldRedirectWithError() throws Exception {
        // Given - non-existent ID
        Long nonExistentId = 99999L;

        // When & Then
        mockMvc.perform(get("/admin/membership-types/edit/" + nonExistentId).session(mockSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void testUpdateMembershipType_ValidData_ShouldUpdateInDatabase() throws Exception {
        // Given - create initial membership type
        MembershipType original = createMembershipType("Silver", "Silver Membership", 99.99, 0.05, 1);
        original = membershipTypeRepository.save(original);
        Long id = original.getId();

        // When - update the membership type
        mockMvc.perform(post("/admin/membership-types/edit/" + id).session(mockSession)
                        .param("name", "Silver")
                        .param("displayName", "Enhanced Silver Membership")
                        .param("description", "Updated benefits")
                        .param("price", "109.99")
                        .param("discountRate", "0.07")
                        .param("durationMonths", "12")
                        .param("active", "true")
                        .param("displayOrder", "1")
                        .param("tierLevel", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("success"));

        // Then - verify updated in database
        MembershipType updated = membershipTypeRepository.findById(id).orElseThrow();
        assertEquals("Enhanced Silver Membership", updated.getDisplayName());
        assertEquals(109.99, updated.getPrice());
        assertEquals(0.07, updated.getDiscountRate());
    }

    @Test
    void testDeleteMembershipType_ExistingType_ShouldRemoveFromDatabase() throws Exception {
        // Given
        MembershipType membershipType = createMembershipType("Bronze", "Bronze Membership", 49.99, 0.03, 1);
        membershipType = membershipTypeRepository.save(membershipType);
        Long id = membershipType.getId();

        // When
        mockMvc.perform(post("/admin/membership-types/delete/" + id).session(mockSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("success"));

        // Then - verify deleted from database
        assertTrue(membershipTypeRepository.findById(id).isEmpty(), "Membership type should be deleted");
        assertEquals(0, membershipTypeRepository.count());
    }

    @Test
    void testToggleActiveStatus_ShouldChangeStatus() throws Exception {
        // Given - active membership type
        MembershipType membershipType = createMembershipType("Gold", "Gold Membership", 199.99, 0.10, 2);
        membershipType.setActive(true);
        membershipType = membershipTypeRepository.save(membershipType);
        Long id = membershipType.getId();

        // When - toggle to inactive
        mockMvc.perform(post("/admin/membership-types/toggle/" + id).session(mockSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("success"));

        // Then - verify status changed
        MembershipType toggled = membershipTypeRepository.findById(id).orElseThrow();
        assertFalse(toggled.getActive(), "Status should be toggled to inactive");

        // When - toggle back to active
        mockMvc.perform(post("/admin/membership-types/toggle/" + id).session(mockSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("success"));

        // Then - verify status changed back
        MembershipType toggledBack = membershipTypeRepository.findById(id).orElseThrow();
        assertTrue(toggledBack.getActive(), "Status should be toggled back to active");
    }

    @Test
    void testListMembershipTypes_EmptyList_ShouldReturnEmpty() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/membership-types").session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/membership-types"))
                .andExpect(model().attributeExists("membershipTypes"))
                .andExpect(model().attribute("membershipTypes", hasSize(0)));
    }

    // Helper method to create membership type
    private MembershipType createMembershipType(String name, String displayName, Double price,
                                                Double discountRate, Integer tierLevel) {
        MembershipType type = new MembershipType();
        type.setName(name);
        type.setDisplayName(displayName);
        type.setDescription("Test description for " + name);
        type.setPrice(price);
        type.setDiscountRate(discountRate);
        type.setDurationMonths(12);
        type.setActive(true);
        type.setDisplayOrder(tierLevel);
        type.setTierLevel(tierLevel);
        return type;
    }
}
