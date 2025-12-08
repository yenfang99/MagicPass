package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.MembershipTypeService;
import com.app.MagicPass.service.StripeService;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/membership")
public class MembershipController {

    private final MembershipService membershipService;
    private final MembershipTypeService membershipTypeService;
    private final StripeService stripeService;

    @Value("${server.port:8081}")
    private String serverPort;

    public MembershipController(MembershipService membershipService,
                               MembershipTypeService membershipTypeService,
                               StripeService stripeService) {
        this.membershipService = membershipService;
        this.membershipTypeService = membershipTypeService;
        this.stripeService = stripeService;
    }

    @GetMapping
    public String membershipPage(@RequestParam(value = "userId", defaultValue = "1") Long userId, Model model) {
        // Load dynamic membership types from database
        List<MembershipType> membershipTypes = membershipTypeService.getActiveMembershipTypes();
        model.addAttribute("membershipTypes", membershipTypes);

        // Get user's current active membership
        Membership currentMembership = membershipService.getActiveMembership(userId);
        model.addAttribute("currentMembership", currentMembership);

        return "membership";
    }

    @PostMapping("/purchase")
    public String purchaseMembership(
            @RequestParam("membershipTypeId") Long membershipTypeId,
            @RequestParam(value = "userId", defaultValue = "1") Long userId,
            HttpServletRequest request,
            RedirectAttributes ra) {

        try {
            // Get the membership type from database
            MembershipType membershipType = membershipTypeService.getMembershipTypeById(membershipTypeId);

            // Get base URL
            String baseUrl = "http://localhost:" + serverPort;

            // Create Stripe Checkout Session
            Session session = stripeService.createCheckoutSession(
                    membershipType.getDisplayName(),
                    membershipType.getPrice(),
                    userId,
                    baseUrl + "/membership/success?session_id={CHECKOUT_SESSION_ID}",
                    baseUrl + "/membership/cancel"
            );

            // Store membershipTypeId in Stripe metadata
            // Note: You may want to add this to the createCheckoutSession method

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
