
package com.app.MagicPass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.DatePolicyService;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.MembershipTypeService;
import com.app.MagicPass.service.OrderService;
import com.app.MagicPass.service.PricingService;
import com.app.MagicPass.service.StripeService;
import com.stripe.model.checkout.Session;

import jakarta.servlet.http.HttpSession;

@Controller
public class PaymentController {

  private final OrderService orderService;
  private final PricingService pricingService;
  private final DatePolicyService datePolicyService;
  private final StripeService stripeService;
  private final MembershipService membershipService;
  private final MembershipTypeService membershipTypeService;

  public PaymentController(OrderService orderService,
                           PricingService pricingService,
                           DatePolicyService datePolicyService,
                           StripeService stripeService,
                           MembershipService membershipService,
                           MembershipTypeService membershipTypeService) {
    this.orderService = orderService;
    this.pricingService = pricingService;
    this.datePolicyService = datePolicyService;
    this.stripeService = stripeService;
    this.membershipService = membershipService;
    this.membershipTypeService = membershipTypeService;
  }

  @GetMapping("/payment")
  public String payment(Model model, HttpSession session, RedirectAttributes ra) {
    CheckoutRequest req = (CheckoutRequest) session.getAttribute("previewReq");
    PricingBreakdown pricing = (PricingBreakdown) session.getAttribute("previewPricing");

    if (req == null || pricing == null) {
      ra.addFlashAttribute("error", "Please enter ticket details again.");
      return "redirect:/tickets";
    }

    User currentUser = (User) session.getAttribute("currentUser");
    if (currentUser == null) {
      ra.addFlashAttribute("error", "Please log in to continue with payment.");
      return "redirect:/login";
    }

    // Force user id from session (don't trust client payload)
    req.setUserId(currentUser.getId());

    // Build membership hint/CTA
    Membership activeMembership = membershipService.getActiveMembershipWithActiveType(currentUser.getId());
    MembershipType currentType = activeMembership != null ? activeMembership.getMembershipType() : null;
    int currentTierLevel = currentType != null && currentType.getTierLevel() != null ? currentType.getTierLevel() : 0;
    String currentMembershipName = currentType != null ? currentType.getDisplayName() : null;
    Double currentMembershipDiscountRate = currentType != null ? currentType.getDiscountRate() : null;

    var activeTypes = membershipTypeService.getActiveMembershipTypes();
    MembershipType topMembershipType = activeTypes.stream()
      .filter(t -> t.getTierLevel() != null)
      .max(java.util.Comparator.comparingInt(MembershipType::getTierLevel))
      .orElse(null);

    int highestTierLevel = topMembershipType != null && topMembershipType.getTierLevel() != null
      ? topMembershipType.getTierLevel()
      : 0;
    String highestTierName = topMembershipType != null
      ? topMembershipType.getDisplayName()
      : "top tier";
    Double topMembershipDiscountRate = topMembershipType != null ? topMembershipType.getDiscountRate() : null;

    boolean canUpgrade = currentTierLevel < highestTierLevel;
    String membershipHint;
    if (currentType == null) {
      membershipHint = "Want lower prices? Buy a membership to unlock ticket discounts before checkout.";
    } else if (canUpgrade) {
      membershipHint = "You're on " + currentType.getDisplayName() + ". Upgrade to a higher tier for bigger discounts.";
    } else {
      membershipHint = "You're on our top membership (" + currentType.getDisplayName() + ") and already getting the maximum discount.";
    }

    model.addAttribute("req", req);
    model.addAttribute("pricing", pricing);
    model.addAttribute("membershipHint", membershipHint);
    model.addAttribute("canUpgradeMembership", canUpgrade);
    model.addAttribute("topMembershipName", highestTierName);
    model.addAttribute("topMembershipDiscountRate", topMembershipDiscountRate);
    model.addAttribute("currentMembershipName", currentMembershipName);
    model.addAttribute("currentMembershipDiscountRate", currentMembershipDiscountRate);
    return "payment";
  }

  @PostMapping("/payment/confirm")
  public String confirm(@RequestParam("paymentMethod") String paymentMethod,
                        HttpSession session,
                        RedirectAttributes ra) {

    CheckoutRequest req = (CheckoutRequest) session.getAttribute("previewReq");
    PricingBreakdown pricing = (PricingBreakdown) session.getAttribute("previewPricing");

    if (req == null || pricing == null) {
      ra.addFlashAttribute("error", "Session expired. Please re-enter ticket details.");
      return "redirect:/tickets";
    }

    User currentUser = (User) session.getAttribute("currentUser");
    if (currentUser == null) {
      ra.addFlashAttribute("error", "Please log in to confirm your payment.");
      return "redirect:/login";
    }

    // Validate again (server-side safety)
    int totalQty = req.getAdultQty() + req.getStudentQty() + req.getChildQty();
    if (totalQty <= 0 || req.getReservationDate() == null || !datePolicyService.isWithin7Days(req.getReservationDate())) {
      session.removeAttribute("previewReq");
      session.removeAttribute("previewPricing");
      ra.addFlashAttribute("error", "Invalid ticket input. Please re-enter.");
      return "redirect:/tickets";
    }

    // Enforce purchaser identity from session
    req.setUserId(currentUser.getId());

    boolean isMember = membershipService.getActiveMembershipWithActiveType(currentUser.getId()) != null;
    // Recalculate pricing (don't trust session for money)
    pricing = pricingService.calculate(req, isMember);

    try {
      // Create Stripe checkout session
      String successUrl = "http://localhost:8081/payment/success?session_id={CHECKOUT_SESSION_ID}";
      String cancelUrl = "http://localhost:8081/payment/cancel";

      Session stripeSession = stripeService.createTicketCheckoutSession(
        pricing.getGrandTotal(),
        req,
        successUrl,
        cancelUrl
      );

      // Store session info for later verification
      session.setAttribute("stripeSessionId", stripeSession.getId());

      // Redirect to Stripe Checkout
      return "redirect:" + stripeSession.getUrl();

    } catch (Exception e) {
      ra.addFlashAttribute("error", "Payment processing failed: " + e.getMessage());
      return "redirect:/payment";
    }
  }

  @GetMapping("/payment/success")
  public String paymentSuccess(@RequestParam("session_id") String sessionId,
                               HttpSession session,
                               RedirectAttributes ra) {
    try {
      // Verify payment with Stripe
      Session stripeSession = stripeService.retrieveSession(sessionId);

      if (!"complete".equals(stripeSession.getStatus())) {
        ra.addFlashAttribute("error", "Payment was not completed.");
        return "redirect:/tickets";
      }

      // Retrieve booking details from session
      CheckoutRequest req = (CheckoutRequest) session.getAttribute("previewReq");
      PricingBreakdown pricing = (PricingBreakdown) session.getAttribute("previewPricing");

      if (req == null || pricing == null) {
        ra.addFlashAttribute("error", "Session expired. Contact support with payment confirmation.");
        return "redirect:/tickets";
      }

      // Get the actual payment method used
      String paymentMethod = "CARD"; // Default
      try {
        if (stripeSession.getPaymentIntent() != null) {
          com.stripe.model.PaymentIntent paymentIntent = com.stripe.model.PaymentIntent.retrieve(stripeSession.getPaymentIntent());
          if (paymentIntent.getPaymentMethod() != null) {
            com.stripe.model.PaymentMethod pm = com.stripe.model.PaymentMethod.retrieve(paymentIntent.getPaymentMethod());
            paymentMethod = pm.getType().toUpperCase();
          }
        }
      } catch (Exception e) {
        // If we can't retrieve payment method, use default
        paymentMethod = "CARD";
      }

      // Create the order after successful payment
      var order = orderService.createPaidOrder(req, pricing, paymentMethod);

      // Clear session
      session.removeAttribute("previewReq");
      session.removeAttribute("previewPricing");
      session.removeAttribute("stripeSessionId");

      return "redirect:/receipt/" + order.getId();

    } catch (Exception e) {
      ra.addFlashAttribute("error", "Payment verification failed: " + e.getMessage());
      return "redirect:/tickets";
    }
  }

  @GetMapping("/payment/cancel")
  public String paymentCancel(RedirectAttributes ra) {
    ra.addFlashAttribute("error", "Payment was cancelled.");
    return "redirect:/payment";
  }

  @GetMapping("/receipt/{orderId}")
  public String receipt(@PathVariable Long orderId, Model model) {
    var order = orderService.getById(orderId);
    model.addAttribute("order", order);
    return "receipt";
  }
}
