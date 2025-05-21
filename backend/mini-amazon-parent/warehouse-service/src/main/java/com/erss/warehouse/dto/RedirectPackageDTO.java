package com.erss.warehouse.dto;

import lombok.Data;

@Data
public class RedirectPackageDTO {
    private Long packageId;
    private Integer newDestinationX;
    private Integer newDestinationY;
    private Integer userId;
}
