package com.app.MagicPass.service;

import com.app.MagicPass.model.MembershipType;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {

    @Value("${stripe.publishable.key}")
    private String publishableKey;

    public String getPublishableKey() {
        return publishableKey;
    }

    /**
     * Create a Stripe Checkout Session for membership purchase
     */
    public Session createCheckoutSession(
            MembershipType membershipType,
            Long userId,
            String successUrl,
            String cancelUrl) throws StripeException {

        // Convert price to cents (Stripe uses smallest currency unit)
        long priceInCents = (long) (membershipType.getPrice() * 100);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("myr")  // Malaysian Ringgit
                                                .setUnitAmount(priceInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("MagicPass " + membershipType.getDisplayName() + " Membership")
                                                                .setDescription(membershipType.getDurationMonths() + " month membership with exclusive benefits")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                // Enable multiple payment methods
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)  // Credit/Debit cards
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.FPX)  // Malaysian online banking
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.GRABPAY)  // GrabPay e-wallet
                // Store metadata to retrieve later
                .putMetadata("userId", userId.toString())
                .putMetadata("membershipTypeId", membershipType.getId().toString())
                .build();

        return Session.create(params);
    }

    /**
     * Create a Stripe Checkout Session for ticket purchase
     */
    public Session createTicketCheckoutSession(
            Double grandTotal,
            com.app.MagicPass.dto.CheckoutRequest req,
            String successUrl,
            String cancelUrl) throws StripeException {

        // Convert price to cents (Stripe uses smallest currency unit)
        long priceInCents = (long) (grandTotal * 100);

        // Build ticket description
        String description = String.format("Adult: %d, Student: %d, Children: %d - Date: %s",
                req.getAdultQty(), req.getStudentQty(), req.getChildQty(),
                req.getReservationDate().toString());

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("myr")  // Malaysian Ringgit
                                                .setUnitAmount(priceInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("MagicPass Tickets")
                                                                .setDescription(description)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                // Enable multiple payment methods
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.FPX)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.GRABPAY)
                // Store metadata
                .putMetadata("type", "TICKET_PURCHASE")
                .putMetadata("userId", req.getUserId() != null ? req.getUserId().toString() : "anonymous")
                .putMetadata("adultQty", String.valueOf(req.getAdultQty()))
                .putMetadata("studentQty", String.valueOf(req.getStudentQty()))
                .putMetadata("childQty", String.valueOf(req.getChildQty()))
                .putMetadata("reservationDate", req.getReservationDate().toString())
                .build();

        return Session.create(params);
    }

    /**
     * Retrieve a checkout session by ID
     */
    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}
