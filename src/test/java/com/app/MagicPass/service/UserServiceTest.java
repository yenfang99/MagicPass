package com.app.MagicPass.service;

import com.app.MagicPass.model.User;
import com.app.MagicPass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllUsers_ReturnsListFromRepository() {
        User u1 = new User(); u1.setId(1L); u1.setEmail("a@example.com");
        User u2 = new User(); u2.setId(2L); u2.setEmail("b@example.com");
        when(userRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUserById_WhenNotFound_ShouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(RuntimeException.class, () -> userService.getUserById(99L));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void createUser_CallsSave() {
        User u = new User(); u.setEmail("new@example.com");
        when(userRepository.save(u)).thenReturn(u);

        User saved = userService.createUser(u);
        assertNotNull(saved);
        verify(userRepository, times(1)).save(u);
    }

    @Test
    void updateUser_UpdatesEmail() {
        User existing = new User(); existing.setId(5L); existing.setEmail("old@example.com");
        when(userRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User updated = new User(); updated.setEmail("new@example.com");
        User result = userService.updateUser(5L, updated);

        assertEquals("new@example.com", result.getEmail());
        verify(userRepository, times(1)).save(existing);
    }

    @Test
    void deleteUser_CallsRepository() {
        doNothing().when(userRepository).deleteById(3L);
        userService.deleteUser(3L);
        verify(userRepository, times(1)).deleteById(3L);
    }
}
