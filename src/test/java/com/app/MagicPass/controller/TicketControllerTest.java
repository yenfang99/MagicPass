package com.app.MagicPass.controller;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
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

    // ===================== BRANCH COVERAGE TESTS =====================

    @Test
    void ticketsKeepsExistingReqWhenFromPreviewIsTrue() throws Exception {
        User user = loggedInUser();

        // Pre-populate session with existing request
        CheckoutRequest existingReq = new CheckoutRequest();
        existingReq.setAdultQty(2);
        existingReq.setReservationDate(LocalDate.now().plusDays(1));
        session.setAttribute("previewReq", existingReq);

        PricingBreakdown existingPricing = new PricingBreakdown();
        existingPricing.setGrandTotal(200);
        session.setAttribute("previewPricing", existingPricing);

        MvcResult result = mockMvc.perform(get("/tickets")
                        .session(session)
                        .param("fromPreview", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets"))
                .andExpect(model().attributeExists("req"))
                .andReturn();

        // Verify session attributes were NOT cleared
        MockHttpSession updated = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(updated.getAttribute("previewReq"));
        assertNotNull(updated.getAttribute("previewPricing"));
    }

    @Test
    void ticketsClearsSessionWhenFromPreviewIsFalse() throws Exception {
        User user = loggedInUser();

        // Pre-populate session with existing request
        CheckoutRequest existingReq = new CheckoutRequest();
        existingReq.setAdultQty(2);
        session.setAttribute("previewReq", existingReq);
        session.setAttribute("previewPricing", new PricingBreakdown());

        MvcResult result = mockMvc.perform(get("/tickets")
                        .session(session)
                        .param("fromPreview", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets"))
                .andReturn();

        // Verify session attributes were cleared
        MockHttpSession updated = (MockHttpSession) result.getRequest().getSession(false);
        assertNull(updated.getAttribute("previewReq"));
        assertNull(updated.getAttribute("previewPricing"));
    }

    @Test
    void ticketsUsesExistingReqWithNullReservationDate() throws Exception {
        User user = loggedInUser();

        // Pre-populate session with existing request that has null reservation date
        CheckoutRequest existingReq = new CheckoutRequest();
        existingReq.setAdultQty(3);
        existingReq.setReservationDate(null); // null date
        session.setAttribute("previewReq", existingReq);

        mockMvc.perform(get("/tickets")
                        .session(session)
                        .param("fromPreview", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets"))
                .andExpect(model().attributeExists("req"));

        // The controller should set reservation date to today if null
    }

    @Test
    void previewRedirectsToLoginWhenNotAuthenticated() throws Exception {
        // Don't call loggedInUser() - session has no user

        mockMvc.perform(post("/tickets/preview")
                        .session(session)
                        .param("adultQty", "1")
                        .param("studentQty", "0")
                        .param("childQty", "0")
                        .param("reservationDate", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Please log in to purchase tickets."));
    }

    @Test
    void previewCalculatesMemberPricing() throws Exception {
        User user = loggedInUser();
        LocalDate date = LocalDate.now();
        when(datePolicyService.isWithin7Days(date)).thenReturn(true);

        // User IS a member
        when(membershipService.getActiveMembershipWithActiveType(user.getId()))
                .thenReturn(new com.app.MagicPass.model.Membership());

        PricingBreakdown pb = new PricingBreakdown();
        pb.setGrandTotal(80); // discounted price
        when(pricingService.calculate(any(CheckoutRequest.class), eq(true))).thenReturn(pb);

        mockMvc.perform(post("/tickets/preview")
                        .session(session)
                        .param("adultQty", "1")
                        .param("studentQty", "0")
                        .param("childQty", "0")
                        .param("reservationDate", date.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment"));

        // Verify member pricing was used
        verify(pricingService).calculate(any(CheckoutRequest.class), eq(true));
    }

    @Test
    void ticketsClearsSessionWhenFromPreviewIsNull() throws Exception {
        User user = loggedInUser();

        // Pre-populate session
        session.setAttribute("previewReq", new CheckoutRequest());
        session.setAttribute("previewPricing", new PricingBreakdown());

        MvcResult result = mockMvc.perform(get("/tickets")
                        .session(session))
                // No fromPreview param - should default to clearing
                .andExpect(status().isOk())
                .andExpect(view().name("tickets"))
                .andReturn();

        // Verify session attributes were cleared (fromPreview is null/false)
        MockHttpSession updated = (MockHttpSession) result.getRequest().getSession(false);
        assertNull(updated.getAttribute("previewReq"));
        assertNull(updated.getAttribute("previewPricing"));
    }
}
