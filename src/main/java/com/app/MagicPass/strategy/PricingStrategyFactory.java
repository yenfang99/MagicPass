package com.app.MagicPass.strategy;

import org.springframework.stereotype.Component;

@Component
public class PricingStrategyFactory {

    private final PricingStrategy nonMemberPricingStrategy;
    private final PricingStrategy memberPricingStrategy;

    public PricingStrategyFactory(PricingStrategy nonMemberPricingStrategy,
                                 PricingStrategy memberPricingStrategy) {
        this.nonMemberPricingStrategy = nonMemberPricingStrategy;
        this.memberPricingStrategy = memberPricingStrategy;
    }

    public PricingStrategy getStrategy(boolean isMember) {
        return isMember ? memberPricingStrategy : nonMemberPricingStrategy;
    }
}
