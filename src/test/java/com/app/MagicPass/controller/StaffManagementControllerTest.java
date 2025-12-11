package com.app.MagicPass.controller;

import com.app.MagicPass.service.StaffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class StaffManagementControllerTest {

    @Mock
    private StaffService staffService;

    @InjectMocks
    private StaffManagementController staffManagementController;

    private MockMvc mockMvc;
    private MockHttpSession bossSession;
    private MockHttpSession normalSession;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(staffManagementController)
                .build();

        // Boss session (isStaff = true, isBoss = true)
        bossSession = new MockHttpSession();
        bossSession.setAttribute("isStaff", true);
        bossSession.setAttribute("isBoss", true);

        // Normal / not-boss session
        normalSession = new MockHttpSession();
        normalSession.setAttribute("isStaff", false);
        normalSession.setAttribute("isBoss", false);
    }

    // ---------------------------------------------------------------------
    // GET /staff/manage
    // ---------------------------------------------------------------------

    @Test
    public void manageStaff_AsBoss_ShouldShowManagePage() throws Exception {
        mockMvc.perform(get("/staff/manage").session(bossSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/managestaff"))
                .andExpect(model().attributeExists("staffList"));

        verify(staffService, times(1)).getAllStaff();
    }

    @Test
    public void manageStaff_NotBoss_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/staff/manage").session(normalSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(staffService, never()).getAllStaff();
    }

    // ---------------------------------------------------------------------
    // GET /staff/manage/add
    // ---------------------------------------------------------------------

    @Test
    public void showAddStaffPage_AsBoss_ShouldShowAddStaffView() throws Exception {
        mockMvc.perform(get("/staff/manage/add").session(bossSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/addstaff"));
    }

    @Test
    public void showAddStaffPage_NotBoss_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/staff/manage/add").session(normalSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    // ---------------------------------------------------------------------
    // POST /staff/manage/add
    // ---------------------------------------------------------------------

    @Test
    public void addStaff_AsBoss_Success_ShouldRedirectWithFlashSuccess() throws Exception {
        String email = "staff@magicpass.my";
        String password = "Password1!";

        mockMvc.perform(post("/staff/manage/add")
                        .param("email", email)
                        .param("password", password)
                        .session(bossSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/manage"))
                .andExpect(flash().attribute("success",
                        "Staff account created successfully."));

        verify(staffService, times(1)).createStaff(email, password);
    }

    @Test
    public void addStaff_AsBoss_ServiceThrows_ShouldReturnAddStaffWithError() throws Exception {
        String email = "staff@magicpass.my";
        String password = "Password1!";
        String errorMsg = "This staff email already exists.";

        doThrow(new IllegalArgumentException(errorMsg))
                .when(staffService).createStaff(email, password);

        mockMvc.perform(post("/staff/manage/add")
                        .param("email", email)
                        .param("password", password)
                        .session(bossSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/addstaff"))
                .andExpect(model().attribute("error", errorMsg))
                .andExpect(model().attribute("email", email));

        verify(staffService, times(1)).createStaff(email, password);
    }

    @Test
    public void addStaff_NotBoss_ShouldRedirectToLogin_AndNotCallService() throws Exception {
        mockMvc.perform(post("/staff/manage/add")
                        .param("email", "staff@magicpass.my")
                        .param("password", "Password1!")
                        .session(normalSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(staffService, never()).createStaff(anyString(), anyString());
    }

    // ---------------------------------------------------------------------
    // POST /staff/manage/delete
    // ---------------------------------------------------------------------

    @Test
    public void deleteStaff_AsBoss_Success_ShouldRedirectWithSuccessFlash() throws Exception {
        Long id = 10L;

        mockMvc.perform(post("/staff/manage/delete")
                        .param("id", String.valueOf(id))
                        .session(bossSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/manage"))
                .andExpect(flash().attribute("success", "Staff account removed."));

        verify(staffService, times(1)).deleteStaff(id);
    }

    @Test
    public void deleteStaff_AsBoss_ServiceThrows_ShouldRedirectWithErrorFlash() throws Exception {
        Long id = 10L;
        String errorMsg = "Boss account cannot be removed.";

        doThrow(new IllegalArgumentException(errorMsg))
                .when(staffService).deleteStaff(id);

        mockMvc.perform(post("/staff/manage/delete")
                        .param("id", String.valueOf(id))
                        .session(bossSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/manage"))
                .andExpect(flash().attribute("error", errorMsg));

        verify(staffService, times(1)).deleteStaff(id);
    }

    @Test
    public void deleteStaff_NotBoss_ShouldRedirectToLogin_AndNotCallService() throws Exception {
        mockMvc.perform(post("/staff/manage/delete")
                        .param("id", "5")
                        .session(normalSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(staffService, never()).deleteStaff(anyLong());
    }
}
