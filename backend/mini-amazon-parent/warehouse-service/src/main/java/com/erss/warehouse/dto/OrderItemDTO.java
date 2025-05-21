package com.erss.warehouse.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemDTO {
    @NotNull
    private Long productId;

    private String description;
    @Min(1)
    private int quantity;
}