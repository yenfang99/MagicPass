package com.app.MagicPass.service;

import org.springframework.stereotype.Service;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.model.Order;
import com.app.MagicPass.repository.OrderRepository;

@Service
public class OrderService {

  private final OrderRepository orderRepository;

  public OrderService(OrderRepository orderRepository) {
    this.orderRepository = orderRepository;
  }

  public java.util.List<Order> getOrdersForUser(Long userId) {
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  public Order createPaidOrder(CheckoutRequest req, PricingBreakdown pricing, String paymentMethod) {
    Order order = new Order();
    order.setUserId(req.getUserId());
    order.setAdultQty(req.getAdultQty());
    order.setStudentQty(req.getStudentQty());
    order.setChildQty(req.getChildQty());
    order.setReservationDate(req.getReservationDate());

    order.setTotal(pricing.getTotal());
    order.setDiscount(pricing.getDiscount());
    order.setTax(pricing.getTax());
    order.setGrandTotal(pricing.getGrandTotal());

    order.setStatus("PAID");
    order.setPaymentMethod(paymentMethod);

    return orderRepository.save(order);
  }

  public Order getById(Long orderId) {
    return orderRepository.findById(orderId).orElseThrow();
  }
}
