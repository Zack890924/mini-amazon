package com.erss.warehouse.client;

import com.erss.warehouse.entity.PackageOperation;
import com.erss.warehouse.entity.TrackingInfo;
import com.erss.warehouse.repository.PackageOperationRepository;
import com.erss.warehouse.repository.TrackingInfoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpsClient {

    private final RestTemplate restTemplate;

    @Value("${ups.api.url}")
    private String upsApiUrl;

    @Value("${ups.auth.token:your-auth-token}")
    private String authToken;

    private final TrackingInfoRepository trackingInfoRepository;
    private final PackageOperationRepository packageOperationRepository;

    public void notifyLoaded(Long packageId, Integer truckId) {
        try {
            PackageOperation op = packageOperationRepository.findByPackageIdAndOperation(packageId, "LOAD")
                    .orElse(null);

            if (op == null || !"COMPLETED".equals(op.getStatus())) {
                log.warn("Cannot notify UPS about loaded package: packageId={}, status not COMPLETED", packageId);
                return;
            }

            String trackingNumber = trackingInfoRepository.findByPackageId(packageId)
                    .map(TrackingInfo::getTrackingNumber)
                    .orElse(packageId.toString());

            log.info("Preparing to notify UPS about package loaded: packageId={} (tracking={}), truckId={}",
                    packageId, trackingNumber, truckId);


            try {
                log.info("Adding delay before notifying package_loaded...");
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            log.info("Now notifyLoaded to UPS: packageId={} (tracking={}), truckId={}",
                    packageId, trackingNumber, truckId);
            Map<String, Object> request = new HashMap<>();
            request.put("action", "package_loaded");
            request.put("package_id", trackingNumber);
            request.put("truck_id", truckId);
            sendNotificationToUps(request);
        } catch (Exception e) {
            log.error("Error in notifyLoaded", e);
        }
    }

public Map<String,Object> notifyLoadingPackage(Long packageId, Integer truckId, Integer warehouseId) {
    try {
        Map<String, Object> request = new HashMap<>();
        request.put("action", "loading_package");
        request.put("package_id", packageId.toString());
        request.put("truck_id", truckId);
        request.put("warehouse_id", warehouseId);
        // 加 timestamp / message_id 若需要

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authToken);
        String json = new ObjectMapper().writeValueAsString(request);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        ResponseEntity<Map> resp = restTemplate.exchange(
                upsApiUrl, HttpMethod.POST, entity, Map.class
        );

        log.info("UPS HTTP={} body={}", resp.getStatusCode(), resp.getBody());
        return resp.getBody();
    } catch (Exception e) {
        log.error("notifyLoadingPackage error", e);
        return null;
    }
}

    public void notifyPackageReady(Long packageId){
        try {

            String trackingNumber = trackingInfoRepository.findByPackageId(packageId)
                    .map(TrackingInfo::getTrackingNumber)
                    .orElse(packageId.toString());

            log.info("notifyPackageReady to UPS: packageId={} (tracking={})", packageId, trackingNumber);


            Map<String, Object> request = new HashMap<>();
            request.put("action", "package_ready");
            request.put("package_id", trackingNumber);
            sendNotificationToUps(request);
        } catch (Exception e){
            log.error("error", e);
        }
    }



    public String requestPickup(Long packageId, Integer warehouseId, Integer userId, Integer destX, Integer destY, String description, List<Map<String, Object>> items) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("action", "request_pickup");
            if (packageId != null) request.put("package_id", packageId.toString());
            request.put("warehouse_id", warehouseId);
            if (userId != null) request.put("user_id", userId);
            request.put("destination_x", destX);
            request.put("destination_y", destY);
            if (description != null) request.put("description", description);
            if (items != null) request.put("items", items);

            String json = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);
            byte[] bodyBytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            headers.setContentLength(bodyBytes.length);

            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<Map> response = restTemplate.exchange(upsApiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object status = response.getBody().get("status");
                if ("success".equals(status)) {
                    log.info("UPS response: {}", response.getBody());
                    Object trackingNumber = response.getBody().get("tracking_number");

                    if (trackingNumber != null) {
                        // 保存映射
                        TrackingInfo trackingInfo = new TrackingInfo();
                        trackingInfo.setPackageId(packageId);
                        trackingInfo.setTrackingNumber(trackingNumber.toString());
                        trackingInfoRepository.save(trackingInfo);

                        log.info("Pickup request success, tracking_number={} for packageId={}",
                                trackingNumber, packageId);

                        return trackingNumber.toString(); // 返回追蹤號碼
                    }
                } else {
                    log.warn("UPS pickup error: {}", response.getBody().get("message"));
                }
            } else {
                log.warn("UPS pickup HTTP error: {}", response.getStatusCode());
            }
            return null; // 如果失敗
        } catch (ResourceAccessException e) {
            log.error("UPS server not responding, fallback triggered. Reason: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("requestPickup error", e);
            return null;
        }
    }





    public Map<String, Object> queryPackageStatus(Long packageId){
        try {

            String trackingNumber = trackingInfoRepository.findByPackageId(packageId)
                    .map(TrackingInfo::getTrackingNumber)
                    .orElse(packageId.toString());


            log.info("queryPackageStatus to UPS: packageId={} (tracking={})", packageId, trackingNumber);
            Map<String, Object> request = new HashMap<>();
            request.put("action", "query_status");
            request.put("package_id", packageId);
            ResponseEntity<Map> response = restTemplate.postForEntity(upsApiUrl, request, Map.class);
            if(response.getStatusCode().is2xxSuccessful()){
                log.info("UPS query_status successful: {}", response.getBody());
                return response.getBody();
            }
            else{
                log.warn("UPS query_status failed : {}", response.getStatusCode());
                return Map.of("status", "error", "message", "UPS query_status failed");
            }
        }catch (Exception e){
            log.error("error while queryPackageStatus" , e);
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    private void sendNotificationToUps(Map<String, Object> request){
        try {
            log.info("sendNotification to UPS: {}", request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(authToken);

            String json = new ObjectMapper().writeValueAsString(request);

            HttpEntity<String> entity = new HttpEntity<>(json, headers);
//            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);



            ResponseEntity<Map> response = restTemplate.exchange(upsApiUrl, HttpMethod.POST, entity, Map.class);
            log.info("UPS HTTP={} body={}", response.getStatusCode(), response.getBody());
            if (response.getStatusCode().is2xxSuccessful()){
                log.info("UPS successfully notified: action={}", request.get("action"));
            } else{
                log.warn("UPS notification failed: action={}, status={}", request.get("action"), response.getStatusCode());
            }
        } catch (Exception e){
            log.error("error", e);
        }
    }
}
