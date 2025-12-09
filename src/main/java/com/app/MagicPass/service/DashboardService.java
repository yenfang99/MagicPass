package com.app.MagicPass.service;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.model.Order;
import com.app.MagicPass.repository.MembershipRepository;
import com.app.MagicPass.repository.MembershipTypeRepository;
import com.app.MagicPass.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final MembershipRepository membershipRepository;
    private final OrderRepository orderRepository;
    private final MembershipTypeRepository membershipTypeRepository;

    public DashboardService(MembershipRepository membershipRepository, OrderRepository orderRepository, MembershipTypeRepository membershipTypeRepository) {
        this.membershipRepository = membershipRepository;
        this.orderRepository = orderRepository;
        this.membershipTypeRepository = membershipTypeRepository;
    }

    /**
     * Get total number of active members
     */
    public long getTotalActiveMembers() {
        List<Membership> activeMemberships = membershipRepository.findByStatus(Membership.MembershipStatus.ACTIVE);
        return activeMemberships.size();
    }

    /**
     * Get total number of paid orders (tickets sold)
     */
    public long getTotalTicketsSold() {
        List<Order> paidOrders = orderRepository.findByStatus("PAID");
        return paidOrders.size();
    }

    /**
     * Calculate total revenue from orders and memberships
     * Revenue = Sum of all paid order grand totals + Sum of all membership prices
     */
    public double getTotalRevenue() {
        // Revenue from ticket orders
        List<Order> paidOrders = orderRepository.findByStatus("PAID");
        double orderRevenue = paidOrders.stream()
                .mapToDouble(Order::getGrandTotal)
                .sum();

        // Revenue from memberships (all memberships, not just active)
        List<Membership> allMemberships = membershipRepository.findAll();
        double membershipRevenue = allMemberships.stream()
                .mapToDouble(Membership::getPrice)
                .sum();

        return orderRevenue + membershipRevenue;
    }

    /**
     * Get total count of all tickets (sum of quantities from all paid orders)
     */
    public int getTotalTicketCount() {
        List<Order> paidOrders = orderRepository.findByStatus("PAID");
        return paidOrders.stream()
                .mapToInt(order -> order.getAdultQty() + order.getStudentQty() + order.getChildQty())
                .sum();
    }

    /**
     * Get revenue from tickets only
     */
    public double getTicketRevenue() {
        List<Order> paidOrders = orderRepository.findByStatus("PAID");
        return paidOrders.stream()
                .mapToDouble(Order::getGrandTotal)
                .sum();
    }

    /**
     * Get revenue from memberships only
     */
    public double getMembershipRevenue() {
        List<Membership> allMemberships = membershipRepository.findAll();
        return allMemberships.stream()
                .mapToDouble(Membership::getPrice)
                .sum();
    }

    /**
     * Get total count of active membership types
     */
    public long getActiveMembershipTypeCount() {
        List<MembershipType> activeMembershipTypes = membershipTypeRepository.findByActiveTrueOrderByDisplayOrderAsc();
        return activeMembershipTypes.size();
    }
}
