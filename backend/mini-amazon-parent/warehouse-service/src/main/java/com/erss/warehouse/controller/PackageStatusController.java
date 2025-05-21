package com.erss.warehouse.controller;

import com.erss.warehouse.client.UpsClient;
import com.erss.warehouse.entity.PackageOperation;
import com.erss.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouse/status")
@RequiredArgsConstructor
@Slf4j
public class PackageStatusController{

    private final WarehouseService warehouseService;
    private final UpsClient upsClient;

    @GetMapping("/{packageId}")
    public ResponseEntity<Map<String, Object>> getPackageStatus(@PathVariable Long packageId){

        List<PackageOperation> operations = warehouseService.getOperationsByPackage(packageId);

        if (operations.isEmpty()){

            return ResponseEntity.notFound().build();
        }


        Map<String, Object> localStatus = buildLocalStatus(operations);


        try {
            Map<String, Object> upsStatus = upsClient.queryPackageStatus(packageId);

            Map<String, Object> combinedStatus = new HashMap<>(localStatus);
            combinedStatus.put("ups_status", upsStatus);

            return ResponseEntity.ok(combinedStatus);
        }catch (Exception e){

            log.error("error while getting UPS status", e);

            localStatus.put("ups_status_error", "error while getting UPS status");
            return ResponseEntity.ok(localStatus);
        }
    }

    private Map<String, Object> buildLocalStatus(List<PackageOperation> operations){
        Map<String, Object> status = new HashMap<>();

        if (!operations.isEmpty()){

            status.put("package_id", operations.get(0).getPackageId());
        }

        List<Map<String, Object>> operationsList = new ArrayList<>();
        for (PackageOperation op : operations){

            Map<String, Object> opStatus = new HashMap<>();
            opStatus.put("operation", op.getOperation());
            opStatus.put("status", op.getStatus());
            if (op.getTruckId() != null){

                opStatus.put("truck_id", op.getTruckId());
            }
            operationsList.add(opStatus);
        }
        status.put("operations", operationsList);

        String currentStatus = determinePackageStatus(operations);
        status.put("status", currentStatus);

        return status;
    }

    private String determinePackageStatus(List<PackageOperation> operations){
        boolean hasPurchase = false;
        boolean hasArrived  = false;
        boolean hasPack     = false;
        boolean hasLoad     = false;

        for(PackageOperation op : operations){
            if(!"COMPLETED".equals(op.getStatus())){
                continue;
            }
            switch(op.getOperation()){
                case "PURCHASE":
                    hasPurchase = true;
                    break;
                case "ARRIVED":
                    hasArrived = true;
                    break;
                case "PACK":
                    hasPack = true;
                    break;
                case "LOAD":
                    hasLoad = true;
                    break;
                default:
            }
        }

        if(hasLoad){
            return "loaded";
        }
        else if (hasPack){
            return "ready_for_pickup";
        }
        else if (hasArrived){
            return "arrived";
        }
        else if (hasPurchase){
            return "purchased";
        }else{
            return "unknown";
        }
    }

}
