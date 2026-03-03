package com.erss.warehouse.dto;

import lombok.Data;

import java.util.List;

@Data
public class PackageOperationRequestDTO {
    private Long packageId;
    private Integer truckId;
    private List<OrderItemDTO> items;
}
