package com.erss.warehouse.controller;

import com.erss.warehouse.dto.*;
import com.erss.warehouse.entity.PackageOperation;
import com.erss.warehouse.service.WarehouseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;


    @PostMapping("/pack")
    public ResponseEntity<PackageOperationResponseDTO> pack(@Valid @RequestBody PackageOperationRequestDTO dto){

        return ResponseEntity.ok(warehouseService.packOrder(dto));
    }

    @PostMapping("/purchase")
    public ResponseEntity<PurchaseResponseDTO> purchase(@Valid @RequestBody PurchaseRequestDTO dto){

        return ResponseEntity.ok(warehouseService.processPurchase(dto));
    }

    @PostMapping("/load")
    public ResponseEntity<PackageOperationResponseDTO> load(@Valid @RequestBody PackageOperationRequestDTO dto){

        return ResponseEntity.ok(warehouseService.processLoading(dto));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> products(){

        return ResponseEntity.ok(warehouseService.getAllProducts());
    }

    @GetMapping("/products/{worldProductId}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable Long worldProductId){

        ProductDTO dto = warehouseService.getProduct(worldProductId);
        if (dto == null){

            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/operations/{pkgId}")
    public ResponseEntity<List<PackageOperation>> ops(@PathVariable Long pkgId){

//        log.info(">>> HIT OPS with pkgId={}", pkgId);
        return ResponseEntity.ok(warehouseService.getOperationsByPackage(pkgId));
    }



    @GetMapping("/operations/redirects/{userId}")
    public ResponseEntity<List<PackageOperation>> getRedirects(@PathVariable Integer userId) {
        return ResponseEntity.ok(warehouseService.getRedirectsByUser(userId));
    }



}
