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
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.PricingService;

import jakarta.servlet.http.HttpSession;

@Controller
public class TicketController {

    private final PricingService pricingService;
    private final DatePolicyService datePolicyService;
    private final MembershipService membershipService;

    public TicketController(PricingService pricingService,
                            DatePolicyService datePolicyService,
                            MembershipService membershipService) {
        this.pricingService = pricingService;
        this.datePolicyService = datePolicyService;
        this.membershipService = membershipService;
    }

    @GetMapping("/tickets")
    public String tickets(@RequestParam(value = "fromPreview", required = false) Boolean fromPreview,
                          HttpSession session,
                          Model model,
                          RedirectAttributes ra) {
        // Get current user from session
        User currentUser = (User) session.getAttribute("currentUser");

        if (currentUser == null) {
            ra.addFlashAttribute("error", "Please log in to purchase tickets.");
            return "redirect:/login";
        }

        boolean keepExisting = Boolean.TRUE.equals(fromPreview);
        if (!keepExisting) {
            session.removeAttribute("previewReq");
            session.removeAttribute("previewPricing");
        }

        CheckoutRequest req = keepExisting ? (CheckoutRequest) session.getAttribute("previewReq") : null;
        if (req == null) {
            req = new CheckoutRequest();
            req.setReservationDate(LocalDate.now().plusDays(1)); // default to earliest allowed date (tomorrow)
        } else if (req.getReservationDate() == null) {
            req.setReservationDate(LocalDate.now().plusDays(1));
        }

        // Set/refresh userId from session if logged in
        req.setUserId(currentUser.getId());  // Set userId for membership discount lookup

        model.addAttribute("req", req);
        model.addAttribute("currentUser", currentUser);
        return "tickets";
    }

    @PostMapping("/tickets/preview")
    public String preview(@ModelAttribute("req") CheckoutRequest req,
                          HttpSession session,
                          RedirectAttributes ra) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            ra.addFlashAttribute("error", "Please log in to purchase tickets.");
            return "redirect:/login";
        }

        int totalQty = req.getAdultQty() + req.getStudentQty() + req.getChildQty();
        if (totalQty <= 0) {
            session.removeAttribute("previewReq");
            session.removeAttribute("previewPricing");
            ra.addFlashAttribute("error", "Please select at least 1 ticket.");
            return "redirect:/tickets";
        }

        if (req.getReservationDate() == null) {
            session.removeAttribute("previewReq");
            session.removeAttribute("previewPricing");
            ra.addFlashAttribute("error", "Please select a reservation date.");
            return "redirect:/tickets";
        }

        if (!datePolicyService.isWithin7Days(req.getReservationDate())) {
            session.removeAttribute("previewReq");
            session.removeAttribute("previewPricing");
            ra.addFlashAttribute("error", "Invalid reservation date. Please choose a date from tomorrow within the next 7 days.");
            return "redirect:/tickets";
        }

        // Enforce the purchaser's user id from session
        req.setUserId(currentUser.getId());

        boolean isMember = membershipService.getActiveMembershipWithActiveType(currentUser.getId()) != null;
        PricingBreakdown pricing = pricingService.calculate(req, isMember);

        // store in session
        session.setAttribute("previewReq", req);
        session.setAttribute("previewPricing", pricing);

        // PRG redirect (so refresh doesn't re-POST)
        return "redirect:/payment";
    }
}
