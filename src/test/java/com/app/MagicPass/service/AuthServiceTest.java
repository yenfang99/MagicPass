package com.app.MagicPass.service;

import com.app.MagicPass.model.User;
import com.app.MagicPass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository);
    }

    // helper to hash passwords consistently
    private String hash(String raw) {
        try {
            Method m = AuthService.class.getDeclaredMethod("hashPassword", String.class);
            m.setAccessible(true);
            return (String) m.invoke(authService, raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------------------------------------------
    // REGISTER
    // ----------------------------------------------------------------

    @Test
    void register_ValidData_ShouldSaveUser() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User user = authService.register(
                "John Doe",
                "user@test.com",
                "Passw0rd!",
                LocalDate.of(2003, 11, 11),
                "M",
                "0176989118"
        );

        assertNotNull(user);
        assertEquals("John Doe", user.getName());
        assertEquals(22, user.getAge());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_PasswordTooShort_ShouldThrow() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            authService.register(
                    "John",
                    "user@test.com",
                    "Abc!12",                  // too short
                    LocalDate.of(2003, 11, 11),
                    "M",
                    "0176989118"
            )
        );
    }

    @Test
    void register_PasswordMissingSpecialChar_ShouldThrow() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            authService.register(
                    "John",
                    "user@test.com",
                    "Password1",               // no special char
                    LocalDate.of(2003, 11, 11),
                    "M",
                    "0176989118"
            )
        );
    }

    @Test
    void register_InvalidBirthdayFuture_ShouldThrow() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            authService.register(
                    "John",
                    "user@test.com",
                    "Passw0rd!",
                    LocalDate.now().plusDays(1),
                    "M",
                    "0176989118"
            )
        );
    }

    @Test
    void register_InvalidGender_ShouldThrow() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
            authService.register(
                    "John",
                    "user@test.com",
                    "Passw0rd!",
                    LocalDate.of(2003, 11, 11),
                    "X",
                    "0176989118"
            )
        );
    }

    @Test
    void register_EmailAlreadyExists_ShouldThrow() {
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
            authService.register(
                    "John",
                    "user@test.com",
                    "Passw0rd!",
                    LocalDate.of(2003, 11, 11),
                    "M",
                    "0176989118"
            )
        );

        verify(userRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // LOGIN
    // ----------------------------------------------------------------

    @Test
    void login_ValidCredentials_ShouldReturnUser() {
        User user = new User();
        user.setEmail("user@test.com");
        user.setPasswordHash(hash("Correct1!"));

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        User result = authService.login("user@test.com", "Correct1!");

        assertNotNull(result);
    }

    @Test
    void login_UserNotFound_ShouldThrow() {
        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                authService.login("missing@test.com", "Passw0rd!")
        );
    }

    @Test
    void login_WrongPassword_ShouldThrow() {
        User user = new User();
        user.setEmail("user@test.com");
        user.setPasswordHash(hash("Correct1!"));

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () ->
                authService.login("user@test.com", "Wrong1!")
        );
    }
}
