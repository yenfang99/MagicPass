package com.app.MagicPass.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.DatePolicyService;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.PricingService;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    @Mock
    private PricingService pricingService;
    @Mock
    private DatePolicyService datePolicyService;
    @Mock
    private MembershipService membershipService;

    @InjectMocks
    private TicketController controller;

    private MockMvc mockMvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
        session = new MockHttpSession();
    }

    private User loggedInUser() {
        User user = new User();
        user.setId(1L);
        session.setAttribute("currentUser", user);
        return user;
    }

    @Test
    void ticketsRedirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Please log in to purchase tickets."));
    }

    @Test
    void ticketsLoadsViewWhenAuthenticated() throws Exception {
        loggedInUser();

        mockMvc.perform(get("/tickets").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets"))
                .andExpect(model().attributeExists("req"))
                .andExpect(model().attributeExists("currentUser"));
    }

    @Test
    void previewRejectsWhenNoQuantity() throws Exception {
        loggedInUser();

        mockMvc.perform(post("/tickets/preview")
                        .session(session)
                        .param("adultQty", "0")
                        .param("studentQty", "0")
                        .param("childQty", "0")
                        .param("reservationDate", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Please select at least 1 ticket."));
    }

    @Test
    void previewRejectsInvalidDate() throws Exception {
        loggedInUser();

        mockMvc.perform(post("/tickets/preview")
                        .session(session)
                        .param("adultQty", "1")
                        .param("studentQty", "0")
                        .param("childQty", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Please select a reservation date."));
    }

    @Test
    void previewRejectsOutsidePolicy() throws Exception {
        loggedInUser();
        LocalDate invalidDate = LocalDate.now().plusDays(10);
        when(datePolicyService.isWithin7Days(invalidDate)).thenReturn(false);

        mockMvc.perform(post("/tickets/preview")
                        .session(session)
                        .param("adultQty", "1")
                        .param("studentQty", "0")
                        .param("childQty", "0")
                        .param("reservationDate", invalidDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Invalid reservation date. Please choose a date from today within the next 7 days."));
    }

    @Test
    void previewStoresSessionAndRedirectsOnSuccess() throws Exception {
        User user = loggedInUser();
        LocalDate date = LocalDate.now();
        when(datePolicyService.isWithin7Days(date)).thenReturn(true);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);

        PricingBreakdown pb = new PricingBreakdown();
        pb.setGrandTotal(100);
        when(pricingService.calculate(any(CheckoutRequest.class), eq(false))).thenReturn(pb);

        MvcResult result = mockMvc.perform(post("/tickets/preview")
                        .session(session)
                        .param("adultQty", "1")
                        .param("studentQty", "1")
                        .param("childQty", "0")
                        .param("reservationDate", date.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment"))
                .andReturn();

        MockHttpSession updated = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(updated.getAttribute("previewReq"));
        assertNotNull(updated.getAttribute("previewPricing"));
    }
}
