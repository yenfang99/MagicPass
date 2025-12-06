package com.app.MagicPass.service;

import org.springframework.stereotype.Service;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;

@Service
public class PricingService {

    private static final double ADULT_PRICE = 370.0;
    private static final double STUDENT_PRICE = 320.0;
    private static final double CHILD_PRICE = 250.0;
    private static final double TAX_RATE = 0.06;

    public PricingBreakdown calculate(CheckoutRequest req, double discountRate) {
        double total = req.getAdultQty() * ADULT_PRICE
                + req.getStudentQty() * STUDENT_PRICE
                + req.getChildQty() * CHILD_PRICE;

        double discount = total * discountRate;  // legacy: member discount
        double subtotal = total - discount;
        double tax = subtotal * TAX_RATE;
        double grandTotal = subtotal + tax;

        return new PricingBreakdown(total, discount, tax, grandTotal);
    }
}
