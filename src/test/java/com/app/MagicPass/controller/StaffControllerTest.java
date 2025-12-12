package com.app.MagicPass.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for StaffController
 * Tests session-based access + view rendering
 */
@ExtendWith(MockitoExtension.class)
class StaffControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private StaffController staffController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(staffController).build();
    }

    @Test
    void staffHome_whenNoIsStaffInSession_redirectToLogin() throws Exception {
        mockMvc.perform(get("/staff"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void staffHome_whenIsStaffFalse_redirectToLogin() throws Exception {
        mockMvc.perform(get("/staff").sessionAttr("isStaff", false))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void staffHome_whenIsStaffTrue_returnStaffPage() throws Exception {
        mockMvc.perform(get("/staff").sessionAttr("isStaff", true))
                .andExpect(status().isOk())
                .andExpect(view().name("staffpage"));
    }
}
