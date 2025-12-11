package com.app.MagicPass.service;

import com.app.MagicPass.model.Staff;
import com.app.MagicPass.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StaffService: createStaff() and deleteStaff()
 */
@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    private StaffService staffService;

    @BeforeEach
    void setUp() {
        staffService = new StaffService(staffRepository);
    }

    // ---------------------------------------------------------
    // createStaff
    // ---------------------------------------------------------

    @Test
    void createStaff_WithValidInput_ShouldSaveStaffWithNormalizedEmailAndHashedPassword() {
        // Given
        String rawEmail = "NewStaff@MagicPass.my";
        String normalizedEmail = "newstaff@magicpass.my";
        String password = "Password1!";

        when(staffRepository.existsByEmail(normalizedEmail)).thenReturn(false);
        when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> {
            Staff s = invocation.getArgument(0);
            s.setId(1L);
            return s;
        });

        // When
        staffService.createStaff(rawEmail, password);

        // Then
        ArgumentCaptor<Staff> captor = ArgumentCaptor.forClass(Staff.class);
        verify(staffRepository, times(1)).existsByEmail(normalizedEmail);
        verify(staffRepository, times(1)).save(captor.capture());

        Staff saved = captor.getValue();
        assertEquals(normalizedEmail, saved.getEmail());
        assertFalse(saved.isBoss());
        assertNotNull(saved.getPasswordHash());
        assertNotEquals(password, saved.getPasswordHash()); // must be hashed
    }

    @Test
    void createStaff_NullOrBlankEmail_ShouldThrow() {
        IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> staffService.createStaff(null, "Password1!")
        );
        assertEquals("Staff email is required.", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> staffService.createStaff("   ", "Password1!")
        );
        assertEquals("Staff email is required.", ex2.getMessage());

        verifyNoInteractions(staffRepository);
    }

    @Test
    void createStaff_InvalidEmailDomain_ShouldThrow() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> staffService.createStaff("user@gmail.com", "Password1!")
        );
        assertEquals("Staff email must end with @magicpass.my.", ex.getMessage());

        // invalid domain should fail before repository call
        verifyNoInteractions(staffRepository);
    }

    @Test
    void createStaff_EmailAlreadyExists_ShouldThrow() {
        String email = "staff@magicpass.my";
        when(staffRepository.existsByEmail(email)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> staffService.createStaff(email, "Password1!")
        );
        assertEquals("This staff email already exists.", ex.getMessage());

        verify(staffRepository, times(1)).existsByEmail(email);
        verify(staffRepository, never()).save(any(Staff.class));
    }

    @Test
    void createStaff_PasswordTooShort_ShouldThrow() {
        String email = "staff@magicpass.my";

        // service checks existsByEmail() first, then password length
        when(staffRepository.existsByEmail(email)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> staffService.createStaff(email, "Abc!12")
        );
        assertEquals("Password must be at least 8 characters.", ex.getMessage());

        verify(staffRepository, times(1)).existsByEmail(email);
        verify(staffRepository, never()).save(any(Staff.class));
    }

    @Test
    void createStaff_PasswordMissingSpecialChar_ShouldThrow() {
        String email = "staff@magicpass.my";

        when(staffRepository.existsByEmail(email)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> staffService.createStaff(email, "Abc12345")
        );
        assertEquals(
                "Password must contain at least one special character (e.g. !,@,#).",
                ex.getMessage()
        );

        verify(staffRepository, times(1)).existsByEmail(email);
        verify(staffRepository, never()).save(any(Staff.class));
    }

        // ---------------------------------------------------------
    // deleteStaff
    // ---------------------------------------------------------

    @Test
    void deleteStaff_NormalStaff_ShouldDeleteSuccessfully() {
        Long id = 10L;

        Staff staff = new Staff();
        staff.setId(id);
        staff.setEmail("staff@magicpass.my");
        staff.setBoss(false);

        when(staffRepository.findById(id)).thenReturn(java.util.Optional.of(staff));

        staffService.deleteStaff(id);

        verify(staffRepository, times(1)).findById(id);
        verify(staffRepository, times(1)).delete(staff);
    }

    @Test
    void deleteStaff_BossStaff_ShouldThrowAndNotDelete() {
        Long id = 1L;

        Staff boss = new Staff();
        boss.setId(id);
        boss.setEmail("boss@magicpass.my");
        boss.setBoss(true);

        when(staffRepository.findById(id)).thenReturn(java.util.Optional.of(boss));

        assertThrows(IllegalArgumentException.class,
                () -> staffService.deleteStaff(id));

        verify(staffRepository, times(1)).findById(id);
        verify(staffRepository, never()).delete(any(Staff.class));
    }

    @Test
    void deleteStaff_StaffNotFound_ShouldNotDelete() {
        Long id = 99L;

        when(staffRepository.findById(id)).thenReturn(java.util.Optional.empty());

        staffService.deleteStaff(id);

        verify(staffRepository, times(1)).findById(id);
        verify(staffRepository, never()).delete(any(Staff.class));
    }
}

