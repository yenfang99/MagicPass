package com.app.MagicPass.strategy;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;

public interface PricingStrategy {
    PricingBreakdown calculate(CheckoutRequest req);
}
