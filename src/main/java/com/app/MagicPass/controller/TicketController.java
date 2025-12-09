package com.app.MagicPass.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.DatePolicyService;
import com.app.MagicPass.service.PricingService;

import jakarta.servlet.http.HttpSession;

@Controller
public class TicketController {

    private final PricingService pricingService;
    private final DatePolicyService datePolicyService;

    public TicketController(PricingService pricingService, DatePolicyService datePolicyService) {
        this.pricingService = pricingService;
        this.datePolicyService = datePolicyService;
    }

    @GetMapping("/tickets")
    public String tickets(HttpSession session, Model model) {
        // Get current user from session
        User currentUser = (User) session.getAttribute("currentUser");

        CheckoutRequest req = new CheckoutRequest();
        req.setReservationDate(LocalDate.now());

        // Set userId from session if logged in
        if (currentUser != null) {
            req.setUserId(currentUser.getId());  // Set userId for membership discount lookup
        }

        model.addAttribute("req", req);
        model.addAttribute("currentUser", currentUser);
        return "tickets";
    }

    @PostMapping("/tickets/preview")
    public String preview(@ModelAttribute("req") CheckoutRequest req,
                          HttpSession session,
                          RedirectAttributes ra) {

        int totalQty = req.getAdultQty() + req.getStudentQty() + req.getChildQty();
        if (totalQty <= 0) {
            ra.addFlashAttribute("error", "Please select at least 1 ticket.");
            return "redirect:/tickets";
        }

        if (req.getReservationDate() == null) {
            ra.addFlashAttribute("error", "Please select a reservation date.");
            return "redirect:/tickets";
        }

        if (!datePolicyService.isWithin7Days(req.getReservationDate())) {
            ra.addFlashAttribute("error", "Invalid reservation date. Please choose a date within 7 days from today.");
            return "redirect:/tickets";
        }

        PricingBreakdown pricing = pricingService.calculate(req, false);

        // store in session
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);

        // PRG redirect (so refresh doesn't re-POST)
        return "redirect:/payment";
    }
}
