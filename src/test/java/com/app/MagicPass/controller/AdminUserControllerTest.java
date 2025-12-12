package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminUserController
 * Tests CRUD operations for users and membership tracking
 */
@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private MembershipService membershipService;

    @InjectMocks
    private AdminUserController adminUserController;

    private User user1;
    private User user2;
    private User user3;
    private Membership membership1;
    private Membership membership2;
    private MembershipType silverType;
    private MembershipType goldType;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController).build();

        // Setup membership types
        silverType = new MembershipType();
        silverType.setId(1L);
        silverType.setName("Silver");
        silverType.setDisplayName("Silver Membership");
        silverType.setPrice(99.99);
        silverType.setDiscountRate(0.05);

        goldType = new MembershipType();
        goldType.setId(2L);
        goldType.setName("Gold");
        goldType.setDisplayName("Gold Membership");
        goldType.setPrice(199.99);
        goldType.setDiscountRate(0.10);

        // Setup users
        user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@test.com");
        user1.setPasswordHash("password123");

        user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@test.com");
        user2.setPasswordHash("password456");

        user3 = new User();
        user3.setId(3L);
        user3.setEmail("user3@test.com");
        user3.setPasswordHash("password789");

        // Setup memberships
        membership1 = new Membership();
        membership1.setUserId(1L);
        membership1.setMembershipType(silverType);
        membership1.setStatus(Membership.MembershipStatus.ACTIVE);
        membership1.setStartDate(LocalDate.now().minusMonths(3).atStartOfDay());
        membership1.setExpiryDate(LocalDate.now().plusMonths(9).atStartOfDay());

        membership2 = new Membership();
        membership2.setUserId(2L);
        membership2.setMembershipType(goldType);
        membership2.setStatus(Membership.MembershipStatus.ACTIVE);
        membership2.setStartDate(LocalDate.now().minusMonths(1).atStartOfDay());
        membership2.setExpiryDate(LocalDate.now().plusMonths(11).atStartOfDay());
    }

    @Test
    void testListUsers_WithMemberships_ShouldDisplayAll() throws Exception {
        // Given
        List<User> users = Arrays.asList(user1, user2, user3);
        List<Membership> memberships = Arrays.asList(membership1, membership2);

        when(userService.getAllUsers()).thenReturn(users);
        when(membershipService.getAllMemberships()).thenReturn(memberships);

        // When & Then
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("userList", users))
                .andExpect(model().attributeExists("userMembershipMap"))
                .andExpect(model().attribute("totalUsers", 3L))
                .andExpect(model().attribute("totalMembers", 2L));

        verify(userService, times(1)).getAllUsers();
        verify(membershipService, times(1)).getAllMemberships();
    }

    @Test
    void testListUsers_NoMemberships_ShouldShowZeroMembers() throws Exception {
        // Given
        List<User> users = Arrays.asList(user1, user2, user3);
        List<Membership> memberships = Arrays.asList();

        when(userService.getAllUsers()).thenReturn(users);
        when(membershipService.getAllMemberships()).thenReturn(memberships);

        // When & Then
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("userList", users))
                .andExpect(model().attribute("totalUsers", 3L))
                .andExpect(model().attribute("totalMembers", 0L));

        verify(userService, times(1)).getAllUsers();
        verify(membershipService, times(1)).getAllMemberships();
    }

    @Test
    void testListUsers_OnlyActiveMemberships_ShouldCount() throws Exception {
        // Given
        Membership expiredMembership = new Membership();
        expiredMembership.setUserId(3L);
        expiredMembership.setMembershipType(silverType);
        expiredMembership.setStatus(Membership.MembershipStatus.EXPIRED);

        List<User> users = Arrays.asList(user1, user2, user3);
        List<Membership> memberships = Arrays.asList(membership1, membership2, expiredMembership);

        when(userService.getAllUsers()).thenReturn(users);
        when(membershipService.getAllMemberships()).thenReturn(memberships);

        // When & Then
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("totalUsers", 3L))
                .andExpect(model().attribute("totalMembers", 2L)); // Only active counted

        verify(userService, times(1)).getAllUsers();
        verify(membershipService, times(1)).getAllMemberships();
    }

    @Test
    void testShowCreateForm_ShouldDisplayForm() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/users/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("mode", "create"));
    }

    @Test
    void testCreateUser_Success_ShouldRedirect() throws Exception {
        // Given
        when(userService.createUser(any(User.class))).thenReturn(user1);

        // When & Then
        mockMvc.perform(post("/admin/users/create")
                        .param("email", "newuser@test.com")
                        .param("password", "newpassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("success"));

        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    void testCreateUser_DuplicateEmail_ShouldRedirectWithError() throws Exception {
        // Given
        when(userService.createUser(any(User.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        // When & Then
        mockMvc.perform(post("/admin/users/create")
                        .param("email", "user1@test.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/create"))
                .andExpect(flash().attributeExists("error"));

        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    void testCreateUser_Exception_ShouldRedirectWithError() throws Exception {
        // Given
        when(userService.createUser(any(User.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/admin/users/create")
                        .param("email", "newuser@test.com")
                        .param("password", "newpassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/create"))
                .andExpect(flash().attributeExists("error"));

        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    void testShowEditForm_Found_ShouldDisplayForm() throws Exception {
        // Given
        Long id = 1L;
        when(userService.getUserById(id)).thenReturn(user1);

        // When & Then
        mockMvc.perform(get("/admin/users/edit/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(model().attribute("user", user1))
                .andExpect(model().attribute("mode", "edit"));

        verify(userService, times(1)).getUserById(id);
    }

    @Test
    void testShowEditForm_NotFound_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 999L;
        when(userService.getUserById(id))
                .thenThrow(new RuntimeException("User not found"));

        // When & Then
        mockMvc.perform(get("/admin/users/edit/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("error"));

        verify(userService, times(1)).getUserById(id);
    }

    @Test
    void testUpdateUser_Success_ShouldRedirect() throws Exception {
        // Given
        Long id = 1L;
        when(userService.updateUser(eq(id), any(User.class))).thenReturn(user1);

        // When & Then
        mockMvc.perform(post("/admin/users/edit/{id}", id)
                        .param("email", "updated@test.com")
                        .param("password", "newpassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("success"));

        verify(userService, times(1)).updateUser(eq(id), any(User.class));
    }

    @Test
    void testUpdateUser_NotFound_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 999L;
        when(userService.updateUser(eq(id), any(User.class)))
                .thenThrow(new RuntimeException("User not found"));

        // When & Then
        mockMvc.perform(post("/admin/users/edit/{id}", id)
                        .param("email", "updated@test.com")
                        .param("password", "newpassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/edit/" + id))
                .andExpect(flash().attributeExists("error"));

        verify(userService, times(1)).updateUser(eq(id), any(User.class));
    }

    @Test
    void testUpdateUser_DuplicateEmail_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 1L;
        when(userService.updateUser(eq(id), any(User.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

        // When & Then
        mockMvc.perform(post("/admin/users/edit/{id}", id)
                        .param("email", "user2@test.com")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/edit/" + id))
                .andExpect(flash().attributeExists("error"));

        verify(userService, times(1)).updateUser(eq(id), any(User.class));
    }

    @Test
    void testDeleteUser_Success_ShouldRedirect() throws Exception {
        // Given
        Long id = 1L;
        doNothing().when(userService).deleteUser(id);

        // When & Then
        mockMvc.perform(post("/admin/users/delete/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("success"));

        verify(userService, times(1)).deleteUser(id);
    }

    @Test
    void testDeleteUser_NotFound_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 999L;
        doThrow(new RuntimeException("User not found")).when(userService).deleteUser(id);

        // When & Then
        mockMvc.perform(post("/admin/users/delete/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("error"));

        verify(userService, times(1)).deleteUser(id);
    }

    @Test
    void testDeleteUser_WithActiveMembership_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 1L;
        doThrow(new IllegalStateException("Cannot delete user with active membership"))
                .when(userService).deleteUser(id);

        // When & Then
        mockMvc.perform(post("/admin/users/delete/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("error"));

        verify(userService, times(1)).deleteUser(id);
    }

    @Test
    void testListUsers_EmptyList_ShouldDisplayZeroStats() throws Exception {
        // Given
        List<User> users = Arrays.asList();
        List<Membership> memberships = Arrays.asList();

        when(userService.getAllUsers()).thenReturn(users);
        when(membershipService.getAllMemberships()).thenReturn(memberships);

        // When & Then
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("userList", users))
                .andExpect(model().attribute("totalUsers", 0L))
                .andExpect(model().attribute("totalMembers", 0L));

        verify(userService, times(1)).getAllUsers();
        verify(membershipService, times(1)).getAllMemberships();
    }
}
