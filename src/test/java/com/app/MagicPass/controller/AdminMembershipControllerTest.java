package com.app.MagicPass.controller;

import com.app.MagicPass.model.MembershipType;
import com.app.MagicPass.service.MembershipTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AdminMembershipController
 * Tests CRUD operations for membership types
 */
@ExtendWith(MockitoExtension.class)
class AdminMembershipControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MembershipTypeService membershipTypeService;

    @InjectMocks
    private AdminMembershipController adminMembershipController;

    private MembershipType silverType;
    private MembershipType goldType;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminMembershipController).build();

        silverType = new MembershipType();
        silverType.setId(1L);
        silverType.setName("Silver");
        silverType.setDisplayName("Silver Membership");
        silverType.setPrice(99.99);
        silverType.setDiscountRate(0.05);
        silverType.setActive(true);

        goldType = new MembershipType();
        goldType.setId(2L);
        goldType.setName("Gold");
        goldType.setDisplayName("Gold Membership");
        goldType.setPrice(199.99);
        goldType.setDiscountRate(0.10);
        silverType.setActive(true);
    }

    @Test
    void testListMembershipTypes_ShouldDisplayAllTypes() throws Exception {
        // Given
        List<MembershipType> types = Arrays.asList(silverType, goldType);
        when(membershipTypeService.getAllMembershipTypes()).thenReturn(types);

        // When & Then
        mockMvc.perform(get("/admin/membership-types"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/membership-types"))
                .andExpect(model().attribute("membershipTypes", types));

        verify(membershipTypeService, times(1)).getAllMembershipTypes();
    }

    @Test
    void testShowCreateForm_ShouldDisplayForm() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/membership-types/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/membership-type-form"))
                .andExpect(model().attributeExists("membershipType"))
                .andExpect(model().attribute("mode", "create"));
    }

    @Test
    void testCreateMembershipType_Success_ShouldRedirect() throws Exception {
        // Given
        when(membershipTypeService.createMembershipType(any(MembershipType.class)))
                .thenReturn(silverType);

        // When & Then
        mockMvc.perform(post("/admin/membership-types/create")
                        .param("name", "Silver")
                        .param("displayName", "Silver Membership")
                        .param("price", "99.99")
                        .param("discountRate", "0.05")
                        .param("durationMonths", "12")
                        .param("active", "true")
                        .param("displayOrder", "1")
                        .param("tierLevel", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("success"));

        verify(membershipTypeService, times(1)).createMembershipType(any(MembershipType.class));
    }

    @Test
    void testCreateMembershipType_Error_ShouldRedirectWithError() throws Exception {
        // Given
        when(membershipTypeService.createMembershipType(any(MembershipType.class)))
                .thenThrow(new IllegalArgumentException("Duplicate name"));

        // When & Then
        mockMvc.perform(post("/admin/membership-types/create")
                        .param("name", "Silver")
                        .param("displayName", "Silver Membership")
                        .param("price", "99.99")
                        .param("discountRate", "0.05"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types/create"))
                .andExpect(flash().attributeExists("error"));

        verify(membershipTypeService, times(1)).createMembershipType(any(MembershipType.class));
    }

    @Test
    void testShowEditForm_Found_ShouldDisplayForm() throws Exception {
        // Given
        Long id = 1L;
        when(membershipTypeService.getMembershipTypeById(id)).thenReturn(silverType);

        // When & Then
        mockMvc.perform(get("/admin/membership-types/edit/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/membership-type-form"))
                .andExpect(model().attribute("membershipType", silverType))
                .andExpect(model().attribute("mode", "edit"));

        verify(membershipTypeService, times(1)).getMembershipTypeById(id);
    }

    @Test
    void testShowEditForm_NotFound_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 999L;
        when(membershipTypeService.getMembershipTypeById(id))
                .thenThrow(new RuntimeException("Not found"));

        // When & Then
        mockMvc.perform(get("/admin/membership-types/edit/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("error"));

        verify(membershipTypeService, times(1)).getMembershipTypeById(id);
    }

    @Test
    void testUpdateMembershipType_Success_ShouldRedirect() throws Exception {
        // Given
        Long id = 1L;
        when(membershipTypeService.updateMembershipType(eq(id), any(MembershipType.class)))
                .thenReturn(silverType);

        // When & Then
        mockMvc.perform(post("/admin/membership-types/edit/{id}", id)
                        .param("name", "Silver")
                        .param("displayName", "Updated Silver")
                        .param("price", "109.99")
                        .param("discountRate", "0.06"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("success"));

        verify(membershipTypeService, times(1)).updateMembershipType(eq(id), any(MembershipType.class));
    }

    @Test
    void testUpdateMembershipType_Error_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 1L;
        when(membershipTypeService.updateMembershipType(eq(id), any(MembershipType.class)))
                .thenThrow(new IllegalArgumentException("Duplicate name"));

        // When & Then
        mockMvc.perform(post("/admin/membership-types/edit/{id}", id)
                        .param("name", "Silver")
                        .param("displayName", "Updated Silver"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types/edit/" + id))
                .andExpect(flash().attributeExists("error"));

        verify(membershipTypeService, times(1)).updateMembershipType(eq(id), any(MembershipType.class));
    }

    @Test
    void testDeleteMembershipType_Success_ShouldRedirect() throws Exception {
        // Given
        Long id = 1L;
        doNothing().when(membershipTypeService).deleteMembershipType(id);

        // When & Then
        mockMvc.perform(post("/admin/membership-types/delete/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("success"));

        verify(membershipTypeService, times(1)).deleteMembershipType(id);
    }

    @Test
    void testDeleteMembershipType_Error_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 1L;
        doThrow(new RuntimeException("Cannot delete")).when(membershipTypeService).deleteMembershipType(id);

        // When & Then
        mockMvc.perform(post("/admin/membership-types/delete/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("error"));

        verify(membershipTypeService, times(1)).deleteMembershipType(id);
    }

    @Test
    void testToggleActiveStatus_Success_ShouldRedirect() throws Exception {
        // Given
        Long id = 1L;
        MembershipType updatedType = new MembershipType();
        updatedType.setId(id);
        updatedType.setActive(false);

        when(membershipTypeService.toggleActiveStatus(id)).thenReturn(updatedType);

        // When & Then
        mockMvc.perform(post("/admin/membership-types/toggle/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("success"));

        verify(membershipTypeService, times(1)).toggleActiveStatus(id);
    }

    @Test
    void testToggleActiveStatus_Error_ShouldRedirectWithError() throws Exception {
        // Given
        Long id = 1L;
        when(membershipTypeService.toggleActiveStatus(id))
                .thenThrow(new RuntimeException("Cannot toggle"));

        // When & Then
        mockMvc.perform(post("/admin/membership-types/toggle/{id}", id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/membership-types"))
                .andExpect(flash().attributeExists("error"));

        verify(membershipTypeService, times(1)).toggleActiveStatus(id);
    }
}
