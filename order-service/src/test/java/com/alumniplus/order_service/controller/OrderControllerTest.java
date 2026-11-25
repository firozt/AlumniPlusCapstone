package com.alumniplus.order_service.controller;

import com.alumniplus.order_service.config.SecurityConfig;
import com.alumniplus.order_service.dto.CreateOrderRequest;
import com.alumniplus.order_service.dto.OrderResponse;
import com.alumniplus.order_service.dto.OrderSummaryResponse;
import com.alumniplus.order_service.model.OrderStatus;
import com.alumniplus.order_service.model.Side;
import com.alumniplus.order_service.service.OrderService;
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
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // traders are allowed to view their own orders
    //happy path
    @Test
    void traderCanViewOwnOrders() throws Exception {
        // given -> a trader JWT and a mocked list of that owner's orders
        String owner = "team-1-trader1";
        Instant now = Instant.parse("2026-11-19T12:00:00Z");
        OrderResponse order = new OrderResponse(10L, owner, "AAPL",
                Side.BUY, 10L, new BigDecimal("185.50"), OrderStatus.SUBMITTED,
                now, now, 0L);
        when(orderService.getOrdersForOwner(owner)).thenReturn(List.of(order));

        // when -> the trader requests /view
        mockMvc.perform(get(VIEW_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", owner))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                // then -> return 200 with that owner's orders and use the token username
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].orderId").value(10))
                .andExpect(jsonPath("$[0].owner").value(owner));

        verify(orderService).getOrdersForOwner(owner);
        verify(orderService, never()).getAllOrders();
        verifyNoMoreInteractions(orderService);
    }

    // ops cannot view any orders vis the /view endpoint
    @Test
    void operationsCannotViewTraderOrders() throws Exception {
        // given -> a JWT with only OPERATIONS
        // when -> Operations requests /view
        mockMvc.perform(get(VIEW_URL)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "team-1-ops1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                // then -> return 403 without calling the service
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    //happy path
    @Test
    void traderSummaryUsesTokenOwner() throws Exception {
        // given -> a trader JWT and mocked owner totals
        when(orderService.getSummaryForOwner("team-1-trader1"))
                .thenReturn(new OrderSummaryResponse(2L, new BigDecimal("25.50")));

        // when -> the trader requests /summary
        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().jwt(token -> token.claim("preferred_username", "team-1-trader1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                // then -> return 200 with owner totals rather than overall totals
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(2))
                .andExpect(jsonPath("$.totalRequestedValue").value(25.50));
        verify(orderService).getSummaryForOwner("team-1-trader1");
        verify(orderService, never()).getOverallSummary();
    }

    //happy path
    @Test
    void operationsSummaryUsesOverallTotals() throws Exception {
        // given -> an Operations JWT and mocked overall totals
        when(orderService.getOverallSummary())
                .thenReturn(new OrderSummaryResponse(5L, new BigDecimal("125.75")));

        // when -> Operations requests /summary
        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                // then -> return 200 with overall totals rather than owner totals
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(5))
                .andExpect(jsonPath("$.totalRequestedValue").value(125.75));
        verify(orderService).getOverallSummary();
        verify(orderService, never()).getSummaryForOwner(anyString());
    }

    @Test
    void operationsRoleTakesPrecedenceForSummary() throws Exception {
        // given -> a JWT containing both TRADER and OPERATIONS
        when(orderService.getOverallSummary())
                .thenReturn(new OrderSummaryResponse(0L, BigDecimal.ZERO));
        // when -> the user requests /summary
        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"),
                                new SimpleGrantedAuthority("ROLE_TRADER"))))
                // then -> use overall totals because Operations takes precedence
                .andExpect(status().isOk());
        verify(orderService).getOverallSummary();
        verify(orderService, never()).getSummaryForOwner(anyString());
    }

    @Test
    void summaryRequiresAuthentication() throws Exception {
        // given -> a request without a bearer token
        // when -> /summary is requested
        mockMvc.perform(get("/api/orders/summary"))
                // then -> return 401 without calling the service
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }

    @Test
    void summaryRejectsUnrelatedRole() throws Exception {
        // given -> a JWT with an unrelated role
        // when -> /summary is requested
        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OTHER"))))
                // then -> return 403 without calling the service
                .andExpect(status().isForbidden());
        verifyNoInteractions(orderService);
    }

    @Test
    void traderSummaryRequiresUsername() throws Exception {
        // given -> a trader JWT without preferred_username
        // when -> the trader requests /summary
        mockMvc.perform(get("/api/orders/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                // then -> return 401 without calling the service
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(orderService);
    }

    // ops cannot submit order
    @Test
    void operationsCannotSubmitOrder() throws Exception {
        // given -> an Operations JWT and a valid order body
        // when -> Operations posts to /submit
        mockMvc.perform(post(SUBMIT_URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS")))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                // then -> return 403 without creating an order
                .andExpect(status().isForbidden());
        verifyNoMoreInteractions(orderService);
    }

    // trader can submit order
    //happy path
    @Test
    void traderCanSubmitOrder() throws Exception {
        // given -> a trader JWT, valid request, and mocked submitted order
        CreateOrderRequest request = new CreateOrderRequest(
                "AAPL", Side.BUY, 10L, new BigDecimal("185.50"));
        Instant now = Instant.parse("2026-11-19T12:00:00Z");
        OrderResponse response = new OrderResponse(42L, "team-1-trader1", "AAPL",
                Side.BUY, 10L, new BigDecimal("185.50"), OrderStatus.SUBMITTED,
                now, now, 0L);
        when(orderService.submitOrder(request, "team-1-trader1"))
                .thenReturn(response);

        // when -> the trader posts to /submit
        mockMvc.perform(post(SUBMIT_URL)
                .with(jwt()
                        .jwt(token -> token.claim("preferred_username", "team-1-trader1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_TRADER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_REQUEST))
                // then -> return 201 and pass the request and token username to the service
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(42))
                .andExpect(jsonPath("$.owner").value("team-1-trader1"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        verify(orderService).submitOrder(request, "team-1-trader1");
    }

    // no token results in error (submit order)
    @Test
    void submitWithoutTokenReturnsUnauthorized() throws Exception {
        // given -> a valid order body without a bearer token
        // when -> the request is posted to /submit
        mockMvc.perform(post(SUBMIT_URL)
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                // then -> return 401 without creating an order
                .andExpect(status().isUnauthorized());
        verifyNoMoreInteractions(orderService);
    }

    // negative quantity results in bad request (submit order)
    @Test
    void negativeQuantityReturnsBadRequest() throws Exception {
        // given -> a trader JWT and an order with negative quantity
        // when -> the trader posts to /submit
        mockMvc.perform(post(SUBMIT_URL)
                .with(jwt()
                        .jwt(token -> token.claim("preferred_username", "team-1-trader1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_TRADER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"instrumentId":"AAPL","side":"BUY","quantity":-1,"limitPrice":185.50}
                        """))
                // then -> return 400 without calling the service
                .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(orderService);
    }

    // no username (preferred_username) results in unauthorized
    @Test
    void missingUsernameReturnsUnauthorized() throws Exception {
        // given -> a trader JWT without preferred_username and a valid body
        // when -> the trader posts to /submit
        mockMvc.perform(post(SUBMIT_URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRADER")))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                // then -> return 401 without calling the service
                .andExpect(status().isUnauthorized());
        verifyNoMoreInteractions(orderService);
    }

    // trader attempting an ops endpoint (view all) results in 401 (forbidden)
    @Test
    void traderCannotViewAll() throws Exception {
        // given -> a JWT with only TRADER
        // when -> the trader requests /view-all
        mockMvc.perform(get(VIEW_ALL_URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRADER")))
                .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                // then -> return 403 without fetching orders
                .andExpect(status().isForbidden());
        verifyNoMoreInteractions(orderService);
    }

    // trader can canel their own order
    //happy path
    @Test
    void traderCancelsOrder() throws Exception {
        // given -> a trader JWT and a mocked cancelled order
        // mocked service returns the result of cancellation.
        Long orderId = 10L;
        String owner = "team-1-trader1";
        Instant now = Instant.parse("2026-11-19T12:00:00Z");
        OrderResponse cancelledOrder = new OrderResponse(orderId, owner, "AAPL",
                Side.BUY, orderId, new BigDecimal("185.50"), OrderStatus.CANCELLED,
                now, now, 1L);
        when(orderService.cancelOrder(orderId, owner)).thenReturn(cancelledOrder);

        // Act and assert: the controller returns the cancelled order as JSON.
        // when -> the trader posts to /cancel with the order ID
        mockMvc.perform(post(CANCEL_ORDER_URL + orderId)
                .with(jwt()
                        .jwt(token -> token.claim("preferred_username", owner))
                        .authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                // then -> return 200 with CANCELLED and pass the ID and username to the service
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(orderId.intValue()))
                .andExpect(jsonPath("$.owner").value(owner))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelOrder(orderId, owner);
        verifyNoMoreInteractions(orderService);
    }


    // trader cant cancel another order 
    @Test
    void traderAttempsToCancelAnotherOrder() throws Exception {
        // given -> a trader JWT and a mocked ownership rejection
        Long orderId = 10L;
        String otherTrader = "team-1-trader2";
        // Ownership is checked by the service; simulate its rejection here.
        when(orderService.cancelOrder(orderId, otherTrader))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You cannot cancel another user's order"));

        // when -> the trader requests cancellation of another owner's order
        mockMvc.perform(post(CANCEL_ORDER_URL + orderId)
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", otherTrader))
                                .authorities(new SimpleGrantedAuthority("ROLE_TRADER"))))
                // then -> return the service's 403 and pass the caller's username
                .andExpect(status().isForbidden());

        verify(orderService).cancelOrder(orderId, otherTrader);
        verifyNoMoreInteractions(orderService);
    }

    // ops cant cancel order alltogether
    @Test
    void opsAttemptsToCancel() throws Exception {
        // given -> a JWT with only OPERATIONS
        // when -> Operations posts to /cancel
        mockMvc.perform(post(CANCEL_ORDER_URL + "10")
                        .with(jwt()
                                .jwt(token -> token.claim("preferred_username", "team-1-ops1"))
                                .authorities(new SimpleGrantedAuthority("ROLE_OPERATIONS"))))
                // then -> return 403 before any service call
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }
}
