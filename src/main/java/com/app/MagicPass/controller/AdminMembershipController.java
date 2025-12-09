package com.app.MagicPass.controller;

import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.service.MembershipTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/membership-types")
public class AdminMembershipController {

    private final MembershipTypeService membershipTypeService;

    public AdminMembershipController(MembershipTypeService membershipTypeService) {
        this.membershipTypeService = membershipTypeService;
    }

    /**
     * List all membership types
     */
    @GetMapping
    public String listMembershipTypes(Model model) {
        model.addAttribute("membershipTypes", membershipTypeService.getAllMembershipTypes());
        return "admin/membership-types";
    }

    /**
     * Show create form
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("membershipType", new MembershipType());
        model.addAttribute("mode", "create");
        return "admin/membership-type-form";
    }

    /**
     * Handle create submission
     */
    @PostMapping("/create")
    public String createMembershipType(@ModelAttribute MembershipType membershipType,
                                      RedirectAttributes ra) {
        try {
            membershipTypeService.createMembershipType(membershipType);
            ra.addFlashAttribute("success", "Membership type created successfully!");
            return "redirect:/admin/membership-types";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error creating membership type: " + e.getMessage());
            return "redirect:/admin/membership-types/create";
        }
    }

    /**
     * Show edit form
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            MembershipType membershipType = membershipTypeService.getMembershipTypeById(id);
            model.addAttribute("membershipType", membershipType);
            model.addAttribute("mode", "edit");
            return "admin/membership-type-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Membership type not found!");
            return "redirect:/admin/membership-types";
        }
    }

    /**
     * Handle update submission
     */
    @PostMapping("/edit/{id}")
    public String updateMembershipType(@PathVariable Long id,
                                      @ModelAttribute MembershipType membershipType,
                                      RedirectAttributes ra) {
        try {
            membershipTypeService.updateMembershipType(id, membershipType);
            ra.addFlashAttribute("success", "Membership type updated successfully!");
            return "redirect:/admin/membership-types";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error updating membership type: " + e.getMessage());
            return "redirect:/admin/membership-types/edit/" + id;
        }
    }

    /**
     * Delete membership type
     */
    @PostMapping("/delete/{id}")
    public String deleteMembershipType(@PathVariable Long id, RedirectAttributes ra) {
        try {
            membershipTypeService.deleteMembershipType(id);
            ra.addFlashAttribute("success", "Membership type deleted successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting membership type: " + e.getMessage());
        }
        return "redirect:/admin/membership-types";
    }

    /**
     * Toggle active status
     */
    @PostMapping("/toggle/{id}")
    public String toggleActiveStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            membershipTypeService.toggleActiveStatus(id);
            ra.addFlashAttribute("success", "Status toggled successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error toggling status: " + e.getMessage());
        }
        return "redirect:/admin/membership-types";
    }
}
