package com.app.MagicPass.controller;

import com.app.MagicPass.model.Order;
import com.app.MagicPass.model.User;
import com.app.MagicPass.repository.OrderRepository;
import com.app.MagicPass.repository.UserRepository;
import com.app.MagicPass.service.DatePolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminTicketController
 * Tests CRUD operations for ticket orders with date validation
 */
@ExtendWith(MockitoExtension.class)
class AdminTicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DatePolicyService datePolicyService;

    @InjectMocks
    private AdminTicketController adminTicketController;

    private Order ticket1;
    private Order ticket2;
    private User user1;
    private User user2;
    private List<User> userList;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminTicketController).build();

        // Setup users
        user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@test.com");
        user1.setPasswordHash("password123");

        user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@test.com");
        user2.setPasswordHash("password456");

        userList = Arrays.asList(user1, user2);

        // Setup tickets
        ticket1 = new Order();
        // Note: Order.id is auto-generated, we don't set it manually
        ticket1.setOrderCode("OD0001");
        ticket1.setUserId(1L);
        ticket1.setAdultQty(2);
        ticket1.setStudentQty(1);
        ticket1.setChildQty(1);
        ticket1.setReservationDate(LocalDate.now().plusDays(1));
        ticket1.setTotal(1180.0);
        ticket1.setTax(70.8);
        ticket1.setGrandTotal(1250.8);
        ticket1.setPaymentMethod("CARD");
        ticket1.setStatus("PAID");

        ticket2 = new Order();
        ticket2.setOrderCode("OD0002");
        ticket2.setUserId(2L);
        ticket2.setAdultQty(1);
        ticket2.setStudentQty(0);
        ticket2.setChildQty(2);
        ticket2.setReservationDate(LocalDate.now().plusDays(3));
        ticket2.setTotal(870.0);
        ticket2.setTax(52.2);
        ticket2.setGrandTotal(922.2);
        ticket2.setPaymentMethod("CASH");
        ticket2.setStatus("PREVIEW");
    }

    @Test
    void testListTickets_ShouldDisplayAllTickets() throws Exception {
        // Given
        List<Order> tickets = Arrays.asList(ticket1, ticket2);
        when(orderRepository.findAll()).thenReturn(tickets);

        // When & Then
        mockMvc.perform(get("/admin/tickets"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tickets"))
                .andExpect(model().attribute("tickets", tickets));

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testShowCreateForm_ShouldDisplayForm() throws Exception {
        // Given
        when(userRepository.findAll()).thenReturn(userList);

        // When & Then
        mockMvc.perform(get("/admin/tickets/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ticket-form"))
                .andExpect(model().attributeExists("ticket"))
                .andExpect(model().attribute("mode", "create"))
                .andExpect(model().attribute("users", userList));

        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testCreateTicket_Success_ShouldRedirect() throws Exception {
        // Given
        LocalDate validDate = LocalDate.now().plusDays(2);
        when(datePolicyService.isWithin7Days(validDate)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(ticket1);

        // When & Then
        mockMvc.perform(post("/admin/tickets/create")
                        .param("orderCode", "OD0001")
                        .param("userId", "1")
                        .param("adultQty", "2")
                        .param("studentQty", "1")
                        .param("childQty", "1")
                        .param("reservationDate", validDate.toString())
                        .param("total", "1180.0")
                        .param("tax", "70.8")
                        .param("grandTotal", "1250.8")
                        .param("paymentMethod", "CARD")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"))
                .andExpect(flash().attributeExists("success"));

        verify(datePolicyService, times(1)).isWithin7Days(validDate);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateTicket_NullDate_ShouldRedirectWithError() throws Exception {
        // When & Then
        mockMvc.perform(post("/admin/tickets/create")
                        .param("orderCode", "OD0001")
                        .param("userId", "1")
                        .param("adultQty", "2")
                        .param("studentQty", "1")
                        .param("childQty", "1")
                        .param("total", "1180.0")
                        .param("tax", "70.8")
                        .param("grandTotal", "1250.8")
                        .param("paymentMethod", "CARD")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets/create"))
                .andExpect(flash().attributeExists("error"));

        verify(datePolicyService, never()).isWithin7Days(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateTicket_InvalidDate_ShouldRedirectWithError() throws Exception {
        // Given
        LocalDate invalidDate = LocalDate.now().plusDays(10);
        when(datePolicyService.isWithin7Days(invalidDate)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/admin/tickets/create")
                        .param("orderCode", "OD0001")
                        .param("userId", "1")
                        .param("adultQty", "2")
                        .param("studentQty", "1")
                        .param("childQty", "1")
                        .param("reservationDate", invalidDate.toString())
                        .param("total", "1180.0")
                        .param("tax", "70.8")
                        .param("grandTotal", "1250.8")
                        .param("paymentMethod", "CARD")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets/create"))
                .andExpect(flash().attributeExists("error"));

        verify(datePolicyService, times(1)).isWithin7Days(invalidDate);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateTicket_NoTickets_ShouldRedirectWithError() throws Exception {
        // Given
        LocalDate validDate = LocalDate.now().plusDays(2);
        when(datePolicyService.isWithin7Days(validDate)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/admin/tickets/create")
                        .param("orderCode", "OD0001")
                        .param("userId", "1")
                        .param("adultQty", "0")
                        .param("studentQty", "0")
                        .param("childQty", "0")
                        .param("reservationDate", validDate.toString())
                        .param("total", "0.0")
                        .param("tax", "0.0")
                        .param("grandTotal", "0.0")
                        .param("paymentMethod", "CASH")
                        .param("status", "PREVIEW"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets/create"))
                .andExpect(flash().attributeExists("error"));

        verify(datePolicyService, times(1)).isWithin7Days(validDate);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateTicket_Exception_ShouldRedirectWithError() throws Exception {
        // Given
        LocalDate validDate = LocalDate.now().plusDays(2);
        when(datePolicyService.isWithin7Days(validDate)).thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/admin/tickets/create")
                        .param("orderCode", "OD0001")
                        .param("userId", "1")
                        .param("adultQty", "2")
                        .param("studentQty", "1")
                        .param("childQty", "1")
                        .param("reservationDate", validDate.toString())
                        .param("total", "1180.0")
                        .param("tax", "70.8")
                        .param("grandTotal", "1250.8")
                        .param("paymentMethod", "CARD")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets/create"))
                .andExpect(flash().attributeExists("error"));

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testShowEditForm_Found_ShouldDisplayForm() throws Exception {
        // Given
        Long id = 1L;
        when(orderRepository.findById(id)).thenReturn(Optional.of(ticket1));
        when(userRepository.findAll()).thenReturn(userList);

        // When & Then
        mockMvc.perform(get("/admin/tickets/edit/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ticket-form"))
                .andExpect(model().attribute("ticket", ticket1))
                .andExpect(model().attribute("mode", "edit"))
                .andExpect(model().attribute("users", userList));

        verify(orderRepository, times(1)).findById(id);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testShowEditForm_NotFound_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 999L;
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/admin/tickets/edit/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"))
                .andExpect(flash().attributeExists("error"));

        verify(orderRepository, times(1)).findById(id);
        verify(userRepository, never()).findAll();
    }

    @Test
    void testUpdateTicket_Success_ShouldRedirect() throws Exception {
        // Given
        Long id = 1L;
        LocalDate validDate = LocalDate.now().plusDays(2);
        when(datePolicyService.isWithin7Days(validDate)).thenReturn(true);
        when(orderRepository.findById(id)).thenReturn(Optional.of(ticket1));
        when(orderRepository.save(any(Order.class))).thenReturn(ticket1);

        // When & Then
        mockMvc.perform(post("/admin/tickets/edit/{id}", id)
                        .param("orderCode", "OD0001")
                        .param("userId", "1")
                        .param("adultQty", "3")
                        .param("studentQty", "2")
                        .param("childQty", "1")
                        .param("reservationDate", validDate.toString())
                        .param("total", "1490.0")
                        .param("tax", "89.4")
                        .param("grandTotal", "1579.4")
                        .param("paymentMethod", "FPX")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"))
                .andExpect(flash().attributeExists("success"));

        verify(datePolicyService, times(1)).isWithin7Days(validDate);
        verify(orderRepository, times(1)).findById(id);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testUpdateTicket_InvalidDate_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 1L;
        LocalDate invalidDate = LocalDate.now().minusDays(1);
        when(datePolicyService.isWithin7Days(invalidDate)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/admin/tickets/edit/{id}", id)
                        .param("orderCode", "OD0001")
                        .param("userId", "1")
                        .param("adultQty", "2")
                        .param("studentQty", "1")
                        .param("childQty", "1")
                        .param("reservationDate", invalidDate.toString())
                        .param("total", "1180.0")
                        .param("tax", "70.8")
                        .param("grandTotal", "1250.8")
                        .param("paymentMethod", "CARD")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets/edit/" + id))
                .andExpect(flash().attributeExists("error"));

        verify(datePolicyService, times(1)).isWithin7Days(invalidDate);
        verify(orderRepository, never()).findById(id);
    }

    @Test
    void testUpdateTicket_NotFound_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 999L;
        LocalDate validDate = LocalDate.now().plusDays(2);
        when(datePolicyService.isWithin7Days(validDate)).thenReturn(true);
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/admin/tickets/edit/{id}", id)
                        .param("orderCode", "OD0001")
                        .param("userId", "1")
                        .param("adultQty", "2")
                        .param("studentQty", "1")
                        .param("childQty", "1")
                        .param("reservationDate", validDate.toString())
                        .param("total", "1180.0")
                        .param("tax", "70.8")
                        .param("grandTotal", "1250.8")
                        .param("paymentMethod", "CARD")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"))
                .andExpect(flash().attributeExists("error"));

        verify(orderRepository, times(1)).findById(id);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testUpdateTicket_Exception_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 1L;
        LocalDate validDate = LocalDate.now().plusDays(2);
        when(datePolicyService.isWithin7Days(validDate)).thenReturn(true);
        when(orderRepository.findById(id)).thenReturn(Optional.of(ticket1));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/admin/tickets/edit/{id}", id)
                        .param("orderCode", "OD0001")
                        .param("userId", "1")
                        .param("adultQty", "2")
                        .param("studentQty", "1")
                        .param("childQty", "1")
                        .param("reservationDate", validDate.toString())
                        .param("total", "1180.0")
                        .param("tax", "70.8")
                        .param("grandTotal", "1250.8")
                        .param("paymentMethod", "CARD")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets/edit/" + id))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void testDeleteTicket_Success_ShouldRedirect() throws Exception {
        // Given
        Long id = 1L;
        doNothing().when(orderRepository).deleteById(id);

        // When & Then
        mockMvc.perform(post("/admin/tickets/delete/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"))
                .andExpect(flash().attributeExists("success"));

        verify(orderRepository, times(1)).deleteById(id);
    }

    @Test
    void testDeleteTicket_Exception_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 1L;
        doThrow(new RuntimeException("Cannot delete")).when(orderRepository).deleteById(id);

        // When & Then
        mockMvc.perform(post("/admin/tickets/delete/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"))
                .andExpect(flash().attributeExists("error"));

        verify(orderRepository, times(1)).deleteById(id);
    }
}
