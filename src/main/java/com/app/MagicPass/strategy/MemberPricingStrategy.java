package com.app.MagicPass.strategy;

import org.springframework.stereotype.Component;
import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.model.Membership;
import com.app.MagicPass.service.MembershipService;

@Component("memberPricingStrategy")
public class MemberPricingStrategy implements PricingStrategy {

    private static final double ADULT = 370.0, STUDENT = 320.0, CHILD = 250.0;
    private static final double TAX_RATE = 0.06;

    private final MembershipService membershipService;

    public MemberPricingStrategy(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Override
    public PricingBreakdown calculate(CheckoutRequest req) {
        double subtotal = req.getAdultQty()*ADULT + req.getStudentQty()*STUDENT + req.getChildQty()*CHILD;

        // Get user's membership discount rate (default 0.10 if membership not found)
        double discountRate = 0.10; // Default 10%

        // Try to get actual membership discount from database
        // Note: This assumes userId is set in CheckoutRequest
        // If not available, you'll need to pass it differently
        if (req.getUserId() != null) {
            Membership activeMembership = membershipService.getActiveMembership(req.getUserId());
            if (activeMembership != null) {
                discountRate = activeMembership.getDiscountRate();
            }
        }

        double discount = subtotal * discountRate;
        double tax = (subtotal - discount) * TAX_RATE;
        double grand = (subtotal - discount) + tax;

        PricingBreakdown pb = new PricingBreakdown();
        pb.setTotal(round2(subtotal));
        pb.setDiscount(round2(discount));
        pb.setTax(round2(tax));
        pb.setGrandTotal(round2(grand));
        return pb;
    }

    private double round2(double v){ return Math.round(v * 100.0) / 100.0; }
}
