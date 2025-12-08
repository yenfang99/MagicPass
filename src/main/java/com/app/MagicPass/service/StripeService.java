package com.app.MagicPass.service;

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
            String tierName,
            Double price,
            Long userId,
            String successUrl,
            String cancelUrl) throws StripeException {

        // Convert price to cents (Stripe uses smallest currency unit)
        long priceInCents = (long) (price * 100);

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
                                                                .setName("MagicPass " + tierName + " Membership")
                                                                .setDescription("1 year membership with exclusive benefits")
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
                .putMetadata("tier", tierName)
                .putMetadata("membershipType", "ANNUAL")
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
