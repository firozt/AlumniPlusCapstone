package com.alumniplus.order_service.controller;

import com.alumniplus.order_service.config.SecurityConfig;
import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.dto.OrderSummaryResponse;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.model.Side;
import com.alumniplus.order_service.service.OrderService;
import com.alumniplus.order_service.service.OrderSubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
    private static final String CANCEL_ORDER_URL = "/api/orders/cancel/";
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

    // traders are allowed to view their own orders
    @Test
    void traderCanViewOwnOrders() throws Exception {
        String owner = "team-1-trader1";
        Instant now = Instant.parse("2026-11-19T12:00:00Z");
        OrderResponse order = new OrderResponse(10L, owner, "AAPL",
                Side.BUY, 10L, new BigDecimal("185.50"), OrderStatus.SUBMITTED,
                now, now, 0L);
        when(orderService.getOrdersForOwner(owner)).thenReturn(List.of(order));

        mockMvc.perform(get(VIEW_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", owner))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(10))
                .andExpect(jsonPath("$[0].owner").value(owner));

        verify(orderService).getOrdersForOwner(owner);
        verify(orderService, never()).getAllOrders();
        verifyNoInteractions(orderSubmissionService);
    }

    // ops cannot view any orders vis the /view endpoint
    @Test
    void operationsCannotViewTraderOrders() throws Exception {
        mockMvc.perform(get(VIEW_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "team-1-ops1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService, orderSubmissionService);
    }

    @Test
    void traderSummaryUsesTokenOwner() throws Exception {
        when(orderService.getSummaryForOwner("team-1-trader1"))
                .thenReturn(new OrderSummaryResponse(2L, new BigDecimal("25.50")));

        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().jwt(token -> token.claim("preferred_username", "team-1-trader1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(2))
                .andExpect(jsonPath("$.totalRequestedValue").value(25.50));
        verify(orderService).getSummaryForOwner("team-1-trader1");
        verify(orderService, never()).getOverallSummary();
    }

    @Test
    void operationsSummaryUsesOverallTotals() throws Exception {
        when(orderService.getOverallSummary())
                .thenReturn(new OrderSummaryResponse(5L, new BigDecimal("125.75")));

        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(5))
                .andExpect(jsonPath("$.totalRequestedValue").value(125.75));
        verify(orderService).getOverallSummary();
        verify(orderService, never()).getSummaryForOwner(anyString());
    }

    @Test
    void operationsRoleTakesPrecedenceForSummary() throws Exception {
        when(orderService.getOverallSummary())
                .thenReturn(new OrderSummaryResponse(0L, BigDecimal.ZERO));
        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"),
                                new SimpleGrantedAuthority("ROLE_TRADER"))))
                .andExpect(status().isOk());
        verify(orderService).getOverallSummary();
        verify(orderService, never()).getSummaryForOwner(anyString());
    }

    @Test
    void summaryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/orders/summary"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }

    @Test
    void summaryRejectsUnrelatedRole() throws Exception {
        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OTHER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orderService);
    }

    @Test
    void traderSummaryRequiresUsername() throws Exception {
        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }

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

    // trader can canel their own order
    @Test
    void traderCancelsOrder() throws Exception {
        // mocked service returns the result of cancellation.
        Long orderId = 10L;
        String owner = "team-1-trader1";
        Instant now = Instant.parse("2026-11-19T12:00:00Z");
        OrderResponse cancelledOrder = new OrderResponse(orderId, owner, "AAPL",
                Side.BUY, orderId, new BigDecimal("185.50"), OrderStatus.CANCELLED,
                now, now, 1L);
        when(orderService.cancelOrder(orderId, owner)).thenReturn(cancelledOrder);

        // Act and assert: the controller returns the cancelled order as JSON.
        mockMvc.perform(post(CANCEL_ORDER_URL + orderId)
                .with(jwt()
                        .jwt(token -> token.claim("preferred_username", owner))
                        .authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(orderId.intValue()))
                .andExpect(jsonPath("$.owner").value(owner))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelOrder(orderId, owner);
        verifyNoInteractions(orderSubmissionService);
    }


    // trader cant cancel another order 
    @Test
    void traderAttempsToCancelAnotherOrder() throws Exception {
        Long orderId = 10L;
        String otherTrader = "team-1-trader2";
        // Ownership is checked by the service; simulate its rejection here.
        when(orderService.cancelOrder(orderId, otherTrader))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You cannot cancel another user's order"));

        mockMvc.perform(post(CANCEL_ORDER_URL + orderId)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", otherTrader))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                .andExpect(status().isForbidden());

        verify(orderService).cancelOrder(orderId, otherTrader);
        verifyNoInteractions(orderSubmissionService);
    }

    // ops cant cancel order alltogether
    @Test
    void opsAttemptsToCancel() throws Exception {
        mockMvc.perform(post(CANCEL_ORDER_URL + "10")
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "team-1-ops1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService, orderSubmissionService);
    }
}
