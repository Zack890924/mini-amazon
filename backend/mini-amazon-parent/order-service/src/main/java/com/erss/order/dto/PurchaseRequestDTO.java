package com.erss.order.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseRequestDTO {
    @Valid
    private List<OrderItemDTO> items;
    @Valid
    private DeliveryAddressDTO deliveryAddress;
    private String upsAccount;
}

