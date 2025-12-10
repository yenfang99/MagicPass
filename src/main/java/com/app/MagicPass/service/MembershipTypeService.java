package com.app.MagicPass.service;

import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.repository.MembershipTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MembershipTypeService {

    private final MembershipTypeRepository membershipTypeRepository;

    public MembershipTypeService(MembershipTypeRepository membershipTypeRepository) {
        this.membershipTypeRepository = membershipTypeRepository;
    }

    /**
     * Get all membership types (for admin)
     */
    public List<MembershipType> getAllMembershipTypes() {
        return membershipTypeRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Get only active membership types (for customers)
     */
    public List<MembershipType> getActiveMembershipTypes() {
        return membershipTypeRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Get membership type by ID
     */
    public MembershipType getMembershipTypeById(Long id) {
        return membershipTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Membership type not found with id: " + id));
    }

    /**
     * Get membership type by name
     */
    public MembershipType getMembershipTypeByName(String name) {
        return membershipTypeRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Membership type not found with name: " + name));
    }

    /**
     * Create new membership type
     */
    @Transactional
    public MembershipType createMembershipType(MembershipType membershipType) {
        // Check if name already exists
        if (membershipTypeRepository.findByName(membershipType.getName()).isPresent()) {
            throw new IllegalArgumentException("Membership type with name '" + membershipType.getName() + "' already exists");
        }

        return membershipTypeRepository.save(membershipType);
    }

    /**
     * Update existing membership type
     */
    @Transactional
    public MembershipType updateMembershipType(Long id, MembershipType updatedType) {
        MembershipType existing = getMembershipTypeById(id);

        // Check if changing name to one that already exists
        if (!existing.getName().equals(updatedType.getName())) {
            if (membershipTypeRepository.findByName(updatedType.getName()).isPresent()) {
                throw new IllegalArgumentException("Membership type with name '" + updatedType.getName() + "' already exists");
            }
        }

        existing.setName(updatedType.getName());
        existing.setDisplayName(updatedType.getDisplayName());
        existing.setDescription(updatedType.getDescription());
        existing.setPrice(updatedType.getPrice());
        existing.setDiscountRate(updatedType.getDiscountRate());
        existing.setDurationMonths(updatedType.getDurationMonths());
        existing.setActive(updatedType.getActive());
        existing.setDisplayOrder(updatedType.getDisplayOrder());
        existing.setTierLevel(updatedType.getTierLevel());

        return membershipTypeRepository.save(existing);
    }

    /**
     * Delete membership type
     */
    @Transactional
    public void deleteMembershipType(Long id) {
        MembershipType membershipType = getMembershipTypeById(id);
        membershipTypeRepository.delete(membershipType);
    }

    /**
     * Toggle active status
     */
    @Transactional
    public MembershipType toggleActiveStatus(Long id) {
        MembershipType membershipType = getMembershipTypeById(id);
        membershipType.setActive(!membershipType.getActive());
        return membershipTypeRepository.save(membershipType);
    }
}
