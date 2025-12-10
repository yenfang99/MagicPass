package com.app.MagicPass.service;

import com.app.MagicPass.builder.MembershipBuilder;
import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.repository.MembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final MembershipTypeService membershipTypeService;

    public MembershipService(MembershipRepository membershipRepository,
                             MembershipTypeService membershipTypeService) {
        this.membershipRepository = membershipRepository;
        this.membershipTypeService = membershipTypeService;
    }

    @Transactional
    public Membership purchaseMembership(Long userId, MembershipType membershipType) {
        if (membershipType == null || !Boolean.TRUE.equals(membershipType.getActive())) {
            throw new IllegalArgumentException("Membership type is inactive or missing");
        }

        // Check if user already has an active membership
        Membership existingMembership = getActiveMembership(userId);
        if (existingMembership != null) {
            int currentTierLevel = existingMembership.getMembershipType().getTierLevel();
            int newTierLevel = membershipType.getTierLevel();

            // Prevent buying same or lower tier
            if (newTierLevel <= currentTierLevel) {
                throw new IllegalStateException(
                        "You already have an active " + existingMembership.getMembershipType().getDisplayName() +
                        " membership. You can only upgrade to a higher tier."
                );
            }

            // Upgrade: expire the old membership
            existingMembership.setStatus(Membership.MembershipStatus.EXPIRED);
            membershipRepository.save(existingMembership);
        }

        // Create new membership using Builder Pattern
        LocalDateTime now = LocalDateTime.now();
        Membership membership = new MembershipBuilder()
                .membershipId("MEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId(userId)
                .membershipType(membershipType)
                .price(membershipType.getPrice())
                .discountRate(membershipType.getDiscountRate())
                .startDate(now)
                .expiryDate(now.plusMonths(membershipType.getDurationMonths()))
                .status(Membership.MembershipStatus.ACTIVE)
                .build();

        return membershipRepository.save(membership);
    }

    /**
     * Check if user can purchase or upgrade to a specific membership type
     */
    public String canPurchaseMembershipType(Long userId, MembershipType membershipType) {
        Membership existingMembership = getActiveMembership(userId);

        if (existingMembership == null) {
            return null; // Can purchase any tier
        }

        int currentTierLevel = existingMembership.getMembershipType().getTierLevel();
        int newTierLevel = membershipType.getTierLevel();

        if (newTierLevel <= currentTierLevel) {
            return "You already have an active " + existingMembership.getMembershipType().getDisplayName() +
                   " membership. You can only upgrade to a higher tier.";
        }

        return null; // Can upgrade
    }

    public Membership getActiveMembership(Long userId) {
        return membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE)
                .orElse(null);
    }

    /**
     * Returns an active membership only if its membership type is still active.
     */
    public Membership getActiveMembershipWithActiveType(Long userId) {
        Membership membership = getActiveMembership(userId);
        if (membership == null) {
            return null;
        }

        MembershipType type = membership.getMembershipType();
        if (type != null) {
            MembershipType freshType = membershipTypeService.getMembershipTypeById(type.getId());
            if (Boolean.TRUE.equals(freshType.getActive())) {
                return membership;
            }
        }
        return null;
    }

    public List<Membership> getUserMemberships(Long userId) {
        return membershipRepository.findByUserId(userId);
    }

    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    public Membership getMembershipByMembershipId(String membershipId) {
        return membershipRepository.findByMembershipId(membershipId)
                .orElse(null);
    }
}
