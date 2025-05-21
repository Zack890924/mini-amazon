package com.erss.worldconnector.service;

import com.erss.worldconnector.dto.ArrivedItemDTO;
import com.erss.worldconnector.dto.ArrivedRequestDTO;
import com.erss.worldconnector.dto.LoadedRequestDTO;
import com.erss.worldconnector.dto.PackReadyRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.erss.common.proto.*;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class WarehouseCallbackClient {

    private final RestTemplate restTemplate;

    @Value("${warehouse.callback.url:http://localhost:8082/internal/world/arrived}")
    private String arrivedUrl;

    @Value("${warehouse.callback.ready.url:http://localhost:8082/internal/world/pack-ready}")
    private String callbackReadyUrl;

    @Value("${warehouse.callback.loaded:http://localhost:8082/internal/world/loaded}")
    private String loadedUrl;


    public void notifyArrived(APurchaseMore apm){
        try {
            log.info("send arrived notification back to warehouse: seqNum={}, warehouseId={}",
                    apm.getSeqnum(), apm.getWhnum());

            List<ArrivedItemDTO> items = apm.getThingsList()
                    .stream()
                    .map(t -> {
                        ArrivedItemDTO d = new ArrivedItemDTO();
                        d.setProductId(t.getId());
                        d.setDescription(t.getDescription());
                        d.setCount(t.getCount());
                        return d;
                    })
                    .collect(Collectors.toList());

            ArrivedRequestDTO dto = new ArrivedRequestDTO();
            dto.setSeqNum(apm.getSeqnum());
            dto.setWarehouseId(apm.getWhnum());
            dto.setItems(items);

            dto.setPackageId(apm.getSeqnum());

            log.info("send arrived notification: seqNum={}, packageId={}, items={}",
                    dto.getSeqNum(), dto.getPackageId(), dto.getItems());


            ResponseEntity<?> response = restTemplate.postForEntity(arrivedUrl, dto, Void.class);
            log.info("Arrived notification sent: {}", response.getStatusCode());

            if(!response.getStatusCode().is2xxSuccessful()){
                log.error("arrived notification failed: {}", response.getStatusCode());
            }
        } catch(Exception e){

            log.error("Arrived error", e);
        }
    }


    public void notifyReady(APacked ready){

        try {
            log.info("send pack ready notification to warehouse: shipid={}, seqNum={}",
                    ready.getShipid(), ready.getSeqnum());

            PackReadyRequestDTO dto = new PackReadyRequestDTO();
            dto.setPackageId(ready.getShipid());
            dto.setSeqNum(ready.getSeqnum());

            ResponseEntity<?> response = restTemplate.postForEntity(callbackReadyUrl, dto, Void.class);
            log.info("Pack-ready notification sent: {}", response.getStatusCode());

            if(!response.getStatusCode().is2xxSuccessful()){

                log.error("Pack-ready notification failed: {}", response.getStatusCode());
            }



        } catch(Exception e){
            log.error("Ready error", e);
        }
    }


    public void notifyLoaded(ALoaded ld){

        try {
            log.info("send loaded notification to warehouse: packageId={}, seqNum={}",
                    ld.getShipid(), ld.getSeqnum());

            LoadedRequestDTO dto = new LoadedRequestDTO();
            dto.setPackageId(ld.getShipid());
            dto.setSeqNum(ld.getSeqnum());


            ResponseEntity<?> response = restTemplate.postForEntity(loadedUrl, dto, Void.class);
            log.info("Loaded notification sent: {}", response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful()){

                log.error("Loaded notification failed: {}", response.getStatusCode());
            }

        } catch (Exception e){

            log.error("Loaded error", e);
        }
    }
}