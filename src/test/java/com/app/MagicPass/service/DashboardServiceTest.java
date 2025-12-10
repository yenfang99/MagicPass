package com.app.MagicPass.service;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.model.Order;
import com.app.MagicPass.repository.MembershipRepository;
import com.app.MagicPass.repository.MembershipTypeRepository;
import com.app.MagicPass.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardService
 * Tests all dashboard statistics calculations
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MembershipTypeRepository membershipTypeRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Membership membership1;
    private Membership membership2;
    private Order order1;
    private Order order2;
    private MembershipType membershipType1;
    private MembershipType membershipType2;

    @BeforeEach
    void setUp() {
        // Set up test data
        membership1 = new Membership();
        membership1.setId(1L);
        membership1.setStatus(Membership.MembershipStatus.ACTIVE);
        membership1.setPrice(100.0);

        membership2 = new Membership();
        membership2.setId(2L);
        membership2.setStatus(Membership.MembershipStatus.ACTIVE);
        membership2.setPrice(200.0);

        order1 = new Order();
        order1.setStatus("PAID");
        order1.setGrandTotal(50.0);
        order1.setAdultQty(2);
        order1.setStudentQty(1);
        order1.setChildQty(1);

        order2 = new Order();
        order2.setStatus("PAID");
        order2.setGrandTotal(75.0);
        order2.setAdultQty(1);
        order2.setStudentQty(0);
        order2.setChildQty(2);

        membershipType1 = new MembershipType();
        membershipType1.setId(1L);
        membershipType1.setName("Silver");
        membershipType1.setActive(true);

        membershipType2 = new MembershipType();
        membershipType2.setId(2L);
        membershipType2.setName("Gold");
        membershipType2.setActive(true);
    }

    @Test
    void testGetTotalActiveMembers_WithMultipleMembers() {
        // Given
        List<Membership> activeMemberships = Arrays.asList(membership1, membership2);
        when(membershipRepository.findByStatus(Membership.MembershipStatus.ACTIVE))
                .thenReturn(activeMemberships);

        // When
        long result = dashboardService.getTotalActiveMembers();

        // Then
        assertEquals(2, result);
        verify(membershipRepository, times(1)).findByStatus(Membership.MembershipStatus.ACTIVE);
    }

    @Test
    void testGetTotalActiveMembers_WithNoMembers() {
        // Given
        when(membershipRepository.findByStatus(Membership.MembershipStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // When
        long result = dashboardService.getTotalActiveMembers();

        // Then
        assertEquals(0, result);
        verify(membershipRepository, times(1)).findByStatus(Membership.MembershipStatus.ACTIVE);
    }

    @Test
    void testGetTotalTicketsSold_WithMultipleOrders() {
        // Given
        List<Order> paidOrders = Arrays.asList(order1, order2);
        when(orderRepository.findByStatus("PAID")).thenReturn(paidOrders);

        // When
        long result = dashboardService.getTotalTicketsSold();

        // Then
        assertEquals(2, result);
        verify(orderRepository, times(1)).findByStatus("PAID");
    }

    @Test
    void testGetTotalTicketsSold_WithNoOrders() {
        // Given
        when(orderRepository.findByStatus("PAID")).thenReturn(Collections.emptyList());

        // When
        long result = dashboardService.getTotalTicketsSold();

        // Then
        assertEquals(0, result);
        verify(orderRepository, times(1)).findByStatus("PAID");
    }

    @Test
    void testGetTotalRevenue_WithOrdersAndMemberships() {
        // Given
        List<Order> paidOrders = Arrays.asList(order1, order2);
        List<Membership> allMemberships = Arrays.asList(membership1, membership2);

        when(orderRepository.findByStatus("PAID")).thenReturn(paidOrders);
        when(membershipRepository.findAll()).thenReturn(allMemberships);

        // When
        double result = dashboardService.getTotalRevenue();

        // Then
        // Expected: (50.0 + 75.0) + (100.0 + 200.0) = 425.0
        assertEquals(425.0, result, 0.001);
        verify(orderRepository, times(1)).findByStatus("PAID");
        verify(membershipRepository, times(1)).findAll();
    }

    @Test
    void testGetTotalRevenue_WithOnlyOrders() {
        // Given
        List<Order> paidOrders = Arrays.asList(order1, order2);

        when(orderRepository.findByStatus("PAID")).thenReturn(paidOrders);
        when(membershipRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        double result = dashboardService.getTotalRevenue();

        // Then
        // Expected: 50.0 + 75.0 = 125.0
        assertEquals(125.0, result, 0.001);
    }

    @Test
    void testGetTotalRevenue_WithNoData() {
        // Given
        when(orderRepository.findByStatus("PAID")).thenReturn(Collections.emptyList());
        when(membershipRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        double result = dashboardService.getTotalRevenue();

        // Then
        assertEquals(0.0, result, 0.001);
    }

    @Test
    void testGetTotalTicketCount_WithMultipleOrders() {
        // Given
        List<Order> paidOrders = Arrays.asList(order1, order2);
        when(orderRepository.findByStatus("PAID")).thenReturn(paidOrders);

        // When
        int result = dashboardService.getTotalTicketCount();

        // Then
        // Expected: (2+1+1) + (1+0+2) = 7
        assertEquals(7, result);
        verify(orderRepository, times(1)).findByStatus("PAID");
    }

    @Test
    void testGetTotalTicketCount_WithNoOrders() {
        // Given
        when(orderRepository.findByStatus("PAID")).thenReturn(Collections.emptyList());

        // When
        int result = dashboardService.getTotalTicketCount();

        // Then
        assertEquals(0, result);
    }

    @Test
    void testGetTicketRevenue() {
        // Given
        List<Order> paidOrders = Arrays.asList(order1, order2);
        when(orderRepository.findByStatus("PAID")).thenReturn(paidOrders);

        // When
        double result = dashboardService.getTicketRevenue();

        // Then
        // Expected: 50.0 + 75.0 = 125.0
        assertEquals(125.0, result, 0.001);
        verify(orderRepository, times(1)).findByStatus("PAID");
    }

    @Test
    void testGetMembershipRevenue() {
        // Given
        List<Membership> allMemberships = Arrays.asList(membership1, membership2);
        when(membershipRepository.findAll()).thenReturn(allMemberships);

        // When
        double result = dashboardService.getMembershipRevenue();

        // Then
        // Expected: 100.0 + 200.0 = 300.0
        assertEquals(300.0, result, 0.001);
        verify(membershipRepository, times(1)).findAll();
    }

    @Test
    void testGetActiveMembershipTypeCount_WithMultipleTypes() {
        // Given
        List<MembershipType> activeMembershipTypes = Arrays.asList(membershipType1, membershipType2);
        when(membershipTypeRepository.findByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(activeMembershipTypes);

        // When
        long result = dashboardService.getActiveMembershipTypeCount();

        // Then
        assertEquals(2, result);
        verify(membershipTypeRepository, times(1)).findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    void testGetActiveMembershipTypeCount_WithNoTypes() {
        // Given
        when(membershipTypeRepository.findByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(Collections.emptyList());

        // When
        long result = dashboardService.getActiveMembershipTypeCount();

        // Then
        assertEquals(0, result);
        verify(membershipTypeRepository, times(1)).findByActiveTrueOrderByDisplayOrderAsc();
    }
}
