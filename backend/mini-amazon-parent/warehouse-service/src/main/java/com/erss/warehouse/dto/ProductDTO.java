package com.erss.warehouse.dto;

import lombok.Data;

@Data
public class ProductDTO {
    private Long worldProductId;
    private String description;
    private Integer quantityAvailable;
}
