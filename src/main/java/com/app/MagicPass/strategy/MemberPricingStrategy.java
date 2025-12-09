package com.app.MagicPass.strategy;

import org.springframework.stereotype.Component;
import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
<<<<<<< Updated upstream
=======
import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.MembershipTypeService;
>>>>>>> Stashed changes

@Component("memberPricingStrategy")
public class MemberPricingStrategy implements PricingStrategy {

    private static final double ADULT = 370.0, STUDENT = 320.0, CHILD = 250.0;
    private static final double TAX_RATE = 0.06;
<<<<<<< Updated upstream
    private static final double MEMBER_DISCOUNT_RATE = 0.10; // adjust to match your legacy
=======

    private final MembershipService membershipService;
    private final MembershipTypeService membershipTypeService;

    public MemberPricingStrategy(MembershipService membershipService,
                                 MembershipTypeService membershipTypeService) {
        this.membershipService = membershipService;
        this.membershipTypeService = membershipTypeService;
    }
>>>>>>> Stashed changes

    @Override
    public PricingBreakdown calculate(CheckoutRequest req) {
        double subtotal = req.getAdultQty()*ADULT + req.getStudentQty()*STUDENT + req.getChildQty()*CHILD;
<<<<<<< Updated upstream
        double discount = subtotal * MEMBER_DISCOUNT_RATE;
=======

        // Default: no discount until we confirm an active membership and active type
        double discountRate = 0.0;

        // Try to get actual membership discount from database
        // Note: This assumes userId is set in CheckoutRequest
        // If not available, you'll need to pass it differently
        if (req.getUserId() != null) {
            Membership activeMembership = membershipService.getActiveMembership(req.getUserId());
            if (activeMembership != null) {
                discountRate = activeMembership.getDiscountRate(); // fallback to stored rate

                // Resolve type by name (tier) and ensure the type is active
                if (activeMembership.getTier() != null) {
                    try {
                        MembershipType type = membershipTypeService.getMembershipTypeByName(activeMembership.getTier());
                        if (Boolean.TRUE.equals(type.getActive())) {
                            discountRate = type.getDiscountRate();
                        } else {
                            discountRate = 0.0; // type inactive => no discount
                        }
                    } catch (RuntimeException ignored) {
                        // Missing type; keep stored discount rate
                    }
                }
            }
        }

        double discount = subtotal * discountRate;
>>>>>>> Stashed changes
        double tax = (subtotal - discount) * TAX_RATE;
        double grand = (subtotal - discount) + tax;

        PricingBreakdown pb = new PricingBreakdown();
        pb.setTotal(round2(subtotal));
        pb.setDiscount(round2(discount));
        pb.setTax(round2(tax));
        pb.setGrandTotal(round2(grand));
        pb.setDiscountRate(round2(discountRate * 100.0) / 100.0); // store as fraction (not percent)
        return pb;
    }

    private double round2(double v){ return Math.round(v * 100.0) / 100.0; }
}
