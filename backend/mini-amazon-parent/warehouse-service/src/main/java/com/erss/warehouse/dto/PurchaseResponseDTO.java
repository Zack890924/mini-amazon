package com.erss.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponseDTO {
    private Long orderId;
    private long warehousePackageId;
    private String status;
    private String message;
}