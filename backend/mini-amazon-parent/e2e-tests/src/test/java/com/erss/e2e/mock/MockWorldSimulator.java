package com.erss.e2e.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock World Simulator that simulates World Simulator callbacks
 * In the real system, World Simulator would send events via Protobuf/TCP
 * Here we simulate by directly calling the warehouse callback endpoints
 */
@Slf4j
public class MockWorldSimulator {

    private final String warehouseCallbackUrl;
    private final RestTemplate restTemplate;

    public MockWorldSimulator(String warehouseCallbackUrl) {
        this.warehouseCallbackUrl = warehouseCallbackUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Simulate World responding with "arrived" event after APurchaseMore command
     */
    public void simulateArrivedEvent(Long packageId, Long seqnum) {
        log.info("MockWorldSimulator: Simulating arrived event for packageId={}, seqnum={}", packageId, seqnum);

        Map<String, Object> arrivedRequest = new HashMap<>();
        arrivedRequest.put("packageId", packageId);
        arrivedRequest.put("seqnum", seqnum);
        arrivedRequest.put("items", List.of(
            Map.of("productId", 1L, "description", "Test Product", "count", 2)
        ));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(arrivedRequest, headers);

        try {
            restTemplate.postForEntity(
                warehouseCallbackUrl + "/internal/world/arrived",
                request,
                Void.class
            );
            log.info("MockWorldSimulator: Arrived event sent successfully");
        } catch (Exception e) {
            log.error("MockWorldSimulator: Failed to send arrived event", e);
            throw new RuntimeException("Failed to simulate arrived event", e);
        }
    }

    /**
     * Simulate World responding with "pack-ready" event after APack command
     */
    public void simulatePackReadyEvent(Long packageId, Long seqnum) {
        log.info("MockWorldSimulator: Simulating pack-ready event for packageId={}, seqnum={}", packageId, seqnum);

        Map<String, Object> packReadyRequest = new HashMap<>();
        packReadyRequest.put("packageId", packageId);
        packReadyRequest.put("seqnum", seqnum);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(packReadyRequest, headers);

        try {
            restTemplate.postForEntity(
                warehouseCallbackUrl + "/internal/world/pack-ready",
                request,
                Void.class
            );
            log.info("MockWorldSimulator: Pack-ready event sent successfully");
        } catch (Exception e) {
            log.error("MockWorldSimulator: Failed to send pack-ready event", e);
            throw new RuntimeException("Failed to simulate pack-ready event", e);
        }
    }

    /**
     * Simulate World responding with "loaded" event after APutOnTruck command
     */
    public void simulateLoadedEvent(Long packageId, Long seqnum, Integer truckId) {
        log.info("MockWorldSimulator: Simulating loaded event for packageId={}, seqnum={}, truckId={}",
                packageId, seqnum, truckId);

        Map<String, Object> loadedRequest = new HashMap<>();
        loadedRequest.put("packageId", packageId);
        loadedRequest.put("seqnum", seqnum);
        loadedRequest.put("truckId", truckId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(loadedRequest, headers);

        try {
            restTemplate.postForEntity(
                warehouseCallbackUrl + "/internal/world/loaded",
                request,
                Void.class
            );
            log.info("MockWorldSimulator: Loaded event sent successfully");
        } catch (Exception e) {
            log.error("MockWorldSimulator: Failed to send loaded event", e);
            throw new RuntimeException("Failed to simulate loaded event", e);
        }
    }
}
