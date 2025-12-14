package com.app.MagicPass.controller;

import com.app.MagicPass.model.Order;
import com.app.MagicPass.repository.OrderRepository;
import com.app.MagicPass.service.DatePolicyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/tickets")
public class AdminTicketController {

    private final OrderRepository orderRepository;
    private final com.app.MagicPass.repository.UserRepository userRepository;
    private final DatePolicyService datePolicyService;

    public AdminTicketController(OrderRepository orderRepository,
                                  com.app.MagicPass.repository.UserRepository userRepository,
                                  DatePolicyService datePolicyService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.datePolicyService = datePolicyService;
    }

    @GetMapping
    public String listTickets(Model model) {
        model.addAttribute("tickets", orderRepository.findAll());
        return "admin/tickets";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("ticket", new Order());
        model.addAttribute("mode", "create");
        model.addAttribute("users", userRepository.findAll());
        return "admin/ticket-form";
    }

    @PostMapping("/create")
    public String createTicket(@ModelAttribute Order ticket, RedirectAttributes ra, Model model) {
        // Validate reservation date
        if (ticket.getReservationDate() == null) {
            ra.addFlashAttribute("error", "Please select a reservation date.");
            ra.addFlashAttribute("ticket", ticket);
            return "redirect:/admin/tickets/create";
        }

        if (!datePolicyService.isWithin7Days(ticket.getReservationDate())) {
            ra.addFlashAttribute("error", "Invalid reservation date. Please choose a date from today within the next 7 days.");
            ra.addFlashAttribute("ticket", ticket);
            return "redirect:/admin/tickets/create";
        }

        // Validate at least one ticket is selected
        int totalQty = ticket.getAdultQty() + ticket.getStudentQty() + ticket.getChildQty();
        if (totalQty <= 0) {
            ra.addFlashAttribute("error", "Please select at least 1 ticket.");
            ra.addFlashAttribute("ticket", ticket);
            return "redirect:/admin/tickets/create";
        }

        try {
            orderRepository.save(ticket);
            ra.addFlashAttribute("success", "Ticket (order) created successfully!");
            return "redirect:/admin/tickets";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error creating ticket: " + e.getMessage());
            return "redirect:/admin/tickets/create";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return orderRepository.findById(id)
                .map(order -> {
                    model.addAttribute("ticket", order);
                    model.addAttribute("mode", "edit");
                    model.addAttribute("users", userRepository.findAll());
                    return "admin/ticket-form";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("error", "Ticket (order) not found!");
                    return "redirect:/admin/tickets";
                });
    }

    @PostMapping("/edit/{id}")
    public String updateTicket(@PathVariable Long id, @ModelAttribute Order ticket, RedirectAttributes ra) {
        // Validate reservation date
        if (ticket.getReservationDate() == null) {
            ra.addFlashAttribute("error", "Please select a reservation date.");
            return "redirect:/admin/tickets/edit/" + id;
        }

        if (!datePolicyService.isWithin7Days(ticket.getReservationDate())) {
            ra.addFlashAttribute("error", "Invalid reservation date. Please choose a date from today within the next 7 days.");
            return "redirect:/admin/tickets/edit/" + id;
        }

        // Validate at least one ticket is selected
        int totalQty = ticket.getAdultQty() + ticket.getStudentQty() + ticket.getChildQty();
        if (totalQty <= 0) {
            ra.addFlashAttribute("error", "Please select at least 1 ticket.");
            return "redirect:/admin/tickets/edit/" + id;
        }

        try {
            return orderRepository.findById(id)
                    .map(existing -> {
                        existing.setOrderCode(ticket.getOrderCode());
                        existing.setUserId(ticket.getUserId());
                        existing.setAdultQty(ticket.getAdultQty());
                        existing.setStudentQty(ticket.getStudentQty());
                        existing.setChildQty(ticket.getChildQty());
                        existing.setReservationDate(ticket.getReservationDate());
                        existing.setTotal(ticket.getTotal());
                        existing.setDiscount(ticket.getDiscount());
                        existing.setTax(ticket.getTax());
                        existing.setGrandTotal(ticket.getGrandTotal());
                        existing.setPaymentMethod(ticket.getPaymentMethod());
                        existing.setStatus(ticket.getStatus());
                        orderRepository.save(existing);
                        ra.addFlashAttribute("success", "Ticket (order) updated successfully!");
                        return "redirect:/admin/tickets";
                    })
                    .orElseGet(() -> {
                        ra.addFlashAttribute("error", "Ticket (order) not found!");
                        return "redirect:/admin/tickets";
                    });
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error updating ticket: " + e.getMessage());
            return "redirect:/admin/tickets/edit/" + id;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteTicket(@PathVariable Long id, RedirectAttributes ra) {
        try {
            orderRepository.deleteById(id);
            ra.addFlashAttribute("success", "Ticket (order) deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting ticket: " + e.getMessage());
        }
        return "redirect:/admin/tickets";
    }
}
