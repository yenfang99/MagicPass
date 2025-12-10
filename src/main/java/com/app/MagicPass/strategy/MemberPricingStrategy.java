package com.app.MagicPass.strategy;

import org.springframework.stereotype.Component;
import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
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

        // Default: no discount until we confirm an active membership and active type
        double discountRate = 0.0;

        // Try to get actual membership discount from database
        if (req.getUserId() != null) {
            Membership activeMembership = membershipService.getActiveMembershipWithActiveType(req.getUserId());
            if (activeMembership != null) {
                MembershipType type = activeMembership.getMembershipType();
                discountRate = type != null ? type.getDiscountRate() : activeMembership.getDiscountRate();
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
        pb.setDiscountRate(round2(discountRate * 100.0) / 100.0); // store as fraction (not percent)
        return pb;
    }

    private double round2(double v){ return Math.round(v * 100.0) / 100.0; }
}
