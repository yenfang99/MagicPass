package com.app.MagicPass.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.app.MagicPass.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
