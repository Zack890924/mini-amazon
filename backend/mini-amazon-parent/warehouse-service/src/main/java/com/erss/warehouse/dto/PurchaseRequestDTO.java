package com.erss.warehouse.dto;


import lombok.Data;

import java.util.List;

@Data
public class PurchaseRequestDTO {
    private List<OrderItemDTO> items;
    private DeliveryAddressDTO deliveryAddress;
    private String upsAccount;
    private Long packageId;

    public PackageOperationRequestDTO toPackageOpRequest(){

        PackageOperationRequestDTO dto = new PackageOperationRequestDTO();
        dto.setPackageId(this.packageId);
        return dto;
    }

}
