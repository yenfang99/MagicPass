package com.app.MagicPass.service;

import org.springframework.stereotype.Service;
import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.strategy.PricingStrategyFactory;

@Service
public class PricingService {

    private final PricingStrategyFactory factory;

    public PricingService(PricingStrategyFactory factory) {
        this.factory = factory;
    }

    public PricingBreakdown calculate(CheckoutRequest req, boolean isMember) {
        return factory.getStrategy(isMember).calculate(req);
    }
}
