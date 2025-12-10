package com.app.MagicPass.controller;

import com.app.MagicPass.model.Staff;
import com.app.MagicPass.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminDashboardController
 * Tests dashboard statistics display and authentication
 */
@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private AdminDashboardController adminDashboardController;

    private Staff staffUser;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminDashboardController).build();

        staffUser = new Staff();
        staffUser.setId(1L);
        staffUser.setEmail("admin@magicpass.com");

        session = new MockHttpSession();
    }

    @Test
    void testDashboard_WithAuthentication_ShouldDisplayDashboard() throws Exception {
        // Given
        session.setAttribute("currentStaff", staffUser);
        session.setAttribute("isStaff", true);

        when(dashboardService.getTotalActiveMembers()).thenReturn(100L);
        when(dashboardService.getTotalTicketsSold()).thenReturn(250L);
        when(dashboardService.getTotalRevenue()).thenReturn(12345.67);
        when(dashboardService.getActiveMembershipTypeCount()).thenReturn(3L);

        // When & Then
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("totalMembers", 100L))
                .andExpect(model().attribute("totalTickets", 250L))
                .andExpect(model().attribute("totalRevenue", 12345.67))
                .andExpect(model().attribute("membershipTypeCount", 3L))
                .andExpect(model().attribute("currentStaff", staffUser));

        verify(dashboardService, times(1)).getTotalActiveMembers();
        verify(dashboardService, times(1)).getTotalTicketsSold();
        verify(dashboardService, times(1)).getTotalRevenue();
        verify(dashboardService, times(1)).getActiveMembershipTypeCount();
    }

    @Test
    void testDashboard_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        // Given - no session attributes

        // When & Then
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(dashboardService, never()).getTotalActiveMembers();
        verify(dashboardService, never()).getTotalTicketsSold();
        verify(dashboardService, never()).getTotalRevenue();
        verify(dashboardService, never()).getActiveMembershipTypeCount();
    }

    @Test
    void testDashboard_WithUserButNotStaff_ShouldRedirectToLogin() throws Exception {
        // Given
        session.setAttribute("currentStaff", staffUser);
        session.setAttribute("isStaff", false);

        // When & Then
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(dashboardService, never()).getTotalActiveMembers();
    }

    @Test
    void testDashboard_WithNullStaff_ShouldRedirectToLogin() throws Exception {
        // Given
        session.setAttribute("currentStaff", null);
        session.setAttribute("isStaff", true);

        // When & Then
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(dashboardService, never()).getTotalActiveMembers();
    }

    @Test
    void testDashboard_WithZeroStatistics_ShouldDisplayDashboard() throws Exception {
        // Given
        session.setAttribute("currentStaff", staffUser);
        session.setAttribute("isStaff", true);

        when(dashboardService.getTotalActiveMembers()).thenReturn(0L);
        when(dashboardService.getTotalTicketsSold()).thenReturn(0L);
        when(dashboardService.getTotalRevenue()).thenReturn(0.0);
        when(dashboardService.getActiveMembershipTypeCount()).thenReturn(0L);

        // When & Then
        mockMvc.perform(get("/admin/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("totalMembers", 0L))
                .andExpect(model().attribute("totalTickets", 0L))
                .andExpect(model().attribute("totalRevenue", 0.0))
                .andExpect(model().attribute("membershipTypeCount", 0L));
    }

    @Test
    void testAdminHome_ShouldRedirectToDashboard() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }
}
