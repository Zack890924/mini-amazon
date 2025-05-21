package com.erss.warehouse.controller;

import com.erss.warehouse.client.UpsClient;
import com.erss.warehouse.dto.UpsCallbackDTO;
import com.erss.warehouse.entity.PackageOperation;
import com.erss.warehouse.entity.TrackingInfo;
import com.erss.warehouse.repository.PackageOperationRepository;
import com.erss.warehouse.repository.TrackingInfoRepository;
import com.erss.warehouse.service.WarehouseService;
import com.erss.warehouse.service.WarehouseWorldFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.erss.warehouse.dto.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/ups", "/api/ups/"})
@RequiredArgsConstructor
@Slf4j
public class UpsCallbackController {

    private final WarehouseService warehouseService;
    private final PackageOperationRepository packageOperationRepository;
    private final RestTemplate restTemplate;
    @Autowired
    private TrackingInfoRepository trackingInfoRepository;

    @Autowired
    private TaskExecutor taskExecutor;

    private final UpsClient upsClient;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleUpsCallback(@RequestBody Map<String, Object> request) {

        log.info("received ups callback: {}", request);
        String action = (String) request.get("action");

        if(action == null){
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Missing 'action' field"));
        }

        switch (action){
            case "truck_arrived":
                return handleTruckArrived(request);
            case "query_status":
                return handleQueryStatus(request);
            case "package_delivered":
                return handlePackageDelivered(request);
//            case "delivery_started":
//                return handleDeliveryStarted(request);
            case "redirect_package":
                return handleRedirectPackage(request);
            case "world_created":
                return handleWorldCreated(request);
//            case "loading_package_response":
//                return handleLoadingPackageResponse(request);
            default:
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Unknown action: " + action));
        }
    }


private ResponseEntity<Map<String, Object>> handleTruckArrived(Map<String, Object> req) {
    try {
        Integer truckId     = getIntValue(req, "truck_id");
        Integer warehouseId = getIntValue(req, "warehouse_id");
//        if (truckId==null || warehouseId==null) {
//            return bad("truck_arrived_response","Missing truck_id or warehouse_id");
//        }


        List<PackageOperation> ready =
                packageOperationRepository.findByStatusAndOperation("COMPLETED","PACK");
//        if (ready.isEmpty()) {
//            return ok("truck_arrived_response","No packages ready");
//        }

        Long pkgId = ready.get(0).getPackageId();
        log.info("truck_arrived: notify UPS to load package {}", pkgId);

        Map<String,Object> upsResp =
                upsClient.notifyLoadingPackage(pkgId, truckId, warehouseId);

        if (upsResp!=null && "success".equals(upsResp.get("status"))) {

            PackageOperationRequestDTO dto = new PackageOperationRequestDTO();
            dto.setPackageId(pkgId);
            dto.setTruckId(truckId);
            warehouseService.processLoading(dto);
            log.info("Called processLoading after UPS ack for package {}", pkgId);
        } else {
            log.warn("UPS load rejected or no response: {}", upsResp);
        }

        return ResponseEntity.ok(Map.of(
                "action",         "truck_arrived_response",
                "in_response_to", "ups",
                "status",         "success",
                "message",        "Truck " + truckId + " arrival processed"
        ));
    } catch (Exception e) {
        log.error("handleTruckArrived error", e);
        return ResponseEntity.status(500).body(Map.of(
                "action",  "truck_arrived_response",
                "status",  "error",
                "message", e.getMessage()
        ));
    }
}















//    private ResponseEntity<Map<String, Object>> handleTruckArrived(Map<String, Object> request) {
//        try {
//            String action = (String) request.get("action");
//            Integer truckId = getIntValue(request, "truck_id");
//            Integer warehouseId = getIntValue(request, "warehouse_id");
//
//            if (!"truck_arrived".equals(action) || truckId == null || warehouseId == null) {
//                return ResponseEntity.badRequest().body(Map.of(
//                        "action", "truck_arrived_response",
//                        "in_response_to", "ups",
//                        "status", "error",
//                        "message", "Missing required fields: truck_id, warehouse_id"
//                ));
//            }
//
//            log.info("handleTruckArrived: truckId={}, warehouseId={}", truckId, warehouseId);
//
//
//            List<PackageOperation> readyPackages = packageOperationRepository.findByStatusAndOperation("COMPLETED", "PACK");
//
//            if (!readyPackages.isEmpty()) {
//
//                PackageOperation readyPackage = readyPackages.get(0);
//
//                PackageOperationRequestDTO loadRequest = new PackageOperationRequestDTO();
//                loadRequest.setPackageId(readyPackage.getPackageId());
//                loadRequest.setTruckId(truckId);
//
////                warehouseService.processLoading(loadRequest);
//
//                Long pkgId = readyPackage.getPackageId();
//                upsClient.notifyLoadingPackage(pkgId, truckId, warehouseId);
//
//                log.info("Started loading package: packageId={}, truckId={}", readyPackage.getPackageId(), truckId);
//
//                return ResponseEntity.ok(Map.of(
//                        "action", "truck_arrived_response",
//                        "in_response_to", "ups",
//                        "status", "success",
//                        "message", "Truck " + truckId + " arrived and assigned to load package " + readyPackage.getPackageId()
//                ));
//            } else {
//
//                log.info("No packages ready for loading at this time, scheduling retry...");
//
//                final Integer finalTruckId = truckId;
//                final Integer finalWarehouseId = warehouseId;
//
//                taskExecutor.execute(() -> {
//                    try {
//
//                        Thread.sleep(1000);
//
//
//                        List<PackageOperation> retryPackages = packageOperationRepository.findByStatusAndOperation("COMPLETED", "PACK");
//
//                        if (!retryPackages.isEmpty()) {
//                            PackageOperation readyPackage = retryPackages.get(0);
//
//                            PackageOperationRequestDTO loadRequest = new PackageOperationRequestDTO();
//                            loadRequest.setPackageId(readyPackage.getPackageId());
//                            loadRequest.setTruckId(finalTruckId);
//
//                            warehouseService.processLoading(loadRequest);
//
//                            log.info("Delayed loading started for package: packageId={}, truckId={}",
//                                    readyPackage.getPackageId(), finalTruckId);
//                        } else {
//                            log.info("Retry check: Still no packages ready for loading");
//                        }
//                    } catch (Exception e) {
//                        log.error("Error in delayed package loading", e);
//                    }
//                });
//
//
//                return ResponseEntity.ok(Map.of(
//                        "action", "truck_arrived_response",
//                        "in_response_to", "ups",
//                        "status", "success",
//                        "message", "Truck " + truckId + " arrival acknowledged at warehouse " + warehouseId
//                ));
//            }
//        } catch (Exception e) {
//            log.error("error while processing truck_arrived callback", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
//                    "status", "error",
//                    "message", "error while processing truck_arrived callback: " + e.getMessage()
//            ));
//        }
//    }





    private ResponseEntity<Map<String, Object>> handleQueryStatus(Map<String, Object> request) {
        try{
            String trackingNumberStr = (String) request.get("package_id");
            Long packageId = findPackageIdByTracking(trackingNumberStr);

            if(packageId == null){
                return ResponseEntity.badRequest().body(Map.of(
                        "action", "query_status_response",
                        "status", "error",
                        "message", "Missing required field: package_id"
                ));
            }

            // Get package status from warehouse service
            List<PackageOperation> operations = warehouseService.getOperationsByPackage(packageId);

            // Determine package status based on operations
            String packageStatus = determinePackageStatus(operations);
            Integer truckId = getTruckIdFromOperations(operations);

            Map<String, Object> response = new HashMap<>();
            response.put("action", "query_status_response");
            response.put("status", "success");
            response.put("package_status", packageStatus);
            response.put("in_response_to", "ups");


            if (truckId != null) {
                response.put("truck_id", truckId);
            }
            return ResponseEntity.ok(response);
        }catch (Exception e){
            log.error("Error processing query_status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "action", "query_status_response",
                    "in_response_to", "ups",
                    "status", "error",
                    "message", "Error processing query: " + e.getMessage()
            ));
        }
    }

//    private ResponseEntity<Map<String, Object>> handleDeliveryStarted(Map<String, Object> request) {
//        try {
//            String action = (String) request.get("action");
//            Long packageId = getLongValue(request, "package_id");
//            Integer truckId = getIntValue(request, "truck_id");
//
//            if(!"delivery_started".equals(action) || packageId == null || truckId == null){
//                return ResponseEntity.badRequest().body(Map.of(
//                        "action", "delivery_started_response",
//                        "in_response_to", "ups",
//                        "status", "error",
//                        "message", "Missing required fields: package_id, truck_id"
//                ));
//            }
//
//
//            PackageOperation op = new PackageOperation();
//            op.setPackageId(packageId);
//            op.setOperation("OUT_FOR_DELIVERY");
//            op.setStatus("COMPLETED");
//            op.setTruckId(truckId);
//
//            packageOperationRepository.save(op);
//
//            log.info("Delivery started: packageId={}, truckId={}", packageId, truckId);
//            return ResponseEntity.ok(Map.of(
//                    "action", "delivery_started_response",
//                    "in_response_to", "ups",
//                    "status", "success",
//                    "message", "Delivery started status recorded"
//            ));
//        } catch (Exception e) {
//            log.error("Error processing delivery_started", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
//                    "action", "delivery_started_response",
//                    "in_response_to", "ups",
//                    "status", "error",
//                    "message", "Error processing delivery started: " + e.getMessage()
//            ));
//        }
//    }





    private ResponseEntity<Map<String, Object>> handlePackageDelivered(Map<String, Object> request) {
        try{
            String action = (String) request.get("action");
            Long packageId = getLongValue(request, "package_id");
//            String trackingNumberStr = (String) request.get("package_id");
//            Long packageId = findPackageIdByTracking(trackingNumberStr);
            Integer truckId = getIntValue(request, "truck_id");
            Integer deliveryX = getIntValue(request, "delivery_x");
            Integer deliveryY = getIntValue(request, "delivery_y");

            if (packageId == null || truckId == null || deliveryX == null || deliveryY == null || !action.equals("package_delivered")){
                return ResponseEntity.badRequest().body(Map.of(
                        "action", "package_delivered_response",
                        "status", "error",
                        "message", "Missing required fields for package_delivered"
                ));
            }

            // Create a delivered operation
            PackageOperation op = new PackageOperation();
            op.setPackageId(packageId);
            op.setOperation("DELIVERED");
            op.setStatus("COMPLETED");
            op.setTruckId(truckId);
            // Store delivery coordinates if needed

            packageOperationRepository.save(op);

            log.info("Package delivered: packageId={}, truckId={}, location=({},{})",
                    packageId, truckId, deliveryX, deliveryY);

            return ResponseEntity.ok(Map.of(
                    "action", "package_delivered_response",
                    "in_response_to", "ups",
                    "status", "success",
                    "message", "Delivery confirmed"
            ));
        }catch(Exception e){
            log.error("Error processing package_delivered", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "action", "package_delivered_response",
                    "in_response_to", "ups",
                    "status", "error",
                    "message", "Error processing delivery: " + e.getMessage()
            ));
        }
    }

    private ResponseEntity<Map<String, Object>> handleRedirectPackage(Map<String, Object> request) {
        try{
//            Long packageId = getLongValue(request, "package_id");
            String action = (String) request.get("action");
            Integer newDestX = getIntValue(request, "new_destination_x");
            Integer newDestY = getIntValue(request, "new_destination_y");
            Integer userId = getIntValue(request, "user_id");
            Long packageId = getLongValue(request, "package_id");

            if(!action.equals("redirect_package") || newDestX == null || newDestY == null){
                return ResponseEntity.badRequest().body(Map.of(
                        "action", "redirect_package_response",
                        "in_response_to", "ups",
                        "status", "error",
                        "message", "Missing required fields for redirection"
                ));
            }

            RedirectPackageDTO dto = new RedirectPackageDTO();
            dto.setPackageId(packageId);
            dto.setNewDestinationX(newDestX);
            dto.setNewDestinationY(newDestY);
            dto.setUserId(userId);

            RedirectResponseDTO response = warehouseService.handleRedirect(dto);

            return ResponseEntity.ok(Map.of(
                    "action", "redirect_package_response",
                    "in_response_to", response.getInResponseTo(),
                    "status", response.getStatus(),
                    "message", response.getMessage()
            ));
        }catch (Exception e){
            log.error("Error processing redirect_package", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "action", "redirect_package_response",
                    "status", "error",
                    "message", "Error processing redirection: " + e.getMessage()
            ));
        }
    }


    private ResponseEntity<Map<String, Object>> handleWorldCreated(Map<String,Object> req){
        Long world_id = getLongValue(req, "world_id");
        if(world_id == null){
            return ResponseEntity.badRequest()
                    .body(Map.of("action","world_created_response",
                            "in_response_to","ups",
                            "status","error",
                            "message","missing world_id"));
        }

        restTemplate.postForObject("http://localhost:8089/internal/context", Map.of("world_id", world_id),Void.class);

        return ResponseEntity.ok(Map.of(
                "action","world_created_response",
                "in_response_to","ups",
                "status","success",
                "message","world id "+world_id+" received"
        ));
    }


    // Helper methods
    private Integer getIntValue(Map<String, Object> request, String key){
        Object value = request.get(key);
        if(value == null){
            return null;
        }

        if(value instanceof Integer) {
            return (Integer) value;
        }
        else if(value instanceof String){
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse {} as Integer: {}", key, value);
                return null;
            }
        }
        else if (value instanceof Number){
            return ((Number) value).intValue();
        }

        return null;
    }

    private Long getLongValue(Map<String, Object> request, String key){
        Object value = request.get(key);
        if(value == null){
            return null;
        }

        if(value instanceof Long){
            return (Long) value;
        }
        else if (value instanceof Integer){
            return ((Integer) value).longValue();
        }
        else if (value instanceof String) {
            try{
                return Long.parseLong((String) value);
            }catch (NumberFormatException e){
                log.warn("Failed to parse {} as Long: {}", key, value);
                return null;
            }
        }
        else if(value instanceof Number){
            return ((Number) value).longValue();
        }

        return null;
    }

    private String determinePackageStatus(List<PackageOperation> operations) {
        if(operations == null || operations.isEmpty()){
            return "unknown";
        }

        boolean hasDelivered = false;
        boolean hasLoaded    = false;
        boolean hasLoading   = false;
        boolean hasReady     = false;
        boolean hasPurchase  = false;

        for (PackageOperation op : operations) {
            String operation = op.getOperation();
            String status    = op.getStatus();

            switch (operation) {
                case "DELIVERED":
                    if ("COMPLETED".equals(status)) {
                        hasDelivered = true;
                    }
                    break;

                case "LOAD":
                    if("COMPLETED".equals(status)){
                        hasLoaded = true;
                    }
                    else if("PENDING".equals(status)){
                        hasLoading = true;
                    }
                    break;

                case "PACK":
                    if("COMPLETED".equals(status)){
                        hasReady = true;
                    }
                    break;
                case "PURCHASE":
                    hasPurchase = true;
                    break;
                default:
                    break;
            }
        }

        if (hasDelivered) {
            return "delivered";
        }
        else if (hasLoaded){
            return "out_for_delivery";
        }
        else if (hasLoading){
            return "loading";
        }
        else if (hasReady)
        {
            return "ready_for_pickup";
        }
        else if (hasPurchase){
            return "created";
        }
        else{
            return "unknown";
        }
    }


    private Integer getTruckIdFromOperations(List<PackageOperation> operations) {
        if (operations == null || operations.isEmpty()) {
            return null;
        }

        for (PackageOperation op : operations) {
            if ("LOAD".equals(op.getOperation()) && op.getTruckId() != null) {
                return op.getTruckId();
            }
        }

        return null;
    }

    private Map<String, Object> createErrorResponse(Map<String, Object> request, String errorMessage) {
        String action = (String) request.get("action");
        Map<String, Object> response = new HashMap<>();
        response.put("action", action + "_response");
        response.put("status", "error");
        response.put("message", errorMessage);
        return response;
    }


    private Long findPackageIdByTracking(String trackingNumber) {
        return trackingInfoRepository.findByTrackingNumber(trackingNumber)
                .map(TrackingInfo::getPackageId)
                .orElseGet(() -> {
                    try {
                        return Long.parseLong(trackingNumber);
                    } catch (NumberFormatException e) {
                        log.warn("Cannot parse tracking number as Long: {}", trackingNumber);
                        return null;
                    }
                });
    }
}