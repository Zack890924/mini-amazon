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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for callback idempotency
 *
 * Tests that duplicate callbacks (due to at-least-once delivery semantics)
 * are handled idempotently and do not cause duplicate state transitions
 * or data corruption.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
public class CallbackIdempotencyTest {

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
    @DisplayName("Duplicate package_delivered callback should be idempotent")
    public void testDuplicatePackageDelivered_shouldBeIdempotent() throws Exception {
        // Arrange: Create a loaded package
        Long packageId = 1001L;
        Integer truckId = 500;

        log.info("=== Setting up loaded package: packageId={} ===", packageId);

        PackageOperation purchase = new PackageOperation();
        purchase.setPackageId(packageId);
        purchase.setOperation("PURCHASE");
        purchase.setStatus("COMPLETED");
        packageOperationRepository.save(purchase);

        PackageOperation pack = new PackageOperation();
        pack.setPackageId(packageId);
        pack.setOperation("PACK");
        pack.setStatus("COMPLETED");
        packageOperationRepository.save(pack);

        PackageOperation load = new PackageOperation();
        load.setPackageId(packageId);
        load.setOperation("LOAD");
        load.setStatus("COMPLETED");
        load.setTruckId(truckId);
        packageOperationRepository.save(load);

        // Act: Send package_delivered callback TWICE
        String requestBody = String.format(
            "{\"action\":\"package_delivered\",\"package_id\":%d,\"truck_id\":%d," +
            "\"delivery_x\":100,\"delivery_y\":200}",
            packageId, truckId
        );

        log.info("=== Sending first package_delivered callback ===");
        mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Delivery confirmed"));

        log.info("=== Sending duplicate package_delivered callback ===");
        mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Delivery already confirmed (idempotent)"));

        // Assert: Verify only one DELIVERED operation exists
        List<PackageOperation> deliveredOps = packageOperationRepository
                .findByPackageIdAndOperation(packageId, "DELIVERED")
                .stream()
                .toList();

        log.info("=== Test Results ===");
        log.info("Total DELIVERED operations: {}", deliveredOps.size());

        assertThat(deliveredOps).hasSize(1);
        assertThat(deliveredOps.get(0).getStatus()).isEqualTo("COMPLETED");
        assertThat(deliveredOps.get(0).getTruckId()).isEqualTo(truckId);

        log.info("✓ Test passed: Duplicate callback handled idempotently");
    }

    @Test
    @DisplayName("Multiple truck_arrived callbacks should not double-claim same package")
    public void testDuplicateTruckArrived_shouldNotDoubleClaim() throws Exception {
        // Arrange: Create one ready package
        Long packageId = 2001L;

        log.info("=== Setting up ready package: packageId={} ===", packageId);

        PackageOperation purchase = new PackageOperation();
        purchase.setPackageId(packageId);
        purchase.setOperation("PURCHASE");
        purchase.setStatus("COMPLETED");
        packageOperationRepository.save(purchase);

        PackageOperation pack = new PackageOperation();
        pack.setPackageId(packageId);
        pack.setOperation("PACK");
        pack.setStatus("COMPLETED");
        packageOperationRepository.save(pack);

        // Act: Send truck_arrived callback THREE times
        Integer truck1 = 600;
        Integer truck2 = 601;
        Integer truck3 = 602;

        log.info("=== Sending first truck_arrived (truck {}) ===", truck1);
        String response1 = mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"action\":\"truck_arrived\",\"truck_id\":%d,\"warehouse_id\":1}", truck1
                )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        log.info("Response 1: {}", response1);

        // Small delay to ensure first request completes
        Thread.sleep(100);

        log.info("=== Sending second truck_arrived (truck {}) ===", truck2);
        String response2 = mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"action\":\"truck_arrived\",\"truck_id\":%d,\"warehouse_id\":1}", truck2
                )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        log.info("Response 2: {}", response2);

        Thread.sleep(100);

        log.info("=== Sending third truck_arrived (truck {}) ===", truck3);
        String response3 = mockMvc.perform(post("/api/ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                    "{\"action\":\"truck_arrived\",\"truck_id\":%d,\"warehouse_id\":1}", truck3
                )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        log.info("Response 3: {}", response3);

        // Assert: Verify only one LOAD operation was created
        List<PackageOperation> loadOps = packageOperationRepository
                .findByPackageIdAndOperation(packageId, "LOAD")
                .stream()
                .toList();

        log.info("=== Test Results ===");
        log.info("Total LOAD operations for package {}: {}", packageId, loadOps.size());

        assertThat(loadOps).hasSize(1);

        PackageOperation loadOp = loadOps.get(0);
        log.info("Package {} loaded onto truck {}", packageId, loadOp.getTruckId());

        // Verify second and third responses indicate "no packages ready"
        assertThat(response2).containsIgnoringCase("no packages");
        assertThat(response3).containsIgnoringCase("no packages");

        log.info("✓ Test passed: Package claimed only once, subsequent requests got 'no packages'");
    }

    @Test
    @DisplayName("Retry of pack_ready callback should not create duplicate PACK completion")
    public void testDuplicatePackReady_shouldBeIdempotent() throws Exception {
        // Arrange: Create a package with PACK in PENDING state
        Long packageId = 3001L;

        log.info("=== Setting up package with PACK PENDING: packageId={} ===", packageId);

        PackageOperation purchase = new PackageOperation();
        purchase.setPackageId(packageId);
        purchase.setOperation("PURCHASE");
        purchase.setStatus("COMPLETED");
        packageOperationRepository.save(purchase);

        PackageOperation pack = new PackageOperation();
        pack.setPackageId(packageId);
        pack.setOperation("PACK");
        pack.setStatus("PENDING");
        packageOperationRepository.save(pack);

        // Act: Send pack_ready callback TWICE
        String requestBody = String.format(
            "{\"packageId\":%d,\"seqNum\":100}",
            packageId
        );

        log.info("=== Sending first pack_ready callback ===");
        mockMvc.perform(post("/api/world/callbacks/ready")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        log.info("=== Sending duplicate pack_ready callback ===");
        mockMvc.perform(post("/api/world/callbacks/ready")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        // Assert: Verify only one PACK operation exists and it's COMPLETED
        Optional<PackageOperation> packOp = packageOperationRepository
                .findByPackageIdAndOperation(packageId, "PACK");

        log.info("=== Test Results ===");

        assertThat(packOp).isPresent();
        assertThat(packOp.get().getStatus()).isEqualTo("COMPLETED");

        // Count all PACK operations for this package
        List<PackageOperation> allPackOps = packageOperationRepository.findByPackageId(packageId)
                .stream()
                .filter(op -> "PACK".equals(op.getOperation()))
                .toList();

        assertThat(allPackOps).hasSize(1);

        log.info("✓ Test passed: PACK operation updated to COMPLETED once, no duplicates");
    }

    @Test
    @DisplayName("Retry of loaded callback should not create duplicate LOAD completion")
    public void testDuplicateLoaded_shouldBeIdempotent() throws Exception {
        // Arrange: Create a package with LOAD in PENDING state
        Long packageId = 4001L;
        Integer truckId = 700;

        log.info("=== Setting up package with LOAD PENDING: packageId={} ===", packageId);

        PackageOperation purchase = new PackageOperation();
        purchase.setPackageId(packageId);
        purchase.setOperation("PURCHASE");
        purchase.setStatus("COMPLETED");
        packageOperationRepository.save(purchase);

        PackageOperation pack = new PackageOperation();
        pack.setPackageId(packageId);
        pack.setOperation("PACK");
        pack.setStatus("COMPLETED");
        packageOperationRepository.save(pack);

        PackageOperation load = new PackageOperation();
        load.setPackageId(packageId);
        load.setOperation("LOAD");
        load.setStatus("PENDING");
        load.setTruckId(truckId);
        packageOperationRepository.save(load);

        // Act: Send loaded callback TWICE
        String requestBody = String.format(
            "{\"packageId\":%d,\"seqNum\":200}",
            packageId
        );

        log.info("=== Sending first loaded callback ===");
        mockMvc.perform(post("/api/world/callbacks/loaded")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        log.info("=== Sending duplicate loaded callback ===");
        mockMvc.perform(post("/api/world/callbacks/loaded")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        // Assert: Verify only one LOAD operation exists and it's COMPLETED
        Optional<PackageOperation> loadOp = packageOperationRepository
                .findByPackageIdAndOperation(packageId, "LOAD");

        log.info("=== Test Results ===");

        assertThat(loadOp).isPresent();
        assertThat(loadOp.get().getStatus()).isEqualTo("COMPLETED");

        // Count all LOAD operations for this package
        List<PackageOperation> allLoadOps = packageOperationRepository.findByPackageId(packageId)
                .stream()
                .filter(op -> "LOAD".equals(op.getOperation()))
                .toList();

        assertThat(allLoadOps).hasSize(1);

        log.info("✓ Test passed: LOAD operation updated to COMPLETED once, no duplicates");
    }
}
