package com.erss.order.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class OrderItemDTO {
    @NotNull
    private Long productId;

    private String description;
    @Min(1)
    private int quantity;
}
