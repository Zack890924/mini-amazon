package com.erss.warehouse.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsCallbackDTO {
    private String action;
    private String status;
    private Long packageId;
    private Integer truckId;
    private Integer warehouseId;
    private String message;
    private Integer newDestinationX;
    private Integer newDestinationY;
    private Integer userId;
}
