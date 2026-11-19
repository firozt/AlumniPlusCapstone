package com.alumniplus.order_service.controller;

import com.alumniplus.order_service.config.SecurityConfig;
import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.model.Side;
import com.alumniplus.order_service.service.OrderService;
import com.alumniplus.order_service.service.OrderSubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests HTTP handling and access rules without Oracle, Kafka, or Keycloak.
// Mock JWT authorities are supplied directly; this does not test role conversion.
@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    private static final String SUBMIT_URL = "/api/orders/submit";
    private static final String VIEW_URL = "/api/orders/view";
    private static final String VIEW_ALL_URL = "/api/orders/view-all";
    private static final String VALID_REQUEST = """
            {"instrumentId":"AAPL","side":"BUY","quantity":10,"limitPrice":185.50}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderSubmissionService orderSubmissionService;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // ops cannot submit order
    @Test
    void operationsCannotSubmitOrder() throws Exception {
        mockMvc.perform(post(SUBMIT_URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS")))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orderSubmissionService);
    }

    // trader can submit order
    @Test
    void traderCanSubmitOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "AAPL", Side.BUY, 10L, new BigDecimal("185.50"));
        Instant now = Instant.parse("2026-11-19T12:00:00Z");
        OrderResponse response = new OrderResponse(42L, "team-1-trader1", "AAPL",
                Side.BUY, 10L, new BigDecimal("185.50"), OrderStatus.SUBMITTED,
                now, now, 0L);
        when(orderSubmissionService.submitOrder(request, "team-1-trader1"))
                .thenReturn(response);

        mockMvc.perform(post(SUBMIT_URL)
                .with(jwt()
                        .jwt(token -> token.claim("preferred_username", "team-1-trader1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_TRADER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.owner").value("team-1-trader1"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        verify(orderSubmissionService).submitOrder(request, "team-1-trader1");
    }

    // no token results in error (submit order)
    @Test
    void submitWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SUBMIT_URL)
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderSubmissionService);
    }

    // negative quantity results in bad request (submit order)
    @Test
    void negativeQuantityReturnsBadRequest() throws Exception {
        mockMvc.perform(post(SUBMIT_URL)
                .with(jwt()
                        .jwt(token -> token.claim("preferred_username", "team-1-trader1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_TRADER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"instrumentId":"AAPL","side":"BUY","quantity":-1,"limitPrice":185.50}
                        """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(orderSubmissionService);
    }

    // no username (preferred_username) results in unauthorized
    @Test
    void missingUsernameReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(SUBMIT_URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRADER")))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderSubmissionService);
    }

    // trader attempting an ops endpoint (view all) results in 401 (forbidden)
    @Test
    void traderCannotViewAll() throws Exception {
        mockMvc.perform(get(VIEW_ALL_URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRADER")))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orderSubmissionService);
    }
}
