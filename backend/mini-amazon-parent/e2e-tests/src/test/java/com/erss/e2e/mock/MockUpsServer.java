package com.erss.e2e.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock UPS Server that simulates UPS callbacks
 * In the real system, UPS would be another team's service
 * Here we simulate by providing mock responses and callbacks
 */
@Slf4j
public class MockUpsServer {

    private final String warehouseCallbackUrl;
    private final RestTemplate restTemplate;
    private Integer nextTruckId = 1000;

    public MockUpsServer(String warehouseCallbackUrl) {
        this.warehouseCallbackUrl = warehouseCallbackUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Simulate UPS sending "truck_arrived" callback
     * This happens when UPS truck arrives at warehouse
     */
    public Integer simulateTruckArrived(Integer warehouseId) {
        Integer truckId = nextTruckId++;
        log.info("MockUPS: Simulating truck_arrived event - truckId={}, warehouseId={}", truckId, warehouseId);

        Map<String, Object> truckArrivedRequest = new HashMap<>();
        truckArrivedRequest.put("action", "truck_arrived");
        truckArrivedRequest.put("truck_id", truckId);
        truckArrivedRequest.put("warehouse_id", warehouseId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(truckArrivedRequest, headers);

        try {
            restTemplate.postForEntity(
                warehouseCallbackUrl + "/api/ups",
                request,
                Map.class
            );
            log.info("MockUPS: Truck arrived event sent successfully");
            return truckId;
        } catch (Exception e) {
            log.error("MockUPS: Failed to send truck arrived event", e);
            throw new RuntimeException("Failed to simulate truck arrived", e);
        }
    }

    /**
     * Simulate UPS sending "package_delivered" callback
     * This happens when package is delivered to customer
     */
    public void simulatePackageDelivered(Long packageId, Long trackingId) {
        log.info("MockUPS: Simulating package_delivered event - packageId={}, trackingId={}", packageId, trackingId);

        Map<String, Object> deliveredRequest = new HashMap<>();
        deliveredRequest.put("action", "package_delivered");
        deliveredRequest.put("package_id", packageId);
        deliveredRequest.put("tracking_id", trackingId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(deliveredRequest, headers);

        try {
            restTemplate.postForEntity(
                warehouseCallbackUrl + "/api/ups",
                request,
                Map.class
            );
            log.info("MockUPS: Package delivered event sent successfully");
        } catch (Exception e) {
            log.error("MockUPS: Failed to send package delivered event", e);
            throw new RuntimeException("Failed to simulate package delivered", e);
        }
    }

    /**
     * Mock response for UPS loading package request
     * This would normally be handled by WireMock, but included here for completeness
     */
    public Map<String, Object> mockLoadingPackageResponse(Long packageId, Integer truckId) {
        log.info("MockUPS: Mock response for loading package - packageId={}, truckId={}", packageId, truckId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Package scheduled for loading");
        response.put("package_id", packageId);
        response.put("truck_id", truckId);
        response.put("tracking_id", System.currentTimeMillis());

        return response;
    }
}
