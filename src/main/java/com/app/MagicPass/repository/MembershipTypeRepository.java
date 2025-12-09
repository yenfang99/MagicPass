package com.app.MagicPass.repository;

import com.app.MagicPass.model.MembershipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipTypeRepository extends JpaRepository<MembershipType, Long> {

    List<MembershipType> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<MembershipType> findByName(String name);

    List<MembershipType> findAllByOrderByDisplayOrderAsc();
}
