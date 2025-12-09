package com.app.MagicPass.service;

import com.app.MagicPass.dto.CheckoutRequest;
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
     * Create a Stripe Checkout Session for membership purchase.
     */
    public Session createCheckoutSession(
            MembershipType membershipType,
            Long userId,
            String successUrl,
            String cancelUrl) throws StripeException {

        long priceInCents = (long) (membershipType.getPrice() * 100);
        String durationDescription = membershipType.getDurationMonths() == 12
                ? "1 year membership with exclusive benefits"
                : membershipType.getDurationMonths() + " months membership with exclusive benefits";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("myr")
                                                .setUnitAmount(priceInCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("MagicPass " + membershipType.getDisplayName() + " Membership")
                                                                .setDescription(durationDescription)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.FPX)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.GRABPAY)
                .putMetadata("userId", userId.toString())
                .putMetadata("membershipTypeId", membershipType.getId().toString())
                .build();

        return Session.create(params);
    }

    /**
     * Create a Stripe Checkout Session for ticket purchase.
     */
    public Session createTicketCheckoutSession(
            Double grandTotal,
            CheckoutRequest req,
            String successUrl,
            String cancelUrl) throws StripeException {

        long priceInCents = (long) (grandTotal * 100);

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
                                                .setCurrency("myr")
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
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.FPX)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.GRABPAY)
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
     * Retrieve a checkout session by ID.
     */
    public Session retrieveSession(String sessionId) throws StripeException {
        return Session.retrieve(sessionId);
    }
}
