package com.erss.warehouse.integration;

import com.erss.warehouse.entity.PackageOperation;
import com.erss.warehouse.repository.PackageOperationRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for truck_arrived callback concurrency scenarios
 *
 * Tests the atomic package claiming mechanism using SELECT FOR UPDATE
 * to prevent double-loading when multiple truck_arrived callbacks
 * arrive concurrently.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
public class TruckArrivedConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PackageOperationRepository packageOperationRepository;

    @BeforeEach
    @Transactional
    public void setUp() {
        // Clean up any existing test data
        packageOperationRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrent truck_arrived callbacks should claim different packages atomically")
    public void testConcurrentTruckArrived_shouldClaimDifferentPackages() throws Exception {
        // Arrange: Create 3 ready packages
        log.info("=== Setting up 3 ready packages ===");
        for (int i = 1; i <= 3; i++) {
            PackageOperation purchase = new PackageOperation();
            purchase.setPackageId((long) i);
            purchase.setOperation("PURCHASE");
            purchase.setStatus("COMPLETED");
            packageOperationRepository.save(purchase);

            PackageOperation pack = new PackageOperation();
            pack.setPackageId((long) i);
            pack.setOperation("PACK");
            pack.setStatus("COMPLETED");
            packageOperationRepository.save(pack);

            log.info("Created ready package: packageId={}", i);
        }

        // Act: Send 5 concurrent truck_arrived requests
        int numThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        Set<Long> claimedPackages = ConcurrentHashMap.newKeySet();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        log.info("=== Sending {} concurrent truck_arrived requests ===", numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int truckId = 100 + i;
            executorService.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    String requestBody = String.format(
                        "{\"action\":\"truck_arrived\",\"truck_id\":%d,\"warehouse_id\":1}",
                        truckId
                    );

                    log.info("Thread {} sending truck_arrived for truckId={}",
                            Thread.currentThread().getName(), truckId);

                    String response = mockMvc.perform(post("/api/ups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

                    log.info("Thread {} received response: {}",
                            Thread.currentThread().getName(), response);

                    // Extract package ID from response if present
                    // Response format: "message": "Truck X arrival processed, package Y"
                    if (response.contains("package")) {
                        String[] parts = response.split("package ");
                        if (parts.length > 1) {
                            String pkgStr = parts[1].replaceAll("[^0-9]", "");
                            if (!pkgStr.isEmpty()) {
                                Long packageId = Long.parseLong(pkgStr);
                                claimedPackages.add(packageId);
                                log.info("Truck {} claimed package {}", truckId, packageId);
                            }
                        }
                    }

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("Thread {} encountered error: {}",
                            Thread.currentThread().getName(), e.getMessage());
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads at once for maximum concurrency
        startLatch.countDown();

        // Wait for all threads to complete (with timeout)
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert
        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();

        log.info("=== Test Results ===");
        log.info("Success count: {}", successCount.get());
        log.info("Claimed packages: {}", claimedPackages);

        // All requests should succeed (even if some get "no packages ready")
        assertThat(successCount.get()).isEqualTo(numThreads);

        // At most 3 packages should be claimed (we only have 3 ready packages)
        assertThat(claimedPackages.size()).isLessThanOrEqualTo(3);

        // Each package should be claimed at most once
        List<PackageOperation> loadOps = packageOperationRepository.findByStatusAndOperation("PENDING", "LOAD");
        log.info("Total LOAD operations created: {}", loadOps.size());

        // Verify no duplicate package assignments
        Set<Long> loadedPackageIds = new HashSet<>();
        for (PackageOperation op : loadOps) {
            Long pkgId = op.getPackageId();
            assertThat(loadedPackageIds).doesNotContain(pkgId);
            loadedPackageIds.add(pkgId);
            log.info("LOAD operation: packageId={}, truckId={}", pkgId, op.getTruckId());
        }

        log.info("✓ Test passed: {} packages claimed with no duplicates", loadedPackageIds.size());
    }

    @Test
    @DisplayName("10 concurrent requests with 1 package should result in exactly 1 claim")
    public void testHighConcurrency_singlePackage_shouldClaimOnce() throws Exception {
        // Arrange: Create only 1 ready package
        log.info("=== Setting up 1 ready package ===");
        PackageOperation purchase = new PackageOperation();
        purchase.setPackageId(999L);
        purchase.setOperation("PURCHASE");
        purchase.setStatus("COMPLETED");
        packageOperationRepository.save(purchase);

        PackageOperation pack = new PackageOperation();
        pack.setPackageId(999L);
        pack.setOperation("PACK");
        pack.setStatus("COMPLETED");
        packageOperationRepository.save(pack);

        // Act: Send 10 concurrent truck_arrived requests
        int numThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        log.info("=== Sending {} concurrent truck_arrived requests ===", numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int truckId = 200 + i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    String requestBody = String.format(
                        "{\"action\":\"truck_arrived\",\"truck_id\":%d,\"warehouse_id\":1}",
                        truckId
                    );

                    mockMvc.perform(post("/api/ups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                            .andExpect(status().isOk());

                } catch (Exception e) {
                    log.error("Error: {}", e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert
        assertThat(completed).isTrue();

        // Verify exactly 1 LOAD operation was created
        List<PackageOperation> loadOps = packageOperationRepository.findByStatusAndOperation("PENDING", "LOAD");

        log.info("=== Test Results ===");
        log.info("Total LOAD operations: {}", loadOps.size());

        assertThat(loadOps).hasSize(1);
        assertThat(loadOps.get(0).getPackageId()).isEqualTo(999L);

        log.info("✓ Test passed: Package 999 claimed exactly once by truck {}",
                loadOps.get(0).getTruckId());
    }

    @Test
    @DisplayName("Concurrent requests with no ready packages should all return success with 'no packages' message")
    public void testConcurrentTruckArrived_noReadyPackages_shouldAllSucceed() throws Exception {
        // Arrange: No ready packages (empty DB)
        log.info("=== No ready packages in system ===");

        // Act: Send 5 concurrent truck_arrived requests
        int numThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);

        log.info("=== Sending {} concurrent truck_arrived requests ===", numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int truckId = 300 + i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    String requestBody = String.format(
                        "{\"action\":\"truck_arrived\",\"truck_id\":%d,\"warehouse_id\":1}",
                        truckId
                    );

                    String response = mockMvc.perform(post("/api/ups")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                            .andExpect(status().isOk())
                            .andReturn()
                            .getResponse()
                            .getContentAsString();

                    // Should contain "No packages ready" message
                    assertThat(response).containsIgnoringCase("no packages");
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    log.error("Error: {}", e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // Assert
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(numThreads);

        // Verify no LOAD operations were created
        List<PackageOperation> loadOps = packageOperationRepository.findByStatusAndOperation("PENDING", "LOAD");
        assertThat(loadOps).isEmpty();

        log.info("✓ Test passed: All {} requests succeeded with 'no packages' response", numThreads);
    }
}
