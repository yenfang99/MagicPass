package com.app.MagicPass.strategy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.service.MembershipService;

@ExtendWith(MockitoExtension.class)
class MemberPricingStrategyTest {

    @Mock
    private MembershipService membershipService;

    @InjectMocks
    private MemberPricingStrategy strategy;

    private CheckoutRequest req;
    private Membership membership;
    private MembershipType type;

    @BeforeEach
    void setUp() {
        req = new CheckoutRequest();
        req.setUserId(7L);
        req.setAdultQty(1);
        req.setStudentQty(1);
        req.setChildQty(1);

        type = new MembershipType();
        type.setDiscountRate(0.10);
        type.setDisplayName("Gold");
        type.setTierLevel(2);

        membership = new Membership();
        membership.setMembershipType(type);
        membership.setDiscountRate(0.10);
    }

    @Test
    void usesMembershipDiscountWhenAvailable() {
        when(membershipService.getActiveMembershipWithActiveType(7L)).thenReturn(membership);

        PricingBreakdown result = strategy.calculate(req);

        assertEquals(940.00, result.getTotal());
        assertEquals(94.00, result.getDiscount());
        assertEquals(50.76, result.getTax());
        assertEquals(896.76, result.getGrandTotal());
        assertEquals(0.10, result.getDiscountRate());
    }

    @Test
    void fallsBackToNoDiscountWhenNoMembership() {
        when(membershipService.getActiveMembershipWithActiveType(7L)).thenReturn(null);

        PricingBreakdown result = strategy.calculate(req);

        assertEquals(0.0, result.getDiscount());
        assertEquals(0.0, result.getDiscountRate());
    }

    @Test
    void usesMembershipDiscountWhenTypeMissing() {
        membership.setMembershipType(null);
        membership.setDiscountRate(0.15);
        when(membershipService.getActiveMembershipWithActiveType(7L)).thenReturn(membership);

        PricingBreakdown result = strategy.calculate(req);

        assertEquals(940.00, result.getTotal());
        assertEquals(141.00, result.getDiscount());
        assertEquals(47.94, result.getTax());
        assertEquals(846.94, result.getGrandTotal());
        assertEquals(0.15, result.getDiscountRate());
    }

    @Test
    void userIdNullSkipsLookupAndAppliesNoDiscount() {
        req.setUserId(null);

        PricingBreakdown result = strategy.calculate(req);

        verifyNoInteractions(membershipService);
        assertEquals(940.00, result.getTotal());
        assertEquals(0.00, result.getDiscount());
        assertEquals(56.40, result.getTax());
        assertEquals(996.40, result.getGrandTotal());
        assertEquals(0.0, result.getDiscountRate());
    }
}
