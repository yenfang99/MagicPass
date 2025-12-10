package com.app.MagicPass.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class OrderTest {

    @Test
    void onCreateSetsCreatedAtAndDefaultStatus() {
        Order order = new Order();
        order.onCreate();

        assertNotNull(order.getCreatedAt());
        assertEquals("PREVIEW", order.getStatus());
    }

    @Test
    void onCreateDoesNotOverrideExistingStatus() {
        Order order = new Order();
        order.setStatus("PAID");
        order.onCreate();

        assertEquals("PAID", order.getStatus());
        LocalDateTime created = order.getCreatedAt();
        assertNotNull(created);
    }
}
