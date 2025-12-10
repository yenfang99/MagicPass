package com.app.MagicPass.service;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.repository.MembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MembershipService
 * Tests membership purchase, upgrade logic, and retrieval methods
 */
@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipTypeService membershipTypeService;

    @InjectMocks
    private MembershipService membershipService;

    private MembershipType silverType;
    private MembershipType goldType;
    private MembershipType platinumType;
    private Membership activeSilverMembership;

    @BeforeEach
    void setUp() {
        // Silver membership type
        silverType = new MembershipType();
        silverType.setId(1L);
        silverType.setName("Silver");
        silverType.setDisplayName("Silver Membership");
        silverType.setPrice(99.99);
        silverType.setDiscountRate(0.05);
        silverType.setDurationMonths(12);
        silverType.setActive(true);
        silverType.setTierLevel(1);

        // Gold membership type
        goldType = new MembershipType();
        goldType.setId(2L);
        goldType.setName("Gold");
        goldType.setDisplayName("Gold Membership");
        goldType.setPrice(199.99);
        goldType.setDiscountRate(0.10);
        goldType.setDurationMonths(12);
        goldType.setActive(true);
        goldType.setTierLevel(2);

        // Platinum membership type
        platinumType = new MembershipType();
        platinumType.setId(3L);
        platinumType.setName("Platinum");
        platinumType.setDisplayName("Platinum Membership");
        platinumType.setPrice(299.99);
        platinumType.setDiscountRate(0.15);
        platinumType.setDurationMonths(12);
        platinumType.setActive(true);
        platinumType.setTierLevel(3);

        // Existing active membership
        activeSilverMembership = new Membership();
        activeSilverMembership.setMembershipId("MEM-12345678");
        activeSilverMembership.setUserId(100L);
        activeSilverMembership.setMembershipType(silverType);
        activeSilverMembership.setPrice(99.99);
        activeSilverMembership.setDiscountRate(0.05);
        activeSilverMembership.setStartDate(LocalDateTime.now());
        activeSilverMembership.setExpiryDate(LocalDateTime.now().plusMonths(12));
        activeSilverMembership.setStatus(Membership.MembershipStatus.ACTIVE);
    }

    @Test
    void testPurchaseMembership_NewUser_ShouldSucceed() {
        // Given
        Long userId = 100L;
        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(membershipRepository.save(any(Membership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Membership result = membershipService.purchaseMembership(userId, silverType);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(silverType, result.getMembershipType());
        assertEquals(99.99, result.getPrice());
        assertEquals(0.05, result.getDiscountRate());
        assertEquals(Membership.MembershipStatus.ACTIVE, result.getStatus());
        assertTrue(result.getMembershipId().startsWith("MEM-"));

        verify(membershipRepository, times(1)).save(any(Membership.class));
        verify(membershipRepository, never()).save(argThat(m ->
            m.getStatus() == Membership.MembershipStatus.EXPIRED));
    }

    @Test
    void testPurchaseMembership_WithInactiveType_ShouldThrowException() {
        // Given
        MembershipType inactiveType = new MembershipType();
        inactiveType.setActive(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.purchaseMembership(100L, inactiveType);
        });

        assertEquals("Membership type is inactive or missing", exception.getMessage());
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void testPurchaseMembership_WithNullType_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipService.purchaseMembership(100L, null);
        });

        assertEquals("Membership type is inactive or missing", exception.getMessage());
    }

    @Test
    void testPurchaseMembership_UpgradeFromSilverToGold_ShouldSucceed() {
        // Given
        Long userId = 100L;
        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(activeSilverMembership));
        when(membershipRepository.save(any(Membership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Membership result = membershipService.purchaseMembership(userId, goldType);

        // Then
        assertNotNull(result);
        assertEquals(goldType, result.getMembershipType());
        assertEquals(Membership.MembershipStatus.ACTIVE, result.getStatus());

        // Verify old membership was expired
        ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepository, times(2)).save(captor.capture());

        List<Membership> savedMemberships = captor.getAllValues();
        assertEquals(Membership.MembershipStatus.EXPIRED, savedMemberships.get(0).getStatus());
        assertEquals(Membership.MembershipStatus.ACTIVE, savedMemberships.get(1).getStatus());
    }

    @Test
    void testPurchaseMembership_BuySameTier_ShouldThrowException() {
        // Given
        Long userId = 100L;
        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(activeSilverMembership));

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            membershipService.purchaseMembership(userId, silverType);
        });

        assertTrue(exception.getMessage().contains("You already have an active"));
        assertTrue(exception.getMessage().contains("You can only upgrade to a higher tier"));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void testPurchaseMembership_Downgrade_ShouldThrowException() {
        // Given
        Long userId = 100L;
        Membership activeGoldMembership = new Membership();
        activeGoldMembership.setUserId(userId);
        activeGoldMembership.setMembershipType(goldType);
        activeGoldMembership.setStatus(Membership.MembershipStatus.ACTIVE);

        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(activeGoldMembership));

        // When & Then - Try to buy Silver when user has Gold
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            membershipService.purchaseMembership(userId, silverType);
        });

        assertTrue(exception.getMessage().contains("You already have an active"));
        verify(membershipRepository, never()).save(any(Membership.class));
    }

    @Test
    void testCanPurchaseMembershipType_NoExistingMembership_ShouldReturnNull() {
        // Given
        Long userId = 100L;
        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // When
        String result = membershipService.canPurchaseMembershipType(userId, silverType);

        // Then
        assertNull(result);
    }

    @Test
    void testCanPurchaseMembershipType_UpgradeAllowed_ShouldReturnNull() {
        // Given
        Long userId = 100L;
        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(activeSilverMembership));

        // When
        String result = membershipService.canPurchaseMembershipType(userId, goldType);

        // Then
        assertNull(result);
    }

    @Test
    void testCanPurchaseMembershipType_SameTier_ShouldReturnError() {
        // Given
        Long userId = 100L;
        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(activeSilverMembership));

        // When
        String result = membershipService.canPurchaseMembershipType(userId, silverType);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("You already have an active"));
    }

    @Test
    void testGetActiveMembership_WithActiveMembership_ShouldReturnMembership() {
        // Given
        Long userId = 100L;
        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(activeSilverMembership));

        // When
        Membership result = membershipService.getActiveMembership(userId);

        // Then
        assertNotNull(result);
        assertEquals(activeSilverMembership, result);
        verify(membershipRepository, times(1)).findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE);
    }

    @Test
    void testGetActiveMembership_NoActiveMembership_ShouldReturnNull() {
        // Given
        Long userId = 100L;
        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // When
        Membership result = membershipService.getActiveMembership(userId);

        // Then
        assertNull(result);
    }

    @Test
    void testGetActiveMembershipWithActiveType_BothActive_ShouldReturnMembership() {
        // Given
        Long userId = 100L;
        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(activeSilverMembership));
        when(membershipTypeService.getMembershipTypeById(1L))
                .thenReturn(silverType);

        // When
        Membership result = membershipService.getActiveMembershipWithActiveType(userId);

        // Then
        assertNotNull(result);
        assertEquals(activeSilverMembership, result);
    }

    @Test
    void testGetActiveMembershipWithActiveType_TypeInactive_ShouldReturnNull() {
        // Given
        Long userId = 100L;
        MembershipType inactiveSilverType = new MembershipType();
        inactiveSilverType.setId(1L);
        inactiveSilverType.setActive(false);

        when(membershipRepository.findFirstByUserIdAndStatusOrderByExpiryDateDesc(
                userId, Membership.MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(activeSilverMembership));
        when(membershipTypeService.getMembershipTypeById(1L))
                .thenReturn(inactiveSilverType);

        // When
        Membership result = membershipService.getActiveMembershipWithActiveType(userId);

        // Then
        assertNull(result);
    }

    @Test
    void testGetUserMemberships() {
        // Given
        Long userId = 100L;
        List<Membership> memberships = Arrays.asList(activeSilverMembership);
        when(membershipRepository.findByUserId(userId)).thenReturn(memberships);

        // When
        List<Membership> result = membershipService.getUserMemberships(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(activeSilverMembership, result.get(0));
        verify(membershipRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testGetAllMemberships() {
        // Given
        List<Membership> memberships = Arrays.asList(activeSilverMembership);
        when(membershipRepository.findAll()).thenReturn(memberships);

        // When
        List<Membership> result = membershipService.getAllMemberships();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(membershipRepository, times(1)).findAll();
    }

    @Test
    void testGetMembershipByMembershipId_Found() {
        // Given
        String membershipId = "MEM-12345678";
        when(membershipRepository.findByMembershipId(membershipId))
                .thenReturn(Optional.of(activeSilverMembership));

        // When
        Membership result = membershipService.getMembershipByMembershipId(membershipId);

        // Then
        assertNotNull(result);
        assertEquals(activeSilverMembership, result);
    }

    @Test
    void testGetMembershipByMembershipId_NotFound() {
        // Given
        String membershipId = "MEM-NOTFOUND";
        when(membershipRepository.findByMembershipId(membershipId))
                .thenReturn(Optional.empty());

        // When
        Membership result = membershipService.getMembershipByMembershipId(membershipId);

        // Then
        assertNull(result);
    }
}
