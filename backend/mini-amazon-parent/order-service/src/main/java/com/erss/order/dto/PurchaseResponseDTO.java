package com.erss.order.dto;

import lombok.Data;

@Data
public class PurchaseResponseDTO {
    private Long orderId;
    private String status;
    private String message;

}

