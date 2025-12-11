package com.app.MagicPass.service;

import com.app.MagicPass.model.User;
import com.app.MagicPass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService: register() + login()
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository);
    }

    // Helper to call the private hashPassword method so our test hash
    // is always consistent with the real implementation.
    private String hash(String raw) {
        try {
            Method m = AuthService.class.getDeclaredMethod("hashPassword", String.class);
            m.setAccessible(true);
            return (String) m.invoke(authService, raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------------------------
    // REGISTER TESTS
    // --------------------------------------------------------------------

    @Test
    void register_WithValidData_ShouldSaveAndReturnUser() {
        String email = "User@Test.com";   // will be normalised to lower-case
        String normalized = "user@test.com";
        String password = "Passw0rd!";

        when(userRepository.existsByEmail(normalized)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = authService.register(email, password);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(normalized, result.getEmail());
        // Just basic sanity check: password should not be stored in plain text
        assertNotEquals(password, result.getPasswordHash());

        verify(userRepository, times(1)).existsByEmail(normalized);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_NullOrBlankEmail_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(null, "Passw0rd!"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("   ", "Passw0rd!"));

        verifyNoInteractions(userRepository);
    }

    @Test
    void register_InvalidEmailFormat_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("not-an-email", "Passw0rd!"));

        verifyNoInteractions(userRepository);
    }

    @Test
    void register_StaffDomainEmail_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("staff@magicpass.my", "Passw0rd!"));

        verifyNoInteractions(userRepository);
    }

    @Test
    void register_PasswordTooShort_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("user@test.com", "Abc!12"));

        verifyNoInteractions(userRepository);
    }

    @Test
    void register_PasswordMissingSpecialChar_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("user@test.com", "Abc12345"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void register_EmailAlreadyExists_ShouldThrow() {
        String email = "user@test.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(email, "Passw0rd!"));

        verify(userRepository, times(1)).existsByEmail(email);
        verify(userRepository, never()).save(any(User.class));
    }

    // --------------------------------------------------------------------
    // LOGIN TESTS (your existing ones – kept as-is)
    // --------------------------------------------------------------------

    @Test
    void login_WithValidCredentials_ReturnsUser() {
        // Given
        String email = "user@test.com";
        String password = "Passw0rd!";

        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(hash(password));

        when(userRepository.findByEmail(email.toLowerCase()))
                .thenReturn(Optional.of(user));

        // When
        User result = authService.login(email, password);

        // Then
        assertNotNull(result);
        assertEquals(email.toLowerCase(), result.getEmail());
        verify(userRepository, times(1))
                .findByEmail(email.toLowerCase());
    }

    @Test
    void login_WithNullEmailOrPassword_ThrowsException() {
        // email null
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(null, "password"));

        // password null
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("user@test.com", null));

        verifyNoInteractions(userRepository);
    }

    @Test
    void login_UserNotFound_ThrowsInvalidEmailOrPassword() {
        String email = "missing@test.com";
        String password = "Passw0rd!";

        when(userRepository.findByEmail(email.toLowerCase()))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> authService.login(email, password));

        verify(userRepository, times(1))
                .findByEmail(email.toLowerCase());
    }

    @Test
    void login_WrongPassword_ThrowsInvalidEmailOrPassword() {
        String email = "user@test.com";
        String correctPassword = "Passw0rd!";
        String wrongPassword = "WrongPass1!";

        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(hash(correctPassword));

        when(userRepository.findByEmail(email.toLowerCase()))
                .thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> authService.login(email, wrongPassword));

        verify(userRepository, times(1))
                .findByEmail(email.toLowerCase());
    }
}
