package com.erss.order.client;

import com.erss.order.dto.PurchaseRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import com.erss.order.dto.PurchaseResponseDTO;


@Component
@RequiredArgsConstructor
public class WarehouseClient {
    private final RestTemplate restTemplate;

    @Value("${warehouse.service.url:http://localhost:8082}")
    private String warehouseUrl;

    public PurchaseResponseDTO purchase(PurchaseRequestDTO req){

        return restTemplate.postForObject(warehouseUrl + "/purchase", req, PurchaseResponseDTO.class);
    }
}
