package com.app.MagicPass.strategy;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;

class NonMemberPricingStrategyTest {

    private final NonMemberPricingStrategy strategy = new NonMemberPricingStrategy();

    @Test
    void calculatesTotalsWithoutDiscount() {
        CheckoutRequest req = new CheckoutRequest();
        req.setAdultQty(1);
        req.setStudentQty(1);
        req.setChildQty(1);

        PricingBreakdown result = strategy.calculate(req);

        assertEquals(940.00, result.getTotal());
        assertEquals(0.00, result.getDiscount());
        assertEquals(56.4, result.getTax());
        assertEquals(996.4, result.getGrandTotal());
        assertEquals(0.0, result.getDiscountRate());
    }
}
