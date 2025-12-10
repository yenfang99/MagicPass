package com.app.MagicPass.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.app.MagicPass.dto.CheckoutRequest;
import com.app.MagicPass.dto.PricingBreakdown;
import com.app.MagicPass.model.Order;
import com.app.MagicPass.repository.OrderRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private CheckoutRequest req;
    private PricingBreakdown pricing;

    @BeforeEach
    void setUp() {
        req = new CheckoutRequest();
        req.setUserId(1L);
        req.setAdultQty(2);
        req.setStudentQty(1);
        req.setChildQty(0);
        req.setReservationDate(LocalDate.now());

        pricing = new PricingBreakdown();
        pricing.setTotal(100);
        pricing.setDiscount(10);
        pricing.setTax(5);
        pricing.setGrandTotal(95);
    }

    @Test
    void getOrdersForUserReturnsRepositoryResult() {
        List<Order> orders = List.of(new Order());
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(orders);

        assertEquals(orders, orderService.getOrdersForUser(1L));
    }

    @Test
    void createPaidOrderMapsFieldsAndSaves() {
        Order saved = new Order();
        ReflectionTestUtils.setField(saved, "id", 99L);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        Order result = orderService.createPaidOrder(req, pricing, "CASH", 120.0, 25.0);

        assertEquals(99L, result.getId());
        verify(orderRepository).save(argThat(order ->
            order.getUserId().equals(req.getUserId()) &&
            order.getAdultQty() == req.getAdultQty() &&
            order.getStudentQty() == req.getStudentQty() &&
            order.getChildQty() == req.getChildQty() &&
            order.getReservationDate().equals(req.getReservationDate()) &&
            order.getTotal() == pricing.getTotal() &&
            order.getDiscount() == pricing.getDiscount() &&
            order.getTax() == pricing.getTax() &&
            order.getGrandTotal() == pricing.getGrandTotal() &&
            "PAID".equals(order.getStatus()) &&
            "CASH".equals(order.getPaymentMethod()) &&
            Double.valueOf(120.0).equals(order.getCashReceived()) &&
            Double.valueOf(25.0).equals(order.getCashChange())
        ));
    }
}
