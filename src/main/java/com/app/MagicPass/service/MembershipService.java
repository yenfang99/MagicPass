package com.app.MagicPass.service;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.repository.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;

    public MembershipService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public Membership purchaseMembership(Long userId, Membership.MembershipTier tier) {
        // Check if user already has an active membership
        Membership existingMembership = getActiveMembership(userId);

        if (existingMembership != null) {
            // Check if trying to purchase same or lower tier
            if (tier.ordinal() <= existingMembership.getTier().ordinal()) {
                throw new IllegalStateException(
                    "You already have an active " + existingMembership.getTier().getDisplayName() +
                    " membership. You can only upgrade to a higher tier."
                );
            }

            // Upgrade: expire the old membership
            existingMembership.setStatus(Membership.MembershipStatus.EXPIRED);
            membershipRepository.save(existingMembership);
        }

        // Create new membership for the user ID from your existing users table
        Membership membership = new Membership();
        membership.setMembershipId("MEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        membership.setUserId(userId);  // This references users.id
        membership.setTier(tier);
        membership.setPrice(tier.getPrice());
        membership.setDiscountRate(tier.getDiscountRate());
        membership.setStartDate(LocalDateTime.now());
        membership.setExpiryDate(LocalDateTime.now().plusYears(1)); // 1 year validity
        membership.setStatus(Membership.MembershipStatus.ACTIVE);

        return membershipRepository.save(membership);
    }

    /**
     * Check if user can purchase or upgrade to a specific tier
     */
    public String canPurchaseTier(Long userId, Membership.MembershipTier tier) {
        Membership existingMembership = getActiveMembership(userId);

        if (existingMembership == null) {
            return null; // Can purchase any tier
        }

        // Check if trying to purchase same or lower tier
        if (tier.ordinal() <= existingMembership.getTier().ordinal()) {
            return "You already have an active " + existingMembership.getTier().getDisplayName() +
                   " membership. You can only upgrade to a higher tier.";
        }

        return null; // Can upgrade
    }

    public Membership getActiveMembership(Long userId) {
        return membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE)
                .orElse(null);
    }

    public List<Membership> getUserMemberships(Long userId) {
        return membershipRepository.findByUserId(userId);
    }

    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    public Membership.MembershipTier[] getAllTiers() {
        return Membership.MembershipTier.values();
    }
}
