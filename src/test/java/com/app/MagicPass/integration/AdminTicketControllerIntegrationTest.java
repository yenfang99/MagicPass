package com.app.MagicPass.integration;

import com.app.MagicPass.model.Order;
import com.app.MagicPass.model.User;
import com.app.MagicPass.repository.OrderRepository;
import com.app.MagicPass.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import org.springframework.mock.web.MockHttpSession;

/**
 * Integration tests for AdminTicketController
 * Tests the full stack with real database (H2 in-memory)
 *
 * Annotations explained:
 * @SpringBootTest - Loads full Spring application context
 * @ActiveProfiles("test") - Uses application-test.properties
 * @Transactional - Auto-rollback database changes after each test
 *
 * Note: For Spring Boot 4.0, MockMvc is manually configured in @BeforeEach
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminTicketControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private MockHttpSession mockSession;

    @BeforeEach
    void setUp() {
        // Setup MockMvc
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .build();

        // Clean up before each test (belt and suspenders with @Transactional)
        orderRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setBirthday(LocalDate.of(1990, 1, 1));
        testUser.setAge(34);
        testUser.setGender("M");
        testUser.setPhone("0123456789");
        testUser = userRepository.save(testUser);

        // Create mock session with authenticated staff user (for admin routes)
        mockSession = new MockHttpSession();
        mockSession.setAttribute("currentStaff", testUser);
    }

    @Test
    void testListTickets_ShouldReturnAllTickets() throws Exception {
        // Given - create test orders
        Order order1 = new Order();
        order1.setOrderCode("OD0001");
        order1.setUserId(testUser.getId());
        order1.setAdultQty(2);
        order1.setStudentQty(1);
        order1.setChildQty(0);
        order1.setReservationDate(LocalDate.now().plusDays(1));
        order1.setTotal(1060.0);
        order1.setTax(63.6);
        order1.setGrandTotal(1123.6);
        order1.setPaymentMethod("CARD");
        order1.setStatus("PAID");
        orderRepository.save(order1);

        Order order2 = new Order();
        order2.setOrderCode("OD0002");
        order2.setUserId(testUser.getId());
        order2.setAdultQty(1);
        order2.setStudentQty(0);
        order2.setChildQty(1);
        order2.setReservationDate(LocalDate.now().plusDays(2));
        order2.setTotal(620.0);
        order2.setTax(37.2);
        order2.setGrandTotal(657.2);
        order2.setPaymentMethod("CASH");
        order2.setStatus("PREVIEW");
        orderRepository.save(order2);

        // When & Then
        mockMvc.perform(get("/admin/tickets").session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tickets"))
                .andExpect(model().attributeExists("tickets"))
                .andExpect(model().attribute("tickets", hasSize(2)));
    }

    @Test
    void testCreateTicket_ValidData_ShouldSaveToDatabase() throws Exception {
        // Given
        LocalDate validDate = LocalDate.now().plusDays(3);

        // When
        mockMvc.perform(post("/admin/tickets/create").session(mockSession)
                        .param("orderCode", "OD0003")
                        .param("userId", testUser.getId().toString())
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

        // Then - verify saved to database
        assertEquals(1, orderRepository.count(), "Order should be saved to database");
        Order savedOrder = orderRepository.findAll().get(0);
        assertEquals("OD0003", savedOrder.getOrderCode());
        assertEquals(testUser.getId(), savedOrder.getUserId());
        assertEquals(3, savedOrder.getAdultQty());
        assertEquals(2, savedOrder.getStudentQty());
        assertEquals(1, savedOrder.getChildQty());
        assertEquals("FPX", savedOrder.getPaymentMethod());
        assertEquals("PAID", savedOrder.getStatus());
    }

    @Test
    void testCreateTicket_InvalidDate_ShouldRejectWithError() throws Exception {
        // Given - date outside 7-day window
        LocalDate invalidDate = LocalDate.now().plusDays(10);

        // When & Then
        mockMvc.perform(post("/admin/tickets/create").session(mockSession)
                        .param("orderCode", "OD0004")
                        .param("userId", testUser.getId().toString())
                        .param("adultQty", "1")
                        .param("studentQty", "0")
                        .param("childQty", "0")
                        .param("reservationDate", invalidDate.toString())
                        .param("total", "370.0")
                        .param("tax", "22.2")
                        .param("grandTotal", "392.2")
                        .param("paymentMethod", "CARD")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets/create"))
                .andExpect(flash().attributeExists("error"));

        // Then - verify NOT saved to database
        assertEquals(0, orderRepository.count(), "Invalid order should not be saved");
    }

    @Test
    void testCreateTicket_NoTickets_ShouldRejectWithError() throws Exception {
        // Given - all quantities are zero
        LocalDate validDate = LocalDate.now().plusDays(2);

        // When & Then
        mockMvc.perform(post("/admin/tickets/create").session(mockSession)
                        .param("orderCode", "OD0005")
                        .param("userId", testUser.getId().toString())
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

        // Then - verify NOT saved to database
        assertEquals(0, orderRepository.count(), "Order with no tickets should not be saved");
    }

    @Test
    void testUpdateTicket_ExistingOrder_ShouldUpdateInDatabase() throws Exception {
        // Given - create initial order
        Order originalOrder = new Order();
        originalOrder.setOrderCode("OD0006");
        originalOrder.setUserId(testUser.getId());
        originalOrder.setAdultQty(1);
        originalOrder.setStudentQty(0);
        originalOrder.setChildQty(0);
        originalOrder.setReservationDate(LocalDate.now().plusDays(1));
        originalOrder.setTotal(370.0);
        originalOrder.setTax(22.2);
        originalOrder.setGrandTotal(392.2);
        originalOrder.setPaymentMethod("CASH");
        originalOrder.setStatus("PREVIEW");
        originalOrder = orderRepository.save(originalOrder);

        LocalDate newDate = LocalDate.now().plusDays(4);
        Long orderId = originalOrder.getId();

        // When - update the order
        mockMvc.perform(post("/admin/tickets/edit/" + orderId).session(mockSession)
                        .param("orderCode", "OD0006")
                        .param("userId", testUser.getId().toString())
                        .param("adultQty", "2")
                        .param("studentQty", "1")
                        .param("childQty", "1")
                        .param("reservationDate", newDate.toString())
                        .param("total", "1180.0")
                        .param("tax", "70.8")
                        .param("grandTotal", "1250.8")
                        .param("paymentMethod", "CARD")
                        .param("status", "PAID"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"))
                .andExpect(flash().attributeExists("success"));

        // Then - verify updated in database
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
        assertEquals(2, updatedOrder.getAdultQty());
        assertEquals(1, updatedOrder.getStudentQty());
        assertEquals(1, updatedOrder.getChildQty());
        assertEquals("CARD", updatedOrder.getPaymentMethod());
        assertEquals("PAID", updatedOrder.getStatus());
    }

    @Test
    void testDeleteTicket_ExistingOrder_ShouldRemoveFromDatabase() throws Exception {
        // Given - create order
        Order order = new Order();
        order.setOrderCode("OD0007");
        order.setUserId(testUser.getId());
        order.setAdultQty(1);
        order.setStudentQty(0);
        order.setChildQty(0);
        order.setReservationDate(LocalDate.now().plusDays(1));
        order.setTotal(370.0);
        order.setTax(22.2);
        order.setGrandTotal(392.2);
        order.setPaymentMethod("CASH");
        order.setStatus("PREVIEW");
        order = orderRepository.save(order);

        Long orderId = order.getId();

        // When
        mockMvc.perform(post("/admin/tickets/delete/" + orderId).session(mockSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"))
                .andExpect(flash().attributeExists("success"));

        // Then - verify deleted from database
        assertTrue(orderRepository.findById(orderId).isEmpty(), "Order should be deleted");
        assertEquals(0, orderRepository.count());
    }

    @Test
    void testShowEditForm_ExistingOrder_ShouldDisplayForm() throws Exception {
        // Given
        Order order = new Order();
        order.setOrderCode("OD0008");
        order.setUserId(testUser.getId());
        order.setAdultQty(1);
        order.setStudentQty(0);
        order.setChildQty(0);
        order.setReservationDate(LocalDate.now().plusDays(1));
        order.setTotal(370.0);
        order.setTax(22.2);
        order.setGrandTotal(392.2);
        order.setPaymentMethod("CASH");
        order.setStatus("PREVIEW");
        order = orderRepository.save(order);

        // When & Then
        mockMvc.perform(get("/admin/tickets/edit/" + order.getId()).session(mockSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/ticket-form"))
                .andExpect(model().attributeExists("ticket"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("mode", "edit"));
    }

    @Test
    void testShowEditForm_NonExistentOrder_ShouldRedirectWithError() throws Exception {
        // Given - non-existent ID
        Long nonExistentId = 99999L;

        // When & Then
        mockMvc.perform(get("/admin/tickets/edit/" + nonExistentId).session(mockSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tickets"))
                .andExpect(flash().attributeExists("error"));
    }
}
