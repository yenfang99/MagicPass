package com.app.MagicPass.strategy;

import org.springframework.stereotype.Component;
import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;

@Component("memberPricingStrategy")
public class MemberPricingStrategy implements PricingStrategy {

    private static final double ADULT = 370.0, STUDENT = 320.0, CHILD = 250.0;
    private static final double TAX_RATE = 0.06;
    private static final double MEMBER_DISCOUNT_RATE = 0.10; // adjust to match your legacy

    @Override
    public PricingBreakdown calculate(CheckoutRequest req) {
        double subtotal = req.getAdultQty()*ADULT + req.getStudentQty()*STUDENT + req.getChildQty()*CHILD;
        double discount = subtotal * MEMBER_DISCOUNT_RATE;
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
