
package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.model.User;
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
import jakarta.servlet.http.HttpSession;
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
    public String membershipPage(HttpSession session, Model model) {
        // Get current user from session
        User currentUser = (User) session.getAttribute("currentUser");

        if (currentUser == null) {
            return "redirect:/login";  // Redirect to login if not authenticated
        }

        Long userId = currentUser.getId();

        // Load dynamic membership types from database
        List<MembershipType> membershipTypes = membershipTypeService.getActiveMembershipTypes();
        model.addAttribute("membershipTypes", membershipTypes);

        // Get user's current active membership
        Membership currentMembership = membershipService.getActiveMembership(userId);
        model.addAttribute("currentMembership", currentMembership);
        model.addAttribute("currentUser", currentUser);

        // Add current tier level for comparison in template
        if (currentMembership != null) {
            model.addAttribute("currentTierLevel", currentMembership.getMembershipType().getTierLevel());
        } else {
            model.addAttribute("currentTierLevel", 0); // No membership = can purchase any tier
        }

        return "membership";
    }

    @PostMapping("/purchase")
    public String purchaseMembership(
            @RequestParam("membershipTypeId") Long membershipTypeId,
            HttpSession httpSession,
            HttpServletRequest request,
            RedirectAttributes ra) {

        try {
            // Get current user from session
            User currentUser = (User) httpSession.getAttribute("currentUser");

            if (currentUser == null) {
                ra.addFlashAttribute("error", "Please login to purchase a membership.");
                return "redirect:/login";
            }

            Long userId = currentUser.getId();

            // Get the membership type from database
            MembershipType membershipType = membershipTypeService.getMembershipTypeById(membershipTypeId);

            // Get base URL
            String baseUrl = "http://localhost:" + serverPort;

            // Create Stripe Checkout Session
            Session session = stripeService.createCheckoutSession(
                    membershipType.getDisplayName(),
                    membershipType.getPrice(),
                    userId,
                    membershipType.getId(),
                    membershipType.getDurationMonths(),
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
                String membershipTypeIdStr = session.getMetadata().get("membershipTypeId");

                // Get membership type from database
                MembershipType membershipType = membershipTypeService.getMembershipTypeById(Long.parseLong(membershipTypeIdStr));

                // Create membership
                Membership membership = membershipService.purchaseMembership(Long.parseLong(userId), membershipType);

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
    public String confirmation(@RequestParam("id") String membershipId, Model model, RedirectAttributes ra) {
        try {
            // Fetch the membership details from database
            Membership membership = membershipService.getMembershipByMembershipId(membershipId);

            if (membership == null) {
                ra.addFlashAttribute("error", "Membership not found.");
                return "redirect:/membership";
            }

            MembershipType membershipType = membership.getMembershipType();

            // Add all membership details to model
            model.addAttribute("membershipId", membership.getMembershipId());
            model.addAttribute("tier", membershipType.getDisplayName());
            model.addAttribute("startDate", membership.getStartDate());
            model.addAttribute("expiryDate", membership.getExpiryDate());
            model.addAttribute("discountRate", membership.getDiscountRate() * 100); // Convert to percentage
            model.addAttribute("price", membership.getPrice());

            // Get benefits from membership type description
            String description = membershipType.getDescription() != null ? membershipType.getDescription() : "";

            // Split description into lines for display
            java.util.List<String> benefitsList = new java.util.ArrayList<>();
            if (description != null && !description.isEmpty()) {
                // Try splitting by newlines first
                String[] lines = description.split("\\r?\\n");
                if (lines.length > 1) {
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) {
                            benefitsList.add(trimmed);
                        }
                    }
                } else {
                    // No newlines - try splitting by periods
                    String[] sentences = description.split("\\.");
                    for (String sentence : sentences) {
                        String trimmed = sentence.trim();
                        if (!trimmed.isEmpty()) {
                            benefitsList.add(trimmed);
                        }
                    }
                }
            }

            model.addAttribute("benefitsList", benefitsList);

            return "membership-confirmation";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error loading membership details: " + e.getMessage());
            return "redirect:/membership";
        }
    }
}
