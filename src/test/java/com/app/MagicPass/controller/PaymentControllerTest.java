package com.app.MagicPass.controller;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
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

    private void seedMembershipTypes(MembershipType... types) {
        when(membershipTypeService.getActiveMembershipTypes()).thenReturn(List.of(types));
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
        void paymentShowsJoinMembershipHintWhenNoActiveMembership() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        MembershipType gold = new MembershipType();
        gold.setDisplayName("Gold");
        gold.setDiscountRate(0.1);
        gold.setTierLevel(2);
        seedMembershipTypes(gold);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);

        mockMvc.perform(get("/payment").session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("payment"))
            .andExpect(model().attribute("membershipHint",
                "Want lower prices? Buy a membership to unlock ticket discounts before checkout."))
            .andExpect(model().attribute("canUpgradeMembership", true));
        }

        @Test
        void paymentShowsUpgradeHintWhenLowerTierMembership() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        MembershipType silver = new MembershipType();
        silver.setDisplayName("Silver");
        silver.setDiscountRate(0.05);
        silver.setTierLevel(1);

        MembershipType gold = new MembershipType();
        gold.setDisplayName("Gold");
        gold.setDiscountRate(0.1);
        gold.setTierLevel(2);

        seedMembershipTypes(silver, gold);

        Membership membership = new Membership();
        membership.setMembershipType(silver);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(membership);

        mockMvc.perform(get("/payment").session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("payment"))
            .andExpect(model().attribute("membershipHint",
                "You're on Silver. Upgrade to a higher tier for bigger discounts."))
            .andExpect(model().attribute("canUpgradeMembership", true))
            .andExpect(model().attribute("topMembershipName", "Gold"));
        }

    @Test
    void paymentShowsTopTierHintWhenAlreadyAtHighestTier() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        MembershipType gold = new MembershipType();
        gold.setDisplayName("Gold");
        gold.setDiscountRate(0.1);
        gold.setTierLevel(2);

        seedMembershipTypes(gold);

        Membership membership = new Membership();
        membership.setMembershipType(gold);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(membership);

        mockMvc.perform(get("/payment").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"))
                .andExpect(model().attribute("membershipHint",
                        "You're on our top membership (Gold) and already getting the maximum discount."))
                .andExpect(model().attribute("canUpgradeMembership", false));
    }

    @Test
    void paymentHandlesMembershipTypeWithNullTierLevel() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        MembershipType typeWithNullTier = new MembershipType();
        typeWithNullTier.setDisplayName("Basic");
        typeWithNullTier.setDiscountRate(0.05);
        typeWithNullTier.setTierLevel(null); // null tier level

        MembershipType gold = new MembershipType();
        gold.setDisplayName("Gold");
        gold.setDiscountRate(0.1);
        gold.setTierLevel(2);

        seedMembershipTypes(typeWithNullTier, gold);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);

        mockMvc.perform(get("/payment").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"))
                .andExpect(model().attribute("topMembershipName", "Gold"));
    }

    @Test
    void paymentHandlesNoActiveMembershipTypes() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        when(membershipTypeService.getActiveMembershipTypes()).thenReturn(List.of());
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);

        mockMvc.perform(get("/payment").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"))
                .andExpect(model().attribute("topMembershipName", "top tier"))
                .andExpect(model().attribute("canUpgradeMembership", false));
    }

    @Test
    void paymentHandlesMembershipWithNullTierLevel() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        MembershipType typeWithNullTier = new MembershipType();
        typeWithNullTier.setDisplayName("Basic");
        typeWithNullTier.setDiscountRate(0.05);
        typeWithNullTier.setTierLevel(null);

        MembershipType gold = new MembershipType();
        gold.setDisplayName("Gold");
        gold.setDiscountRate(0.1);
        gold.setTierLevel(2);

        seedMembershipTypes(typeWithNullTier, gold);

        Membership membership = new Membership();
        membership.setMembershipType(typeWithNullTier);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(membership);

        mockMvc.perform(get("/payment").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"))
                .andExpect(model().attribute("canUpgradeMembership", true));
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
    void confirmRejectsCashWithoutAmount() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);
        when(datePolicyService.isWithin7Days(any())).thenReturn(true);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);
        when(pricingService.calculate(any(), eq(false))).thenReturn(pricing);

        mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment"))
                .andExpect(flash().attribute("error", "Please enter the cash amount received."));
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
    void confirmRejectsNullReservationDate() throws Exception {
        req.setReservationDate(null);
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Invalid ticket input. Please re-enter."));
    }

    @Test
    void confirmRejectsDateOutsidePolicy() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);
        when(datePolicyService.isWithin7Days(any())).thenReturn(false);

        mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Invalid ticket input. Please re-enter."));
    }

    @Test
    void confirmCalculatesMemberPricing() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);
        when(datePolicyService.isWithin7Days(any())).thenReturn(true);

        Membership membership = new Membership();
        MembershipType type = new MembershipType();
        type.setDisplayName("Gold");
        membership.setMembershipType(type);
        when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(membership);

        PricingBreakdown memberPricing = new PricingBreakdown();
        memberPricing.setTotal(100);
        memberPricing.setDiscount(10);
        memberPricing.setTax(5.4);
        memberPricing.setGrandTotal(95.4);
        when(pricingService.calculate(any(), eq(true))).thenReturn(memberPricing);

        Order saved = new Order();
        ReflectionTestUtils.setField(saved, "id", 30L);
        when(orderService.createPaidOrder(any(), any(), eq("CASH"), anyDouble(), anyDouble())).thenReturn(saved);

        mockMvc.perform(post("/payment/confirm").session(session)
                        .param("paymentMethod", "CASH")
                        .param("cashReceived", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/receipt/30"));

        verify(pricingService).calculate(any(), eq(true));
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
            void confirmRedirectsToStripeCheckoutAndStoresSession() throws Exception {
            session.setAttribute("previewReq", req);
            session.setAttribute("previewPricing", pricing);
            session.setAttribute("currentUser", user);
            when(datePolicyService.isWithin7Days(any())).thenReturn(true);
            when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);
            when(pricingService.calculate(any(), eq(false))).thenReturn(pricing);

            Session stripeCheckoutSession = mock(Session.class);
            when(stripeCheckoutSession.getId()).thenReturn("cs_test_123");
            when(stripeCheckoutSession.getUrl()).thenReturn("https://stripe.test/checkout");
            when(stripeService.createTicketCheckoutSession(anyDouble(), any(), anyString(), anyString()))
                .thenReturn(stripeCheckoutSession);

            MvcResult result = mockMvc.perform(post("/payment/confirm").session(session)
                    .param("paymentMethod", "STRIPE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://stripe.test/checkout"))
                .andReturn();

            MockHttpSession updated = (MockHttpSession) result.getRequest().getSession(false);
            assertNotNull(updated.getAttribute("stripeSessionId"));
            assertEquals("cs_test_123", updated.getAttribute("stripeSessionId"));
            }

            @Test
            void confirmReturnsToPaymentWhenStripeCreationFails() throws Exception {
            session.setAttribute("previewReq", req);
            session.setAttribute("previewPricing", pricing);
            session.setAttribute("currentUser", user);
            when(datePolicyService.isWithin7Days(any())).thenReturn(true);
            when(membershipService.getActiveMembershipWithActiveType(user.getId())).thenReturn(null);
            when(pricingService.calculate(any(), eq(false))).thenReturn(pricing);
            when(stripeService.createTicketCheckoutSession(anyDouble(), any(CheckoutRequest.class), anyString(), anyString()))
                .thenThrow(new RuntimeException("Stripe down"));

            mockMvc.perform(post("/payment/confirm").session(session)
                    .param("paymentMethod", "STRIPE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment"))
                .andExpect(flash().attribute("error", "Payment processing failed: Stripe down"));
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

    @Test
    void paymentSuccessRejectsWhenStripeSessionIncomplete() throws Exception {
        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("open");
        when(stripeService.retrieveSession("sess_incomplete")).thenReturn(stripeSession);

        mockMvc.perform(get("/payment/success")
                        .session(session)
                        .param("session_id", "sess_incomplete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Payment was not completed."));
    }

    @Test
    void paymentSuccessRedirectsWhenSessionAttributesMissing() throws Exception {
        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("complete");
        when(stripeService.retrieveSession("sess_no_context")).thenReturn(stripeSession);

        mockMvc.perform(get("/payment/success")
                        .session(session)
                        .param("session_id", "sess_no_context"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Session expired. Contact support with payment confirmation."));
    }

    @Test
    void paymentSuccessHandlesStripeVerificationFailure() throws Exception {
        when(stripeService.retrieveSession("sess_error")).thenThrow(new RuntimeException("Stripe error"));

        mockMvc.perform(get("/payment/success")
                        .session(session)
                        .param("session_id", "sess_error"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Payment verification failed: Stripe error"));
    }

    // ===================== RECEIPT TESTS =====================

    @Test
    void receiptShowsOrderWithoutBackButton() throws Exception {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 1L);

        when(orderService.getById(1L)).thenReturn(order);

        mockMvc.perform(get("/receipt/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("receipt"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("showBackToHistory", false));
    }

    @Test
    void receiptShowsBackButtonWhenFromHistory() throws Exception {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 2L);

        when(orderService.getById(2L)).thenReturn(order);

        mockMvc.perform(get("/receipt/2").param("from", "history"))
                .andExpect(status().isOk())
                .andExpect(view().name("receipt"))
                .andExpect(model().attribute("showBackToHistory", true));
    }

    @Test
    void receiptShowsBackButtonCaseInsensitive() throws Exception {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 3L);

        when(orderService.getById(3L)).thenReturn(order);

        mockMvc.perform(get("/receipt/3").param("from", "HISTORY"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("showBackToHistory", true));
    }

    @Test
    void receiptHidesBackButtonWhenFromIsOther() throws Exception {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", 4L);

        when(orderService.getById(4L)).thenReturn(order);

        mockMvc.perform(get("/receipt/4").param("from", "somewhere"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("showBackToHistory", false));
    }

    // ===================== PAYMENT CANCEL TEST =====================

    @Test
    void paymentCancelRedirectsWithError() throws Exception {
        mockMvc.perform(get("/payment/cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payment"))
                .andExpect(flash().attribute("error", "Payment was cancelled."));
    }

    // ===================== PAYMENT SUCCESS - STRIPE PAYMENT METHOD RETRIEVAL =====================

    @Test
    void paymentSuccessRetrievesPaymentMethodFromStripe() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);

        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("complete");
        when(stripeSession.getPaymentIntent()).thenReturn("pi_123");
        when(stripeService.retrieveSession("sess_with_pm")).thenReturn(stripeSession);

        com.stripe.model.PaymentIntent paymentIntent = mock(com.stripe.model.PaymentIntent.class);
        when(paymentIntent.getPaymentMethod()).thenReturn("pm_456");

        com.stripe.model.PaymentMethod paymentMethod = mock(com.stripe.model.PaymentMethod.class);
        when(paymentMethod.getType()).thenReturn("card");

        Order saved = new Order();
        ReflectionTestUtils.setField(saved, "id", 20L);

        try (var piMock = org.mockito.Mockito.mockStatic(com.stripe.model.PaymentIntent.class);
             var pmMock = org.mockito.Mockito.mockStatic(com.stripe.model.PaymentMethod.class)) {

            piMock.when(() -> com.stripe.model.PaymentIntent.retrieve("pi_123")).thenReturn(paymentIntent);
            pmMock.when(() -> com.stripe.model.PaymentMethod.retrieve("pm_456")).thenReturn(paymentMethod);

            when(orderService.createPaidOrder(any(), any(), eq("CARD"), isNull(), isNull())).thenReturn(saved);

            mockMvc.perform(get("/payment/success")
                            .session(session)
                            .param("session_id", "sess_with_pm"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/receipt/20"));
        }
    }

    @Test
    void paymentSuccessDefaultsToCardWhenPaymentMethodRetrievalFails() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);

        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("complete");
        when(stripeSession.getPaymentIntent()).thenReturn("pi_fail");
        when(stripeService.retrieveSession("sess_pm_fail")).thenReturn(stripeSession);

        Order saved = new Order();
        ReflectionTestUtils.setField(saved, "id", 21L);

        try (var piMock = org.mockito.Mockito.mockStatic(com.stripe.model.PaymentIntent.class)) {
            piMock.when(() -> com.stripe.model.PaymentIntent.retrieve("pi_fail"))
                    .thenThrow(new RuntimeException("Stripe API error"));

            when(orderService.createPaidOrder(any(), any(), eq("CARD"), isNull(), isNull())).thenReturn(saved);

            mockMvc.perform(get("/payment/success")
                            .session(session)
                            .param("session_id", "sess_pm_fail"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/receipt/21"));
        }
    }

    @Test
    void paymentSuccessDefaultsToCardWhenPaymentMethodIsNull() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);

        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("complete");
        when(stripeSession.getPaymentIntent()).thenReturn("pi_null_pm");
        when(stripeService.retrieveSession("sess_null_pm")).thenReturn(stripeSession);

        com.stripe.model.PaymentIntent paymentIntent = mock(com.stripe.model.PaymentIntent.class);
        when(paymentIntent.getPaymentMethod()).thenReturn(null);

        Order saved = new Order();
        ReflectionTestUtils.setField(saved, "id", 22L);

        try (var piMock = org.mockito.Mockito.mockStatic(com.stripe.model.PaymentIntent.class)) {
            piMock.when(() -> com.stripe.model.PaymentIntent.retrieve("pi_null_pm")).thenReturn(paymentIntent);

            when(orderService.createPaidOrder(any(), any(), eq("CARD"), isNull(), isNull())).thenReturn(saved);

            mockMvc.perform(get("/payment/success")
                            .session(session)
                            .param("session_id", "sess_null_pm"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/receipt/22"));
        }
    }

    @Test
    void paymentSuccessRedirectsWhenSessionExpiredAfterStripeComplete() throws Exception {
        // This test covers the branch where req == null || pricing == null AFTER Stripe session is complete
        // Don't set previewReq and previewPricing in session
        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("complete");
        when(stripeService.retrieveSession("sess_expired")).thenReturn(stripeSession);

        mockMvc.perform(get("/payment/success")
                        .session(session)
                        .param("session_id", "sess_expired"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Session expired. Contact support with payment confirmation."));
    }

    @Test
    void paymentSuccessHandlesNullPaymentIntent() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);

        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("complete");
        when(stripeSession.getPaymentIntent()).thenReturn(null); // Null payment intent
        when(stripeService.retrieveSession("sess_no_pi")).thenReturn(stripeSession);

        Order saved = new Order();
        ReflectionTestUtils.setField(saved, "id", 23L);

        when(orderService.createPaidOrder(any(), any(), eq("CARD"), isNull(), isNull())).thenReturn(saved);

        mockMvc.perform(get("/payment/success")
                        .session(session)
                        .param("session_id", "sess_no_pi"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/receipt/23"));
    }

    @Test
    void confirmHandlesNonMemberPricing() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        when(datePolicyService.isWithin7Days(any(LocalDate.class))).thenReturn(true);
        when(membershipService.getActiveMembershipWithActiveType(1L)).thenReturn(null); // Not a member
        when(pricingService.calculate(any(), eq(false))).thenReturn(pricing); // false for non-member

        Order saved = new Order();
        ReflectionTestUtils.setField(saved, "id", 24L);
        when(orderService.createPaidOrder(any(), any(), eq("CASH"), eq(200.0), anyDouble())).thenReturn(saved);

        mockMvc.perform(post("/payment/confirm")
                        .session(session)
                        .param("paymentMethod", "cash")
                        .param("cashReceived", "200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/receipt/24"));

        verify(pricingService).calculate(any(), eq(false));
    }

    @Test
    void confirmHandlesZeroQuantity() throws Exception {
        CheckoutRequest zeroReq = new CheckoutRequest();
        zeroReq.setAdultQty(0);
        zeroReq.setStudentQty(0);
        zeroReq.setChildQty(0);
        zeroReq.setReservationDate(LocalDate.now());
        zeroReq.setUserId(1L);

        session.setAttribute("previewReq", zeroReq);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        mockMvc.perform(post("/payment/confirm")
                        .session(session)
                        .param("paymentMethod", "STRIPE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Invalid ticket input. Please re-enter."));
    }

    @Test
    void paymentHandlesNullCurrentType() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        // User has membership but with null membership type
        Membership membershipWithNullType = new Membership();
        membershipWithNullType.setMembershipType(null);
        when(membershipService.getActiveMembershipWithActiveType(1L)).thenReturn(membershipWithNullType);

        MembershipType topType = new MembershipType();
        topType.setDisplayName("Platinum");
        topType.setDiscountRate(0.2);
        topType.setTierLevel(3);
        seedMembershipTypes(topType);

        mockMvc.perform(get("/payment").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"))
                .andExpect(model().attribute("canUpgradeMembership", true));
    }

    @Test
    void paymentHandlesTopMembershipTypeWithNullTierLevel() throws Exception {
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        when(membershipService.getActiveMembershipWithActiveType(1L)).thenReturn(null);

        // All membership types have null tier levels - should filter them all out
        MembershipType typeWithNullTier = new MembershipType();
        typeWithNullTier.setDisplayName("Basic");
        typeWithNullTier.setTierLevel(null);
        seedMembershipTypes(typeWithNullTier);

        mockMvc.perform(get("/payment").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"))
                .andExpect(model().attribute("topMembershipName", "top tier")); // Falls back to default
    }

    @Test
    void paymentSuccessWithOnlyPricingNull() throws Exception {
        // Only req is set, pricing is null
        session.setAttribute("previewReq", req);
        // Don't set previewPricing

        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("complete");
        when(stripeService.retrieveSession("sess_pricing_null")).thenReturn(stripeSession);

        mockMvc.perform(get("/payment/success")
                        .session(session)
                        .param("session_id", "sess_pricing_null"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Session expired. Contact support with payment confirmation."));
    }

    @Test
    void paymentSuccessWithOnlyReqNull() throws Exception {
        // Only pricing is set, req is null
        session.setAttribute("previewPricing", pricing);
        // Don't set previewReq

        Session stripeSession = mock(Session.class);
        when(stripeSession.getStatus()).thenReturn("complete");
        when(stripeService.retrieveSession("sess_req_null")).thenReturn(stripeSession);

        mockMvc.perform(get("/payment/success")
                        .session(session)
                        .param("session_id", "sess_req_null"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Session expired. Contact support with payment confirmation."));
    }

    @Test
    void confirmWithOnlyReqNull() throws Exception {
        // Only pricing is set
        session.setAttribute("previewPricing", pricing);
        session.setAttribute("currentUser", user);

        mockMvc.perform(post("/payment/confirm")
                        .session(session)
                        .param("paymentMethod", "STRIPE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Session expired. Please re-enter ticket details."));
    }

    @Test
    void confirmWithOnlyPricingNull() throws Exception {
        // Only req is set
        session.setAttribute("previewReq", req);
        session.setAttribute("currentUser", user);

        mockMvc.perform(post("/payment/confirm")
                        .session(session)
                        .param("paymentMethod", "STRIPE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"))
                .andExpect(flash().attribute("error", "Session expired. Please re-enter ticket details."));
    }
}
