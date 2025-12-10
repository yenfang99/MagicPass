package com.app.MagicPass.service;

import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.repository.MembershipTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MembershipTypeService
 * Tests CRUD operations, validation logic, and active status toggling
 */
@ExtendWith(MockitoExtension.class)
class MembershipTypeServiceTest {

    @Mock
    private MembershipTypeRepository membershipTypeRepository;

    @InjectMocks
    private MembershipTypeService membershipTypeService;

    private MembershipType silverType;
    private MembershipType goldType;
    private MembershipType platinumType;

    @BeforeEach
    void setUp() {
        silverType = new MembershipType();
        silverType.setId(1L);
        silverType.setName("Silver");
        silverType.setDisplayName("Silver Membership");
        silverType.setDescription("Basic membership benefits");
        silverType.setPrice(99.99);
        silverType.setDiscountRate(0.05);
        silverType.setDurationMonths(12);
        silverType.setActive(true);
        silverType.setDisplayOrder(1);
        silverType.setTierLevel(1);

        goldType = new MembershipType();
        goldType.setId(2L);
        goldType.setName("Gold");
        goldType.setDisplayName("Gold Membership");
        goldType.setDescription("Premium membership benefits");
        goldType.setPrice(199.99);
        goldType.setDiscountRate(0.10);
        goldType.setDurationMonths(12);
        goldType.setActive(true);
        goldType.setDisplayOrder(2);
        goldType.setTierLevel(2);

        platinumType = new MembershipType();
        platinumType.setId(3L);
        platinumType.setName("Platinum");
        platinumType.setDisplayName("Platinum Membership");
        platinumType.setDescription("Elite membership benefits");
        platinumType.setPrice(299.99);
        platinumType.setDiscountRate(0.15);
        platinumType.setDurationMonths(12);
        platinumType.setActive(false);
        platinumType.setDisplayOrder(3);
        platinumType.setTierLevel(3);
    }

    @Test
    void testGetAllMembershipTypes_ShouldReturnAllTypes() {
        // Given
        List<MembershipType> allTypes = Arrays.asList(silverType, goldType, platinumType);
        when(membershipTypeRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(allTypes);

        // When
        List<MembershipType> result = membershipTypeService.getAllMembershipTypes();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(silverType, result.get(0));
        assertEquals(goldType, result.get(1));
        assertEquals(platinumType, result.get(2));
        verify(membershipTypeRepository, times(1)).findAllByOrderByDisplayOrderAsc();
    }

    @Test
    void testGetActiveMembershipTypes_ShouldReturnOnlyActiveTypes() {
        // Given
        List<MembershipType> activeTypes = Arrays.asList(silverType, goldType);
        when(membershipTypeRepository.findByActiveTrueOrderByDisplayOrderAsc()).thenReturn(activeTypes);

        // When
        List<MembershipType> result = membershipTypeService.getActiveMembershipTypes();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).getActive());
        assertTrue(result.get(1).getActive());
        verify(membershipTypeRepository, times(1)).findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    void testGetMembershipTypeById_Found_ShouldReturnType() {
        // Given
        Long id = 1L;
        when(membershipTypeRepository.findById(id)).thenReturn(Optional.of(silverType));

        // When
        MembershipType result = membershipTypeService.getMembershipTypeById(id);

        // Then
        assertNotNull(result);
        assertEquals(silverType, result);
        assertEquals("Silver", result.getName());
        verify(membershipTypeRepository, times(1)).findById(id);
    }

    @Test
    void testGetMembershipTypeById_NotFound_ShouldThrowException() {
        // Given
        Long id = 999L;
        when(membershipTypeRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipTypeService.getMembershipTypeById(id);
        });

        assertEquals("Membership type not found with id: 999", exception.getMessage());
        verify(membershipTypeRepository, times(1)).findById(id);
    }

    @Test
    void testGetMembershipTypeByName_Found_ShouldReturnType() {
        // Given
        String name = "Silver";
        when(membershipTypeRepository.findByName(name)).thenReturn(Optional.of(silverType));

        // When
        MembershipType result = membershipTypeService.getMembershipTypeByName(name);

        // Then
        assertNotNull(result);
        assertEquals(silverType, result);
        assertEquals(name, result.getName());
        verify(membershipTypeRepository, times(1)).findByName(name);
    }

    @Test
    void testGetMembershipTypeByName_NotFound_ShouldThrowException() {
        // Given
        String name = "Diamond";
        when(membershipTypeRepository.findByName(name)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipTypeService.getMembershipTypeByName(name);
        });

        assertEquals("Membership type not found with name: Diamond", exception.getMessage());
        verify(membershipTypeRepository, times(1)).findByName(name);
    }

    @Test
    void testCreateMembershipType_NewUniqueName_ShouldSucceed() {
        // Given
        MembershipType newType = new MembershipType();
        newType.setName("Diamond");
        newType.setDisplayName("Diamond Membership");
        newType.setPrice(499.99);
        newType.setDiscountRate(0.20);

        when(membershipTypeRepository.findByName("Diamond")).thenReturn(Optional.empty());
        when(membershipTypeRepository.save(newType)).thenReturn(newType);

        // When
        MembershipType result = membershipTypeService.createMembershipType(newType);

        // Then
        assertNotNull(result);
        assertEquals("Diamond", result.getName());
        verify(membershipTypeRepository, times(1)).findByName("Diamond");
        verify(membershipTypeRepository, times(1)).save(newType);
    }

    @Test
    void testCreateMembershipType_DuplicateName_ShouldThrowException() {
        // Given
        MembershipType duplicateType = new MembershipType();
        duplicateType.setName("Silver");
        duplicateType.setDisplayName("New Silver Membership");

        when(membershipTypeRepository.findByName("Silver")).thenReturn(Optional.of(silverType));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipTypeService.createMembershipType(duplicateType);
        });

        assertEquals("Membership type with name 'Silver' already exists", exception.getMessage());
        verify(membershipTypeRepository, times(1)).findByName("Silver");
        verify(membershipTypeRepository, never()).save(any(MembershipType.class));
    }

    @Test
    void testUpdateMembershipType_SameName_ShouldSucceed() {
        // Given
        Long id = 1L;
        MembershipType updatedType = new MembershipType();
        updatedType.setName("Silver");
        updatedType.setDisplayName("Updated Silver Membership");
        updatedType.setDescription("Updated description");
        updatedType.setPrice(109.99);
        updatedType.setDiscountRate(0.06);
        updatedType.setDurationMonths(12);
        updatedType.setActive(true);
        updatedType.setDisplayOrder(1);
        updatedType.setTierLevel(1);

        when(membershipTypeRepository.findById(id)).thenReturn(Optional.of(silverType));
        when(membershipTypeRepository.save(any(MembershipType.class))).thenReturn(silverType);

        // When
        MembershipType result = membershipTypeService.updateMembershipType(id, updatedType);

        // Then
        assertNotNull(result);
        assertEquals("Silver", result.getName());
        assertEquals("Updated Silver Membership", result.getDisplayName());
        assertEquals(109.99, result.getPrice());
        verify(membershipTypeRepository, times(1)).findById(id);
        verify(membershipTypeRepository, times(1)).save(silverType);
        verify(membershipTypeRepository, never()).findByName(any());
    }

    @Test
    void testUpdateMembershipType_ChangeName_Unique_ShouldSucceed() {
        // Given
        Long id = 1L;
        MembershipType updatedType = new MembershipType();
        updatedType.setName("Bronze");
        updatedType.setDisplayName("Bronze Membership");
        updatedType.setDescription("Basic tier");
        updatedType.setPrice(79.99);
        updatedType.setDiscountRate(0.03);
        updatedType.setDurationMonths(12);
        updatedType.setActive(true);
        updatedType.setDisplayOrder(1);
        updatedType.setTierLevel(1);

        when(membershipTypeRepository.findById(id)).thenReturn(Optional.of(silverType));
        when(membershipTypeRepository.findByName("Bronze")).thenReturn(Optional.empty());
        when(membershipTypeRepository.save(any(MembershipType.class))).thenReturn(silverType);

        // When
        MembershipType result = membershipTypeService.updateMembershipType(id, updatedType);

        // Then
        assertNotNull(result);
        assertEquals("Bronze", result.getName());
        verify(membershipTypeRepository, times(1)).findById(id);
        verify(membershipTypeRepository, times(1)).findByName("Bronze");
        verify(membershipTypeRepository, times(1)).save(silverType);
    }

    @Test
    void testUpdateMembershipType_ChangeName_Duplicate_ShouldThrowException() {
        // Given
        Long id = 1L;
        MembershipType updatedType = new MembershipType();
        updatedType.setName("Gold");
        updatedType.setDisplayName("Updated to Gold");

        when(membershipTypeRepository.findById(id)).thenReturn(Optional.of(silverType));
        when(membershipTypeRepository.findByName("Gold")).thenReturn(Optional.of(goldType));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            membershipTypeService.updateMembershipType(id, updatedType);
        });

        assertEquals("Membership type with name 'Gold' already exists", exception.getMessage());
        verify(membershipTypeRepository, times(1)).findById(id);
        verify(membershipTypeRepository, times(1)).findByName("Gold");
        verify(membershipTypeRepository, never()).save(any(MembershipType.class));
    }

    @Test
    void testUpdateMembershipType_NotFound_ShouldThrowException() {
        // Given
        Long id = 999L;
        MembershipType updatedType = new MembershipType();
        updatedType.setName("Test");

        when(membershipTypeRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipTypeService.updateMembershipType(id, updatedType);
        });

        assertEquals("Membership type not found with id: 999", exception.getMessage());
        verify(membershipTypeRepository, times(1)).findById(id);
        verify(membershipTypeRepository, never()).save(any(MembershipType.class));
    }

    @Test
    void testDeleteMembershipType_Found_ShouldDelete() {
        // Given
        Long id = 1L;
        when(membershipTypeRepository.findById(id)).thenReturn(Optional.of(silverType));
        doNothing().when(membershipTypeRepository).delete(silverType);

        // When
        membershipTypeService.deleteMembershipType(id);

        // Then
        verify(membershipTypeRepository, times(1)).findById(id);
        verify(membershipTypeRepository, times(1)).delete(silverType);
    }

    @Test
    void testDeleteMembershipType_NotFound_ShouldThrowException() {
        // Given
        Long id = 999L;
        when(membershipTypeRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipTypeService.deleteMembershipType(id);
        });

        assertEquals("Membership type not found with id: 999", exception.getMessage());
        verify(membershipTypeRepository, times(1)).findById(id);
        verify(membershipTypeRepository, never()).delete(any(MembershipType.class));
    }

    @Test
    void testToggleActiveStatus_ActiveToInactive_ShouldSucceed() {
        // Given
        Long id = 1L;
        when(membershipTypeRepository.findById(id)).thenReturn(Optional.of(silverType));
        when(membershipTypeRepository.save(any(MembershipType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MembershipType result = membershipTypeService.toggleActiveStatus(id);

        // Then
        assertNotNull(result);
        assertFalse(result.getActive());
        verify(membershipTypeRepository, times(1)).findById(id);
        verify(membershipTypeRepository, times(1)).save(silverType);
    }

    @Test
    void testToggleActiveStatus_InactiveToActive_ShouldSucceed() {
        // Given
        Long id = 3L;
        when(membershipTypeRepository.findById(id)).thenReturn(Optional.of(platinumType));
        when(membershipTypeRepository.save(any(MembershipType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        MembershipType result = membershipTypeService.toggleActiveStatus(id);

        // Then
        assertNotNull(result);
        assertTrue(result.getActive());
        verify(membershipTypeRepository, times(1)).findById(id);
        verify(membershipTypeRepository, times(1)).save(platinumType);
    }

    @Test
    void testToggleActiveStatus_NotFound_ShouldThrowException() {
        // Given
        Long id = 999L;
        when(membershipTypeRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            membershipTypeService.toggleActiveStatus(id);
        });

        assertEquals("Membership type not found with id: 999", exception.getMessage());
        verify(membershipTypeRepository, times(1)).findById(id);
        verify(membershipTypeRepository, never()).save(any(MembershipType.class));
    }
}
