package com.app.MagicPass.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.strategy.PricingStrategy;
import com.app.MagicPass.strategy.PricingStrategyFactory;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PricingStrategyFactory factory;

    @Mock
    private PricingStrategy strategy;

    @InjectMocks
    private PricingService pricingService;

    @Test
    void calculateDelegatesToStrategy() {
        CheckoutRequest req = new CheckoutRequest();
        PricingBreakdown expected = new PricingBreakdown();
        when(factory.getStrategy(true)).thenReturn(strategy);
        when(strategy.calculate(req)).thenReturn(expected);

        PricingBreakdown result = pricingService.calculate(req, true);

        assertSame(expected, result);
        verify(factory).getStrategy(true);
        verify(strategy).calculate(req);
    }
}
