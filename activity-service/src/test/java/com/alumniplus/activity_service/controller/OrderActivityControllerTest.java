
package com.alumniplus.activity_service.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.alumniplus.activity_service.model.OrderActivity;
import com.alumniplus.activity_service.repository.OrderActivityRepository;
import com.alumniplus.activity_service.service.OrderActivityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class OrderActivityControllerTest {

    @Autowired
    private OrderActivityService service;

    @Autowired
    private OrderActivityRepository repository;

    @LocalServerPort
    private int port;

    @Test
    void shouldReturnOrderHistory() throws Exception {

        Long orderId = 900000001L;

        OrderActivity activity = new OrderActivity(
            UUID.randomUUID().toString(),
            orderId,
            "SUBMITTED",
            LocalDateTime.now(),
            "test-user"
        );

        service.recordActivity(activity);

        try {
            String url = "http://localhost:" + port
                    + "/api/activity/" + orderId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response;

            HttpClient client = HttpClient.newHttpClient();

            response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            assertEquals(200, response.statusCode());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.body());

            assertTrue(json.isArray());

            boolean found = false;

            for (JsonNode item : json) {
                if (activity.getEventId().equals(
                        item.path("eventId").asText())) {

                    assertEquals(
                        orderId.longValue(),
                        item.path("orderId").asLong()
                    );

                    assertEquals(
                        "SUBMITTED",
                        item.path("eventType").asText()
                    );

                    assertEquals(
                        "test-user",
                        item.path("owner").asText()
                    );

                    found = true;
                    break;
                }
            }

            assertTrue(
                found,
                "The created activity was not returned by the API"
            );

        } finally {
            repository.deleteById(activity.getEventId());
        }
    }
}
