
package com.alumniplus.activity_service.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.BadJwtException;

import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.repository.OrderActivityRepository;
import com.alumniplus.activity_service.service.OrderActivityService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("ci")
class OrderActivityControllerTest {

    @Autowired
    private OrderActivityService service;

    @Autowired
    private OrderActivityRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private static final Long ORDER_ID = 900000001L;
    private static final Long UNOWNED_ORDER_ID = 900000002L;

    private static final String TRADER_ONE = "test-trader1";
    private static final String TRADER_TWO = "test-trader2";

    private String submissionId;
    private String acceptanceId;
    private String unownedEventId;

    @BeforeEach
    void setUp() {

        submissionId = UUID.randomUUID().toString();
        acceptanceId = UUID.randomUUID().toString();
        unownedEventId = UUID.randomUUID().toString();

        LocalDateTime now = LocalDateTime.now();

        service.recordActivity(new OrderActivity(
            submissionId,
            ORDER_ID,
            "SUBMITTED",
            now.minusSeconds(10),
            TRADER_ONE
        ));

        service.recordActivity(new OrderActivity(
            acceptanceId,
            ORDER_ID,
            "ACCEPTED",
            now,
            "SIMULATOR"
        ));

        service.recordActivity(new OrderActivity(
            unownedEventId,
            UNOWNED_ORDER_ID,
            "ACCEPTED",
            now,
            "SIMULATOR"
        ));

        mockToken("trader-one-token", TRADER_ONE, "TRADER");
        mockToken("trader-two-token", TRADER_TWO, "TRADER");
        mockToken("operations-token", "test-operations", "OPERATIONS");
        mockToken("viewer-token", "test-viewer", "VIEWER");

        when(jwtDecoder.decode("invalid-token"))
            .thenThrow(new BadJwtException("Invalid test token"));
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAllById(List.of(
            submissionId,
            acceptanceId,
            unownedEventId
        ));
    }

    private void mockToken(
            String token,
            String username,
            String role) {

        Jwt jwt = Jwt.withTokenValue(token)
            .header("alg", "RS256")
            .subject(username)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(900))
            .claim("preferred_username", username)
            .claim("realm_access", Map.of(
                "roles", List.of(role)
            ))
            .build();

        when(jwtDecoder.decode(token)).thenReturn(jwt);
    }

    private HttpResponse<String> getHistory(
            Long orderId,
            String token) throws Exception {

        HttpRequest.Builder request = HttpRequest.newBuilder()
            .uri(URI.create(
                "http://localhost:" + port
                    + "/api/activity/" + orderId
            ))
            .GET();

        if (token != null) {
            request.header(
                "Authorization",
                "Bearer " + token
            );
        }

        return client.send(
            request.build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {

        HttpResponse<String> response =
            getHistory(ORDER_ID, null);

        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldRejectInvalidToken() throws Exception {

        HttpResponse<String> response =
            getHistory(ORDER_ID, "invalid-token");

        assertEquals(401, response.statusCode());
    }

    @Test
    void shouldAllowTraderToViewOwnOrder() throws Exception {

        HttpResponse<String> response =
            getHistory(ORDER_ID, "trader-one-token");

        assertEquals(200, response.statusCode());

        JsonNode json = objectMapper.readTree(response.body());

        assertTrue(json.isArray());
        assertEquals(2, json.size());

        assertEquals(
            submissionId,
            json.get(0).path("eventId").asText()
        );

        assertEquals(
            ORDER_ID.longValue(),
            json.get(0).path("orderId").asLong()
        );

        assertEquals(
            "SUBMITTED",
            json.get(0).path("eventType").asText()
        );

        assertEquals(
            TRADER_ONE,
            json.get(0).path("owner").asText()
        );

        assertEquals(
            acceptanceId,
            json.get(1).path("eventId").asText()
        );

        assertEquals(
            "ACCEPTED",
            json.get(1).path("eventType").asText()
        );

        assertEquals(
            "SIMULATOR",
            json.get(1).path("owner").asText()
        );
    }

    @Test
    void shouldRejectTraderViewingAnotherOrder() throws Exception {

        HttpResponse<String> response =
            getHistory(ORDER_ID, "trader-two-token");

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldAllowOperationsToViewAnyOrder() throws Exception {

        HttpResponse<String> response =
            getHistory(ORDER_ID, "operations-token");

        assertEquals(200, response.statusCode());

        JsonNode json = objectMapper.readTree(response.body());

        assertTrue(json.isArray());
        assertEquals(2, json.size());
    }

    @Test
    void shouldRejectUnauthorizedRole() throws Exception {

        HttpResponse<String> response =
            getHistory(ORDER_ID, "viewer-token");

        assertEquals(403, response.statusCode());
    }

    @Test
    void shouldRejectTraderWhenOwnershipIsUnknown() throws Exception {

        HttpResponse<String> response =
            getHistory(UNOWNED_ORDER_ID, "trader-one-token");

        assertEquals(403, response.statusCode());
    }
}
