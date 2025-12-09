package com.app.MagicPass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.app.MagicPass.model.Order;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders by status
     * @param status Order status (e.g., "PAID", "PREVIEW")
     * @return List of orders with the specified status
     */
    List<Order> findByStatus(String status);

    /**
     * Find all orders for a user, newest first.
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
