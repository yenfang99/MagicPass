package com.app.MagicPass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.service.DatePolicyService;
import com.app.MagicPass.service.OrderService;
import com.app.MagicPass.service.PricingService;

import jakarta.servlet.http.HttpSession;

@Controller
public class PaymentController {

  private final OrderService orderService;
  private final PricingService pricingService;
  private final DatePolicyService datePolicyService;

  public PaymentController(OrderService orderService,
                           PricingService pricingService,
                           DatePolicyService datePolicyService) {
    this.orderService = orderService;
    this.pricingService = pricingService;
    this.datePolicyService = datePolicyService;
  }

  @GetMapping("/payment")
  public String payment(Model model, HttpSession session, RedirectAttributes ra) {
    CheckoutRequest req = (CheckoutRequest) session.getAttribute("previewReq");
    PricingBreakdown pricing = (PricingBreakdown) session.getAttribute("previewPricing");

    if (req == null || pricing == null) {
      ra.addFlashAttribute("error", "Please enter ticket details again.");
      return "redirect:/tickets";
    }

    model.addAttribute("req", req);
    model.addAttribute("pricing", pricing);
    return "payment";
  }

  @PostMapping("/payment/confirm")
  public String confirm(@RequestParam("paymentMethod") String paymentMethod,
                        HttpSession session,
                        RedirectAttributes ra) {

    CheckoutRequest req = (CheckoutRequest) session.getAttribute("previewReq");
    if (req == null) {
      ra.addFlashAttribute("error", "Session expired. Please re-enter ticket details.");
      return "redirect:/tickets";
    }

    // Validate again (server-side safety)
    int totalQty = req.getAdultQty() + req.getStudentQty() + req.getChildQty();
    if (totalQty <= 0 || req.getReservationDate() == null || !datePolicyService.isWithin7Days(req.getReservationDate())) {
      session.removeAttribute("previewReq");
      session.removeAttribute("previewPricing");
      ra.addFlashAttribute("error", "Invalid ticket input. Please re-enter.");
      return "redirect:/tickets";
    }

    // Recalculate pricing (don’t trust session for money)
    PricingBreakdown pricing = pricingService.calculate(req, false);

    // Save order only after payment
    var order = orderService.createPaidOrder(req, pricing, paymentMethod);

    // Clear preview session
    session.removeAttribute("previewReq");
    session.removeAttribute("previewPricing");

    return "redirect:/receipt/" + order.getId();
  }

  @GetMapping("/receipt/{orderId}")
  public String receipt(@PathVariable Long orderId, Model model) {
    var order = orderService.getById(orderId);
    model.addAttribute("order", order);
    return "receipt";
  }
}
