package com.app.MagicPass.controller;

import com.app.MagicPass.model.Order;
import com.app.MagicPass.repository.OrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/tickets")
public class AdminTicketController {

    private final OrderRepository orderRepository;
    private final com.app.MagicPass.repository.UserRepository userRepository;

    public AdminTicketController(OrderRepository orderRepository, com.app.MagicPass.repository.UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
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
    public String createTicket(@ModelAttribute Order ticket, RedirectAttributes ra) {
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
