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
        // Create membership for the user ID from your existing users table
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
