package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.StripeService;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/membership")
public class MembershipController {

    private final MembershipService membershipService;
    private final StripeService stripeService;

    @Value("${server.port:8081}")
    private String serverPort;

    public MembershipController(MembershipService membershipService, StripeService stripeService) {
        this.membershipService = membershipService;
        this.stripeService = stripeService;
    }

    @GetMapping
    public String membershipPage(Model model) {
        Membership.MembershipTier[] tiers = membershipService.getAllTiers();
        model.addAttribute("tiers", tiers);
        return "membership";
    }

    @PostMapping("/purchase")
    public String purchaseMembership(
            @RequestParam("tier") String tierName,
            @RequestParam(value = "userId", defaultValue = "1") Long userId,
            HttpServletRequest request,
            RedirectAttributes ra) {

        try {
            Membership.MembershipTier tier = Membership.MembershipTier.valueOf(tierName.toUpperCase());

            // Get base URL
            String baseUrl = "http://localhost:" + serverPort;

            // Create Stripe Checkout Session
            Session session = stripeService.createCheckoutSession(
                    tier.getDisplayName(),
                    tier.getPrice(),
                    userId,
                    baseUrl + "/membership/success?session_id={CHECKOUT_SESSION_ID}",
                    baseUrl + "/membership/cancel"
            );

            // Redirect to Stripe Checkout
            return "redirect:" + session.getUrl();

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to initiate payment: " + e.getMessage());
            return "redirect:/membership";
        }
    }

    @GetMapping("/success")
    public String paymentSuccess(
            @RequestParam("session_id") String sessionId,
            RedirectAttributes ra) {

        try {
            // Retrieve session from Stripe
            Session session = stripeService.retrieveSession(sessionId);

            // Check if payment was successful
            if ("paid".equals(session.getPaymentStatus())) {
                // Get metadata
                String userId = session.getMetadata().get("userId");
                String tierName = session.getMetadata().get("tier");

                // Create membership
                Membership.MembershipTier tier = Membership.MembershipTier.valueOf(tierName.toUpperCase());
                Membership membership = membershipService.purchaseMembership(Long.parseLong(userId), tier);

                ra.addFlashAttribute("success", "Payment successful!");
                return "redirect:/membership/confirmation?id=" + membership.getMembershipId();
            } else {
                ra.addFlashAttribute("error", "Payment not completed. Please try again.");
                return "redirect:/membership";
            }

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error processing payment: " + e.getMessage());
            return "redirect:/membership";
        }
    }

    @GetMapping("/cancel")
    public String paymentCancelled(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Payment was cancelled. Please try again if you wish to purchase a membership.");
        return "redirect:/membership";
    }

    @GetMapping("/confirmation")
    public String confirmation(@RequestParam("id") String membershipId, Model model) {
        // In a real app, you'd fetch the membership details here
        model.addAttribute("membershipId", membershipId);
        return "membership-confirmation";
    }
}
