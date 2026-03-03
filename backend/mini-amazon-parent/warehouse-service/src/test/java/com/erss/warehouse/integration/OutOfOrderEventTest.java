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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for out-of-order event handling
 *
 * Tests the state machine validation logic that ensures events arrive
 * in the correct order. For example, package_delivered should not be
 * accepted if the package was never loaded.
 *
 * This prevents state divergence when callbacks arrive out of order
 * due to network delays or retry logic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
public class OutOfOrderEventTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PackageOperationRepository packageOperationRepository;

    @BeforeEach
    @Transactional
    public void setUp() {
        packageOperationRepository.deleteAll();
    }

    @Test
    @DisplayName("package_delivered before LOAD should return 409 Conflict")
    public void testDeliveredBeforeLoad_shouldReturn409() throws Exception {
        // Arrange: Create a package that's only been purchased (not loaded)
        Long packageId = 5001L;
        Integer truckId = 800;

        log.info("=== Setting up package with PURCHASE only: packageId={} ===", packageId);

        PackageOperation purchase = new PackageOperation();
        purchase.setPackageId(packageId);
        purchase.setOperation("PURCHASE");
        purchase.setStatus("COMPLETED");
        packageOperationRepository.save(purchase);

        // Act: Send package_delivered callback (out of order)
        String requestBody = String.format(
            "{\"action\":\"package_delivered\",\"package_id\":%d,\"truck_id\":%d," +
            "\"delivery_x\":100,\"delivery_y\":200}",
            packageId, truckId
        );

        log.info("=== Sending package_delivered before LOAD (out of order) ===");

        // Assert: Should return 409 Conflict
        mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.reason").value("state_machine_violation"))
                .andExpect(jsonPath("$.message").value(
                    "Package not loaded yet. Please retry after loading is confirmed."));

        // Verify no DELIVERED operation was created
        Optional<PackageOperation> deliveredOp = packageOperationRepository
                .findByPackageIdAndOperation(packageId, "DELIVERED");

        assertThat(deliveredOp).isEmpty();

        log.info("✓ Test passed: Out-of-order delivered event rejected with 409 Conflict");
    }

    @Test
    @DisplayName("package_delivered before LOAD COMPLETED should return 409 Conflict")
    public void testDeliveredBeforeLoadCompleted_shouldReturn409() throws Exception {
        // Arrange: Create a package with LOAD still PENDING
        Long packageId = 5002L;
        Integer truckId = 801;

        log.info("=== Setting up package with LOAD PENDING: packageId={} ===", packageId);

        PackageOperation purchase = new PackageOperation();
        purchase.setPackageId(packageId);
        purchase.setOperation("PURCHASE");
        purchase.setStatus("COMPLETED");
        packageOperationRepository.save(purchase);

        PackageOperation load = new PackageOperation();
        load.setPackageId(packageId);
        load.setOperation("LOAD");
        load.setStatus("PENDING");  // Not COMPLETED yet
        load.setTruckId(truckId);
        packageOperationRepository.save(load);

        // Act: Send package_delivered callback (out of order)
        String requestBody = String.format(
            "{\"action\":\"package_delivered\",\"package_id\":%d,\"truck_id\":%d," +
            "\"delivery_x\":100,\"delivery_y\":200}",
            packageId, truckId
        );

        log.info("=== Sending package_delivered before LOAD completes (out of order) ===");

        // Assert: Should return 409 Conflict
        mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.reason").value("state_machine_violation"));

        // Verify no DELIVERED operation was created
        Optional<PackageOperation> deliveredOp = packageOperationRepository
                .findByPackageIdAndOperation(packageId, "DELIVERED");

        assertThat(deliveredOp).isEmpty();

        log.info("✓ Test passed: Delivered before LOAD COMPLETED rejected with 409 Conflict");
    }

    @Test
    @DisplayName("package_delivered after LOAD COMPLETED should succeed")
    public void testDeliveredAfterLoadCompleted_shouldSucceed() throws Exception {
        // Arrange: Create a properly loaded package
        Long packageId = 5003L;
        Integer truckId = 802;

        log.info("=== Setting up properly loaded package: packageId={} ===", packageId);

        PackageOperation purchase = new PackageOperation();
        purchase.setPackageId(packageId);
        purchase.setOperation("PURCHASE");
        purchase.setStatus("COMPLETED");
        packageOperationRepository.save(purchase);

        PackageOperation load = new PackageOperation();
        load.setPackageId(packageId);
        load.setOperation("LOAD");
        load.setStatus("COMPLETED");  // Properly completed
        load.setTruckId(truckId);
        packageOperationRepository.save(load);

        // Act: Send package_delivered callback (in correct order)
        String requestBody = String.format(
            "{\"action\":\"package_delivered\",\"package_id\":%d,\"truck_id\":%d," +
            "\"delivery_x\":100,\"delivery_y\":200}",
            packageId, truckId
        );

        log.info("=== Sending package_delivered after LOAD COMPLETED (correct order) ===");

        // Assert: Should succeed
        mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Delivery confirmed"));

        // Verify DELIVERED operation was created
        Optional<PackageOperation> deliveredOp = packageOperationRepository
                .findByPackageIdAndOperation(packageId, "DELIVERED");

        assertThat(deliveredOp).isPresent();
        assertThat(deliveredOp.get().getStatus()).isEqualTo("COMPLETED");
        assertThat(deliveredOp.get().getTruckId()).isEqualTo(truckId);

        log.info("✓ Test passed: In-order delivered event accepted");
    }

    @Test
    @DisplayName("truck_arrived → package_delivered → truck_arrived (retry) should handle gracefully")
    public void testOutOfOrderRetry_shouldHandleGracefully() throws Exception {
        // Arrange: Create two ready packages
        Long pkg1 = 6001L;
        Long pkg2 = 6002L;

        log.info("=== Setting up two ready packages: pkg1={}, pkg2={} ===", pkg1, pkg2);

        for (Long pkgId : new Long[]{pkg1, pkg2}) {
            PackageOperation purchase = new PackageOperation();
            purchase.setPackageId(pkgId);
            purchase.setOperation("PURCHASE");
            purchase.setStatus("COMPLETED");
            packageOperationRepository.save(purchase);

            PackageOperation pack = new PackageOperation();
            pack.setPackageId(pkgId);
            pack.setOperation("PACK");
            pack.setStatus("COMPLETED");
            packageOperationRepository.save(pack);
        }

        Integer truck1 = 900;
        Integer truck2 = 901;

        // Act: Simulate complex out-of-order scenario
        // 1. Truck 1 arrives → claims pkg1
        log.info("=== Step 1: Truck {} arrives ===", truck1);
        mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"action\":\"truck_arrived\",\"truck_id\":%d,\"warehouse_id\":1}", truck1
                )))
                .andExpect(status().isOk());

        // 2. Manually mark pkg1 as LOAD COMPLETED (simulating world callback)
        Optional<PackageOperation> load1 = packageOperationRepository
                .findByPackageIdAndOperation(pkg1, "LOAD");
        if (load1.isPresent()) {
            load1.get().setStatus("COMPLETED");
            packageOperationRepository.save(load1.get());
            log.info("=== Step 2: Package {} LOAD completed ===", pkg1);
        }

        // 3. Package 1 delivered
        log.info("=== Step 3: Package {} delivered ===", pkg1);
        mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"action\":\"package_delivered\",\"package_id\":%d,\"truck_id\":%d," +
                    "\"delivery_x\":100,\"delivery_y\":200}",
                    pkg1, truck1
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // 4. Truck 2 arrives (late retry of earlier truck_arrived) → should claim pkg2
        log.info("=== Step 4: Truck {} arrives (late retry) ===", truck2);
        mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"action\":\"truck_arrived\",\"truck_id\":%d,\"warehouse_id\":1}", truck2
                )))
                .andExpect(status().isOk());

        // Assert: Verify final state
        Optional<PackageOperation> load1Final = packageOperationRepository
                .findByPackageIdAndOperation(pkg1, "LOAD");
        Optional<PackageOperation> delivered1 = packageOperationRepository
                .findByPackageIdAndOperation(pkg1, "DELIVERED");
        Optional<PackageOperation> load2 = packageOperationRepository
                .findByPackageIdAndOperation(pkg2, "LOAD");

        log.info("=== Final State ===");
        assertThat(load1Final).isPresent();
        assertThat(load1Final.get().getStatus()).isEqualTo("COMPLETED");
        assertThat(load1Final.get().getTruckId()).isEqualTo(truck1);

        assertThat(delivered1).isPresent();
        assertThat(delivered1.get().getStatus()).isEqualTo("COMPLETED");

        assertThat(load2).isPresent();
        assertThat(load2.get().getTruckId()).isEqualTo(truck2);

        log.info("✓ Test passed: Complex out-of-order scenario handled correctly");
        log.info("  - Package {} loaded on truck {} and delivered", pkg1, truck1);
        log.info("  - Package {} loaded on truck {}", pkg2, truck2);
    }

    @Test
    @DisplayName("package_delivered with non-existent package should return error")
    public void testDeliveredNonExistentPackage_shouldReturnError() throws Exception {
        // Arrange: No package in system
        Long packageId = 9999L;
        Integer truckId = 999;

        log.info("=== Sending package_delivered for non-existent package {} ===", packageId);

        // Act & Assert
        String requestBody = String.format(
            "{\"action\":\"package_delivered\",\"package_id\":%d,\"truck_id\":%d," +
            "\"delivery_x\":100,\"delivery_y\":200}",
            packageId, truckId
        );

        mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"));

        log.info("✓ Test passed: Non-existent package rejected");
    }
}
