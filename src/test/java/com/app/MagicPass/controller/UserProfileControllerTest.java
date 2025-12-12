package com.app.MagicPass.controller;

import com.app.MagicPass.model.Membership;
import com.app.MagicPass.model.User;
import com.app.MagicPass.service.MembershipService;
import com.app.MagicPass.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.app.MagicPass.model.Order;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;



/**
 * Unit tests for UserProfileController
 * Tests session-based access + view rendering + model attributes
 */
@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    List<Order> orders = List.of(mock(Order.class), mock(Order.class));
    
    private MockMvc mockMvc;

    @Mock
    private MembershipService membershipService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private UserProfileController userProfileController;

    @BeforeEach
void setUp() {
    InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
    viewResolver.setPrefix("/templates/");
    viewResolver.setSuffix(".html");

    mockMvc = MockMvcBuilders.standaloneSetup(userProfileController)
            .setViewResolvers(viewResolver)
            .build();
}

    @Test
    void profile_whenNotLoggedIn_redirectToLogin() throws Exception {
        mockMvc.perform(get("/user/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(membershipService, orderService);
    }

    @Test
    void profile_whenLoggedIn_returnProfileView_withUserAndMembership() throws Exception {
        User user = new User();
        user.setId(1L);

        Membership membership = new Membership();
        when(membershipService.getActiveMembership(1L)).thenReturn(membership);

        mockMvc.perform(get("/user/profile").sessionAttr("currentUser", user))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("currentMembership", membership));

        verify(membershipService, times(1)).getActiveMembership(1L);
        verifyNoInteractions(orderService);
    }

    @Test
    void profile_whenLoggedInButNoMembership_currentMembershipIsNull() throws Exception {
        User user = new User();
        user.setId(2L);

        when(membershipService.getActiveMembership(2L)).thenReturn(null);

        mockMvc.perform(get("/user/profile").sessionAttr("currentUser", user))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("currentMembership", (Object) null));

        verify(membershipService, times(1)).getActiveMembership(2L);
        verifyNoInteractions(orderService);
    }

    @Test
    void orders_whenNotLoggedIn_redirectToLogin() throws Exception {
        mockMvc.perform(get("/user/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(membershipService, orderService);
    }

   @Test
void orders_whenLoggedIn_returnOrderHistory_withOrdersAndUser() throws Exception {
    User user = new User();
    user.setId(5L);

    List<Order> orders = List.of(new Order(), new Order());
    when(orderService.getOrdersForUser(5L)).thenReturn(orders);

    mockMvc.perform(get("/user/orders").sessionAttr("currentUser", user))
            .andExpect(status().isOk())
            .andExpect(view().name("order-history"))
            .andExpect(model().attribute("user", user))
            .andExpect(model().attribute("orders", orders));

    verify(orderService, times(1)).getOrdersForUser(5L);
    verifyNoInteractions(membershipService);
}

}
