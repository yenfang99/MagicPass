package com.app.MagicPass.strategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

class PricingStrategyFactoryTest {

    @Test
    void returnsMemberOrNonMemberStrategy() {
        PricingStrategy nonMember = mock(PricingStrategy.class);
        PricingStrategy member = mock(PricingStrategy.class);

        PricingStrategyFactory factory = new PricingStrategyFactory(nonMember, member);

        assertSame(member, factory.getStrategy(true));
        assertSame(nonMember, factory.getStrategy(false));
    }
}
