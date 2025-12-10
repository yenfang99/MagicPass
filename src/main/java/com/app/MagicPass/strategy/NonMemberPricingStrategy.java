package com.app.MagicPass.strategy;

import org.springframework.stereotype.Component;
import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;

@Component("nonMemberPricingStrategy")
public class NonMemberPricingStrategy implements PricingStrategy {

    private static final double ADULT = 370.0, STUDENT = 320.0, CHILD = 250.0;
    private static final double TAX_RATE = 0.06;

    @Override
    public PricingBreakdown calculate(CheckoutRequest req) {
        double subtotal = req.getAdultQty()*ADULT + req.getStudentQty()*STUDENT + req.getChildQty()*CHILD;
        double discount = 0.0;
        double tax = (subtotal - discount) * TAX_RATE;
        double grand = (subtotal - discount) + tax;

        PricingBreakdown pb = new PricingBreakdown();
        pb.setTotal(round2(subtotal));
        pb.setDiscount(round2(discount));
        pb.setTax(round2(tax));
        pb.setGrandTotal(round2(grand));
        pb.setDiscountRate(0.0);
        return pb;
    }

    private double round2(double v){ return Math.round(v * 100.0) / 100.0; }
}
