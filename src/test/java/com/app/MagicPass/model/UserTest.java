package com.app.MagicPass.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the User model
 */
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void testGetterSetter_EmailAndPassword() {
        user.setEmail("tester@example.com");
        user.setPasswordHash("hashedpassword123");

        assertEquals("tester@example.com", user.getEmail());
        assertEquals("hashedpassword123", user.getPasswordHash());
    }

    @Test
    void testDefaultMemberFlag_IsTrueAndCanBeToggled() {
        // default should be true as set in model
        assertTrue(user.isMember(), "Default member flag should be true");

        user.setMember(false);
        assertFalse(user.isMember(), "Member flag should be changeable to false");
    }

    @Test
    void testCreatedAt_IsInitializedOnConstruct() {
        LocalDateTime created = user.getCreatedAt();
        assertNotNull(created, "createdAt should be initialized");

        // createdAt should be reasonably recent (within last 5 seconds)
        LocalDateTime now = LocalDateTime.now();
        assertFalse(created.isAfter(now));
        assertTrue(created.isAfter(now.minusSeconds(10)) || created.isEqual(now) , "createdAt should be set to now on construction");
    }

    @Test
    void testIdSetterGetter() {
        user.setId(123L);
        assertEquals(123L, user.getId());
    }
}
