package com.app.MagicPass.controller;

import com.app.MagicPass.model.Staff;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.AuthService;
import com.app.MagicPass.service.StaffService;
import com.app.MagicPass.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for AuthController: register + login + logout
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private StaffService staffService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Prevent "Circular view path [login]" by adding a simple resolver
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // --------------------------------------------------------------------
    // REGISTER
    // --------------------------------------------------------------------

    @Test
    void showRegister_ShouldReturnRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void doRegister_PasswordMismatch_ShouldReturnRegisterWithErrorAndNotCallService()
            throws Exception {

        mockMvc.perform(post("/register")
                        .param("email", "user@test.com")
                        .param("password", "Password1!")
                        .param("confirmPassword", "Different1!"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", "Passwords do not match."));

        verify(authService, never()).register(anyString(), anyString());
    }

    @Test
    void doRegister_Success_ShouldReturnLoginWithSuccessMessage() throws Exception {
        String email = "user@test.com";
        String password = "Password1!";

        when(authService.register(email, password)).thenReturn(new User());

        mockMvc.perform(post("/register")
                        .param("email", email)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("success",
                        "Registration successful. Please log in."));

        verify(authService, times(1)).register(email, password);
    }

    @Test
    void doRegister_ServiceValidationError_ShouldReturnRegisterWithError() throws Exception {
        String email = "user@test.com";
        String password = "Password1!";
        String errorMsg = "Email is already registered."; // example message

        when(authService.register(eq(email), eq(password)))
                .thenThrow(new IllegalArgumentException(errorMsg));

        mockMvc.perform(post("/register")
                        .param("email", email)
                        .param("password", password)
                        .param("confirmPassword", password))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("error", errorMsg));

        verify(authService, times(1)).register(email, password);
    }

    // --------------------------------------------------------------------
    // LOGIN + LOGOUT  (your existing behaviour kept)
    // --------------------------------------------------------------------

    @Test
    void showLogin_ShouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void doLogin_UserSuccess_ShouldReturnHomeAndSetSession() throws Exception {
        String email = "user@test.com";
        String password = "Password1!";

        User user = new User();
        user.setId(1L);
        user.setEmail(email);

        when(staffService.isStaffEmail(email)).thenReturn(false);
        when(authService.login(email, password)).thenReturn(user);

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("success"))
                .andExpect(request().sessionAttribute("currentUser", user))
                .andExpect(request().sessionAttribute("currentStaff", nullValue()))
                .andExpect(request().sessionAttribute("isStaff", false))
                .andExpect(request().sessionAttribute("isBoss", false));

        verify(staffService, times(1)).isStaffEmail(email);
        verify(authService, times(1)).login(email, password);
    }

    @Test
    void doLogin_StaffSuccess_ShouldRedirectToAdminDashboardAndSetSession() throws Exception {
        String email = "boss@magicpass.my";
        String password = "BossPass1!";

        Staff staff = new Staff();
        staff.setId(10L);
        staff.setEmail(email);
        staff.setBoss(true);

        when(staffService.isStaffEmail(email)).thenReturn(true);
        when(staffService.loginStaff(email, password)).thenReturn(staff);

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(request().sessionAttribute("currentStaff", staff))
                .andExpect(request().sessionAttribute("currentUser", nullValue()))
                .andExpect(request().sessionAttribute("isStaff", true))
                .andExpect(request().sessionAttribute("isBoss", true));

        verify(staffService, times(1)).isStaffEmail(email);
        verify(staffService, times(1)).loginStaff(email, password);
    }

    @Test
    void doLogin_UserLoginFailure_ShouldReturnLoginWithError() throws Exception {
        String email = "user@test.com";
        String password = "WrongPass1!";
        String errorMsg = "Invalid email or password.";

        when(staffService.isStaffEmail(email)).thenReturn(false);
        when(authService.login(eq(email), eq(password)))
                .thenThrow(new IllegalArgumentException(errorMsg));

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", errorMsg))
                .andExpect(request().sessionAttribute("currentUser", nullValue()))
                .andExpect(request().sessionAttribute("currentStaff", nullValue()));

        verify(staffService, times(1)).isStaffEmail(email);
        verify(authService, times(1)).login(email, password);
    }

    @Test
    void doLogin_StaffLoginFailure_ShouldReturnLoginWithError() throws Exception {
        String email = "staff@magicpass.my";
        String password = "WrongPass1!";
        String errorMsg = "Invalid email or password.";

        when(staffService.isStaffEmail(email)).thenReturn(true);
        when(staffService.loginStaff(eq(email), eq(password)))
                .thenThrow(new IllegalArgumentException(errorMsg));

        mockMvc.perform(post("/login")
                        .param("email", email)
                        .param("password", password))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", errorMsg))
                .andExpect(request().sessionAttribute("currentUser", nullValue()))
                .andExpect(request().sessionAttribute("currentStaff", nullValue()));

        verify(staffService, times(1)).isStaffEmail(email);
        verify(staffService, times(1)).loginStaff(email, password);
    }

    @Test
    void logout_ShouldInvalidateSessionAndReturnLogin() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("success"));
    }
}
