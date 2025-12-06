package com.app.MagicPass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.service.DatePolicyService;
import com.app.MagicPass.service.PricingService;

@Controller
public class TicketController {

    private final PricingService pricingService;
    private final DatePolicyService datePolicyService;

    public TicketController(PricingService pricingService, DatePolicyService datePolicyService) {
        this.pricingService = pricingService;
        this.datePolicyService = datePolicyService;
    }

    @GetMapping("/tickets")
    public String tickets(Model model) {
        model.addAttribute("req", new CheckoutRequest());
        return "tickets";
    }

    @PostMapping("/tickets/preview")
    public String preview(@ModelAttribute("req") CheckoutRequest req, Model model) {

        int totalQty = req.getAdultQty() + req.getStudentQty() + req.getChildQty();
        if (totalQty <= 0) {
            model.addAttribute("error", "Please select at least 1 ticket.");
            return "tickets";
        }

        // Validate date using legacy rule: format + within 7 days
        try {
            datePolicyService.parseAndValidateReservationDate(req.getReservationDate());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "tickets";
        }

        // For now membership discount rate = 0%
        PricingBreakdown pricing = pricingService.calculate(req, 0.0);

        model.addAttribute("pricing", pricing);
        return "payment";
    }
}
