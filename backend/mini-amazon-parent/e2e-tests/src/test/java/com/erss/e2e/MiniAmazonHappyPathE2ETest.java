package com.erss.e2e;

import com.erss.e2e.mock.MockUpsServer;
import com.erss.e2e.mock.MockWorldSimulator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-End Happy Path Test for Mini-Amazon
 *
 * This test simulates the complete flow:
 * 1. User places an order (via Order Service)
 * 2. Order Service calls Warehouse Service to purchase items
 * 3. Warehouse sends purchase command to World (simulated)
 * 4. World sends "arrived" event back (simulated)
 * 5. Warehouse packs the order
 * 6. World sends "pack-ready" event back (simulated)
 * 7. UPS truck arrives at warehouse (simulated)
 * 8. Warehouse loads package onto truck
 * 9. World sends "loaded" event back (simulated)
 * 10. UPS delivers package (simulated)
 * 11. User queries order status and sees it delivered
 *
 * Prerequisites:
 * - All services must be running: API Gateway (8080), Order Service (8081),
 *   Warehouse Service (8082), World Connector (8089)
 * - Services should be in clean state (empty databases)
 */
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MiniAmazonHappyPathE2ETest {

    // Service URLs
    private static final String API_GATEWAY_URL = "http://localhost:8080";
    private static final String ORDER_SERVICE_URL = "http://localhost:8081";
    private static final String WAREHOUSE_SERVICE_URL = "http://localhost:8082";

    // Test data
    private static final String TEST_UPS_ACCOUNT = "test-user-123";
    private static final Integer TEST_WAREHOUSE_ID = 1;

    // Mock services
    private static MockWorldSimulator mockWorldSimulator;
    private static MockUpsServer mockUpsServer;
    private static RestTemplate restTemplate;

    // Test state - shared across test methods
    private static Long orderId;
    private static Long packageId;
    private static Integer truckId;
    private static Long trackingId;

    @BeforeAll
    public static void setUp() {
        log.info("=".repeat(80));
        log.info("Starting Mini-Amazon E2E Happy Path Test");
        log.info("=".repeat(80));

        restTemplate = new RestTemplate();
        mockWorldSimulator = new MockWorldSimulator(WAREHOUSE_SERVICE_URL);
        mockUpsServer = new MockUpsServer(WAREHOUSE_SERVICE_URL);

        log.info("Test setup completed");
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: User places an order")
    public void step1_userPlacesOrder() {
        log.info("\n" + "=".repeat(80));
        log.info("STEP 1: User places an order");
        log.info("=".repeat(80));

        // Prepare order request
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("upsAccount", TEST_UPS_ACCOUNT);

        Map<String, Object> deliveryAddress = new HashMap<>();
        deliveryAddress.put("x", 100);
        deliveryAddress.put("y", 200);
        orderRequest.put("deliveryAddress", deliveryAddress);

        Map<String, Object> item = new HashMap<>();
        item.put("productId", 1L);
        item.put("quantity", 2);
        orderRequest.put("items", List.of(item));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderRequest, headers);

        // Place order via API Gateway
        log.info("Sending order request to: {}/api/orders", API_GATEWAY_URL);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            API_GATEWAY_URL + "/api/orders",
            request,
            Map.class
        );

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        orderId = ((Number) response.getBody().get("orderId")).longValue();
        log.info("✓ Order created successfully: orderId={}", orderId);

        assertThat(orderId).isNotNull().isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Simulate World 'arrived' event")
    public void step2_worldArrivedEvent() throws InterruptedException {
        log.info("\n" + "=".repeat(80));
        log.info("STEP 2: Simulate World 'arrived' event");
        log.info("=".repeat(80));

        // In real system, World Connector would send APurchaseMore command to World
        // World would process it and send back 'arrived' event
        // We simulate this by directly calling the warehouse callback

        // Use orderId as packageId for simplicity (in real system they might differ)
        packageId = orderId;
        Long seqnum = 1L;

        // Give system a moment to process the purchase
        Thread.sleep(1000);

        log.info("Simulating World 'arrived' event for packageId={}", packageId);
        mockWorldSimulator.simulateArrivedEvent(packageId, seqnum);

        // Wait for the warehouse to process the arrived event
        await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                Map<String, Object> status = getPackageStatus(packageId);
                assertThat(status.get("status")).isEqualTo("arrived");
            });

        log.info("✓ Package status updated to 'arrived'");
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Pack the order")
    public void step3_packOrder() throws InterruptedException {
        log.info("\n" + "=".repeat(80));
        log.info("STEP 3: Pack the order");
        log.info("=".repeat(80));

        // Call warehouse pack endpoint
        Map<String, Object> packRequest = new HashMap<>();
        packRequest.put("packageId", packageId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(packRequest, headers);

        log.info("Sending pack request for packageId={}", packageId);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            API_GATEWAY_URL + "/api/warehouse/pack",
            request,
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        log.info("✓ Pack request sent successfully");

        // Give system a moment to send command to World
        Thread.sleep(1000);
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Simulate World 'pack-ready' event")
    public void step4_worldPackReadyEvent() {
        log.info("\n" + "=".repeat(80));
        log.info("STEP 4: Simulate World 'pack-ready' event");
        log.info("=".repeat(80));

        Long seqnum = 2L;

        log.info("Simulating World 'pack-ready' event for packageId={}", packageId);
        mockWorldSimulator.simulatePackReadyEvent(packageId, seqnum);

        // Wait for the warehouse to process the pack-ready event
        await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                Map<String, Object> status = getPackageStatus(packageId);
                assertThat(status.get("status")).isEqualTo("ready_for_pickup");
            });

        log.info("✓ Package status updated to 'ready_for_pickup'");
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Simulate UPS truck arrives")
    public void step5_upsTruckArrives() {
        log.info("\n" + "=".repeat(80));
        log.info("STEP 5: Simulate UPS truck arrives at warehouse");
        log.info("=".repeat(80));

        log.info("Simulating UPS truck arrival at warehouse {}", TEST_WAREHOUSE_ID);
        truckId = mockUpsServer.simulateTruckArrived(TEST_WAREHOUSE_ID);

        log.info("✓ UPS truck arrived: truckId={}", truckId);
        assertThat(truckId).isNotNull().isPositive();
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Simulate World 'loaded' event")
    public void step6_worldLoadedEvent() throws InterruptedException {
        log.info("\n" + "=".repeat(80));
        log.info("STEP 6: Simulate World 'loaded' event");
        log.info("=".repeat(80));

        // Give system a moment to process truck arrival and send load command
        Thread.sleep(2000);

        Long seqnum = 3L;

        log.info("Simulating World 'loaded' event for packageId={}, truckId={}", packageId, truckId);
        mockWorldSimulator.simulateLoadedEvent(packageId, seqnum, truckId);

        // Wait for the warehouse to process the loaded event
        await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                Map<String, Object> status = getPackageStatus(packageId);
                assertThat(status.get("status")).isEqualTo("loaded");
            });

        log.info("✓ Package status updated to 'loaded'");
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Simulate UPS package delivered")
    public void step7_upsPackageDelivered() {
        log.info("\n" + "=".repeat(80));
        log.info("STEP 7: Simulate UPS package delivered");
        log.info("=".repeat(80));

        trackingId = System.currentTimeMillis();

        log.info("Simulating UPS package delivery: packageId={}, trackingId={}", packageId, trackingId);
        mockUpsServer.simulatePackageDelivered(packageId, trackingId);

        log.info("✓ Package delivery simulated");
    }

    @Test
    @Order(8)
    @DisplayName("Step 8: Verify complete order status")
    public void step8_verifyCompleteOrderStatus() {
        log.info("\n" + "=".repeat(80));
        log.info("STEP 8: Verify complete order status");
        log.info("=".repeat(80));

        // Query order status via API Gateway
        log.info("Querying order status for upsAccount={}", TEST_UPS_ACCOUNT);
        ResponseEntity<List> response = restTemplate.getForEntity(
            API_GATEWAY_URL + "/api/orders/account/" + TEST_UPS_ACCOUNT,
            List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();

        Map<String, Object> order = (Map<String, Object>) response.getBody().get(0);
        log.info("Order status: {}", order);

        assertThat(order.get("id")).isEqualTo(orderId.intValue());

        // Verify package status details
        Map<String, Object> packageStatus = getPackageStatus(packageId);
        log.info("Package status: {}", packageStatus);

        assertThat(packageStatus.get("status")).isEqualTo("loaded");
        assertThat(packageStatus.get("package_id")).isEqualTo(packageId.intValue());

        List<Map<String, Object>> operations = (List<Map<String, Object>>) packageStatus.get("operations");
        assertThat(operations).isNotEmpty();

        // Verify all operations completed
        boolean hasPurchase = operations.stream()
            .anyMatch(op -> "PURCHASE".equals(op.get("operation")) && "COMPLETED".equals(op.get("status")));
        boolean hasArrived = operations.stream()
            .anyMatch(op -> "ARRIVED".equals(op.get("operation")) && "COMPLETED".equals(op.get("status")));
        boolean hasPack = operations.stream()
            .anyMatch(op -> "PACK".equals(op.get("operation")) && "COMPLETED".equals(op.get("status")));
        boolean hasLoad = operations.stream()
            .anyMatch(op -> "LOAD".equals(op.get("operation")) && "COMPLETED".equals(op.get("status")));

        assertThat(hasPurchase).isTrue();
        assertThat(hasArrived).isTrue();
        assertThat(hasPack).isTrue();
        assertThat(hasLoad).isTrue();

        log.info("✓ All operations completed successfully:");
        log.info("  - PURCHASE: COMPLETED");
        log.info("  - ARRIVED: COMPLETED");
        log.info("  - PACK: COMPLETED");
        log.info("  - LOAD: COMPLETED");
    }

    @AfterAll
    public static void tearDown() {
        log.info("\n" + "=".repeat(80));
        log.info("Mini-Amazon E2E Happy Path Test COMPLETED");
        log.info("=".repeat(80));
        log.info("Summary:");
        log.info("  Order ID: {}", orderId);
        log.info("  Package ID: {}", packageId);
        log.info("  Truck ID: {}", truckId);
        log.info("  Tracking ID: {}", trackingId);
        log.info("=".repeat(80));
    }

    // Helper method to get package status
    private Map<String, Object> getPackageStatus(Long packageId) {
        ResponseEntity<Map> response = restTemplate.getForEntity(
            WAREHOUSE_SERVICE_URL + "/api/warehouse/status/" + packageId,
            Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }

        throw new RuntimeException("Failed to get package status: " + response.getStatusCode());
    }
}
