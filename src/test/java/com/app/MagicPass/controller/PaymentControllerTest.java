package com.app.MagicPass.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.model.Order;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.DatePolicyService;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.MembershipTypeService;
import com.app.MagicPass.service.OrderService;
import com.app.MagicPass.service.PricingService;
import com.app.MagicPass.service.StripeService;
import com.stripe.model.checkout.Session;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private PricingService pricingService;
    @Mock
    private DatePolicyService datePolicyService;
    @Mock
    private StripeService stripeService;
    @Mock
    private MembershipService membershipService;
    @Mock
    private MembershipTypeService membershipTypeService;

    @InjectMocks
    private PaymentController controller;

    private MockMvc mockMvc;
    private MockHttpSession session;
    private CheckoutRequest req;
    private PricingBreakdown pricing;
    private User user;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
        session = new MockHttpSession();

        req = new CheckoutRequest();
        req.setAdultQty(1);
        req.setStudentQty(0);
        req.setChildQty(0);
        req.setReservationDate(LocalDate.now());
        req.setUserId(1L);

        pricing = new PricingBreakdown();
        pricing.setTotal(100);
        pricing.setDiscount(0);
        pricing.setTax(6);
        pricing.setGrandTotal(106);

        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
    }

    private void seedMembershipTypes() {
        MembershipType type = new MembershipType();
        type.setDisplayName("Gold");
        type.setDiscountRate(0.1);
        type.setTierLevel(2);
        when(membershipTypeService.getActiveMembershipTypes()).thenReturn(List.of(type));
    }

    @Test
    void paymentRedirectsToTicketsWhenSessionMissing() throws Exception {
        mockMvc.perform(get("/payment").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Please enter ticket details again."));
    }

    @Test
    void paymentRedirectsToLoginWhenNotAuthenticated() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);

        mockMvc.perform(get("/payment").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Please log in to continue with payment."));
    }

    @Test
    void paymentLoadsViewWithMembershipContext() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);
        seedMembershipTypes();

        Membership membership = new Membership();
        MembershipType type = new MembershipType();
        type.setDisplayName("Gold");
        type.setDiscountRate(0.1);
        type.setTierLevel(2);
        membership.setMembershipType(type);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(membership);

        mockMvc.perform(get("/payment").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"))
                .andExpect(model().attributeExists("req"))
                .andExpect(model().attributeExists("pricing"))
                .andExpect(model().attribute("currentMembershipName", "Gold"));
    }

    @Test
    void confirmRedirectsWhenSessionExpired() throws Exception {
        mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Session expired. Please re-enter ticket details."));
    }

    @Test
    void confirmRedirectsWhenNotLoggedIn() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);

        mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("error", "Please log in to confirm your payment."));
    }

    @Test
    void confirmRejectsInvalidQuantities() throws Exception {
        req.setAdultQty(0);
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Invalid ticket input. Please re-enter."));

        assertNull(session.getAttribute("previewReq"));
        assertNull(session.getAttribute("previewPricing"));
    }

    @Test
    void confirmRejectsWhenCashInsufficient() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);
        when(datePolicyService.isWithin7Days(any())).thenReturn(true);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);
        when(pricingService.calculate(any(), eq(false))).thenReturn(pricing);

        mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CASH")
                        .param("cashReceived", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment"))
                .andExpect(flash().attribute("error", "Cash received is less than the grand total."));
    }

    @Test
    void confirmCreatesCashOrderAndClearsSession() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);
        when(datePolicyService.isWithin7Days(any())).thenReturn(true);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);
        when(pricingService.calculate(any(), eq(false))).thenReturn(pricing);

        Order saved = new Order();
        saved.setOrderCode("ABC");
        ReflectionTestUtils.setField(saved, "id", 10L);
        when(orderService.createPaidOrder(any(), any(), eq("CASH"), anyDouble(), anyDouble())).thenReturn(saved);

        MvcResult result = mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CASH")
                        .param("cashReceived", "200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/receipt/10"))
                .andReturn();

        MockHttpSession updated = (MockHttpSession) result.getRequest().getSession(false);
        assertNull(updated.getAttribute("previewReq"));
        assertNull(updated.getAttribute("previewPricing"));
        assertNull(updated.getAttribute("stripeSessionId"));

        verify(orderService).createPaidOrder(any(), any(), eq("CASH"), eq(200.0), eq(94.0));
    }

    @Test
    void confirmRejectsUnsupportedPaymentMethod() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);
        when(datePolicyService.isWithin7Days(any())).thenReturn(true);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);
        when(pricingService.calculate(any(), eq(false))).thenReturn(pricing);

        mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CRYPTO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment"))
                .andExpect(flash().attribute("error", "Unsupported payment method."));
    }

    @Test
    void paymentSuccessCreatesOrderOnCompletedStripeSession() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);

        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("complete");
        when(stripeSession.getPaymentIntent()).thenReturn(null); // skip payment method lookup
        when(stripeService.retrieveSession("sess_123")).thenReturn(stripeSession);

        Order saved = new Order();
        ReflectionTestUtils.setField(saved, "id", 5L);
        when(orderService.createPaidOrder(any(), any(), eq("CARD"), isNull(), isNull())).thenReturn(saved);

        mockMvc.perform(get("/payment/success")
                        .session(session)
                        .param("session_id", "sess_123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/receipt/5"));
    }
}
