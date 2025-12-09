package com.app.MagicPass.repository;

import com.app.MagicPass.model.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    Optional<Membership> findByMembershipId(String membershipId);

    List<Membership> findByUserId(Long userId);

    List<Membership> findByUserIdAndStatus(Long userId, Membership.MembershipStatus status);

    Optional<Membership> findFirstByUserIdAndStatusOrderByExpiryDateDesc(
            Long userId, Membership.MembershipStatus status);

    List<Membership> findByStatus(Membership.MembershipStatus status);
}
