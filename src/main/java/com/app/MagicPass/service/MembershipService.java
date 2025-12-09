package com.app.MagicPass.service;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.repository.MembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final MembershipTypeService membershipTypeService;

    @Autowired
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

        // Check if user already has an active membership, expire it before replacing
        Membership existingMembership = getActiveMembership(userId);
        if (existingMembership != null) {
            existingMembership.setStatus(Membership.MembershipStatus.EXPIRED);
            membershipRepository.save(existingMembership);
        }

        // Create new membership using dynamic type data
        Membership membership = new Membership();
        membership.setMembershipId("MEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        membership.setUserId(userId);  // This references users.id
        membership.setTier(membershipType.getName());
        membership.setPrice(membershipType.getPrice());
        membership.setDiscountRate(membershipType.getDiscountRate());
        membership.setStartDate(LocalDateTime.now());
        membership.setExpiryDate(LocalDateTime.now().plusMonths(membershipType.getDurationMonths()));
        membership.setStatus(Membership.MembershipStatus.ACTIVE);

        return membershipRepository.save(membership);
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

        try {
            MembershipType type = membershipTypeService.getMembershipTypeByName(membership.getTier());
            if (Boolean.TRUE.equals(type.getActive())) {
                return membership;
            }
        } catch (RuntimeException ignored) {
            // Missing type; fall through to return null
        }
        return null;
    }

    public List<Membership> getUserMemberships(Long userId) {
        return membershipRepository.findByUserId(userId);
    }

    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }
}
